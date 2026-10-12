/* =====================================================
   SCHEDULE – list, add, edit and delete classes
   =====================================================
   The API checks room, faculty and section availability
   before saving (new or edited class). If another class
   overlaps, it answers with the list of conflicts and
   nothing is saved.
===================================================== */

/* id of the class being edited (null = adding a new one) */
let editingScheduleId = null;
let scheduleRows = [];


async function loadSchedule() {

    const container = document.getElementById("scheduleList");
    if (!container) return;

    container.innerHTML = "<p>Loading schedules...</p>";

    try {

        const response = await apiFetch("faculty_schedule.php?user_id=" + currentUser.id);
        const result = await readJson(response);

        if (!response.ok || !result.success) {
            container.innerHTML = "<p>" + escapeHtml(result.message || "Unable to load schedules.") + "</p>";
            return;
        }

        scheduleRows = result.schedules || [];

        if (scheduleRows.length === 0) {
            container.innerHTML = "<p>No schedules found. Add the first class below.</p>";
            return;
        }

        container.innerHTML =
            '<div class="schedule-row schedule-head">' +
            "<span>Section</span><span>Subject</span><span>Faculty</span>" +
            "<span>Day and time</span><span>Room</span><span>Actions</span>" +
            "</div>" +
            scheduleRows.map(function(row) {
                return (
                    '<div class="schedule-row' + (row.id === editingScheduleId ? " is-editing" : "") + '">' +
                    "<strong>" + escapeHtml(row.section) + "</strong>" +
                    "<span>" + escapeHtml((row.subject_code ? row.subject_code + " – " : "") + row.subject) + "</span>" +
                    "<span>" + escapeHtml(row.faculty) + "</span>" +
                    "<span>" + escapeHtml(row.day_of_week + " " + formatTime(row.start_time) + " – " + formatTime(row.end_time)) + "</span>" +
                    "<span>" + escapeHtml(row.room) + "</span>" +
                    '<span class="row-actions">' +
                    '<button class="link-btn" onclick="editSchedule(' + row.id + ')">Edit</button>' +
                    '<button class="link-btn danger-link" onclick="deleteSchedule(' + row.id + ', this)">Delete</button>' +
                    "</span>" +
                    "</div>"
                );
            }).join("");

    } catch (error) {

        console.error("Faculty schedule error:", error);
        container.innerHTML = "<p>Unable to connect to the server.</p>";

    }

}


/* Puts a class into the form so it can be changed. */
function editSchedule(id) {

    const row = scheduleRows.find(function(item) { return item.id === id; });
    if (!row) return;

    editingScheduleId = id;
    hideScheduleConflicts();
    clearFormErrors(document.getElementById("scheduleForm"));

    document.getElementById("scheduleSection").value = row.section_id;
    document.getElementById("scheduleSubject").value = row.subject_id;
    document.getElementById("scheduleTeacher").value = row.faculty_id;
    document.getElementById("scheduleRoom").value = row.room_id;
    document.getElementById("scheduleDay").value = row.day_of_week;
    document.getElementById("scheduleStartTime").value = row.start_time;
    document.getElementById("scheduleEndTime").value = row.end_time;

    document.getElementById("scheduleFormTitle").textContent = "Edit Schedule";
    document.getElementById("saveScheduleButton").textContent = "Update Schedule";
    document.getElementById("cancelEditButton").classList.remove("hidden");
    showMessage(document.getElementById("scheduleMessage"),
        "Editing " + row.section + " · " + row.subject + ". Change the details and click Update Schedule.", "");

    loadSchedule();
    document.getElementById("scheduleForm").scrollIntoView({ behavior: "smooth", block: "start" });

}


/* Leaves edit mode and empties the form. */
function cancelEditSchedule(keepMessage) {

    editingScheduleId = null;
    hideScheduleConflicts();

    const form = document.getElementById("scheduleForm");
    clearFormErrors(form);
    form.querySelectorAll("select, input").forEach(function(el) { el.value = ""; });

    document.getElementById("scheduleFormTitle").textContent = "Add Schedule";
    document.getElementById("saveScheduleButton").textContent = "Save Schedule";
    document.getElementById("cancelEditButton").classList.add("hidden");
    if (!keepMessage) showMessage(document.getElementById("scheduleMessage"), "", "");

    loadSchedule();

}


async function deleteSchedule(id, button) {

    const row = scheduleRows.find(function(item) { return item.id === id; });
    const label = row ? row.section + " · " + row.subject + " (" + row.day_of_week + " " +
        formatTime(row.start_time) + ")" : "this class";

    if (!confirm("Delete " + label + "? Students of the section will no longer see it in the app.")) return;

    button.disabled = true;

    try {

        const response = await apiFetch("delete_schedule.php", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ id: id })
        });
        const result = await readJson(response);

        if (!response.ok || !result.success) {
            alert(result.message || "Unable to delete the schedule.");
            button.disabled = false;
            return;
        }

        if (editingScheduleId === id) cancelEditSchedule(true);
        showMessage(document.getElementById("scheduleMessage"), result.message || "Schedule deleted.", "success");
        loadSchedule();

    } catch (error) {

        console.error("Delete schedule error:", error);
        alert("Unable to connect to the server.");
        button.disabled = false;

    }

}

const SCHEDULE_FIELD_IDS = {
    section_id: "scheduleSection",
    subject_id: "scheduleSubject",
    faculty_id: "scheduleTeacher",
    day_of_week: "scheduleDay",
    start_time: "scheduleStartTime",
    end_time: "scheduleEndTime",
    room_id: "scheduleRoom"
};

const CONFLICT_TITLES = {
    room: "Room conflict",
    faculty: "Faculty conflict",
    section: "Section conflict"
};

async function addSchedule() {

    const form = document.getElementById("scheduleForm");
    const message = document.getElementById("scheduleMessage");
    const conflictBox = document.getElementById("scheduleConflicts");
    const button = document.getElementById("saveScheduleButton");
    const value = function(id) { return document.getElementById(id).value.trim(); };

    hideScheduleConflicts();

    /*
     * Required fields (marked with *)
     */
    if (!validateRequired(form)) {
        showMessage(message, "Please complete the required fields marked with *.", "error");
        return;
    }

    if (value("scheduleEndTime") <= value("scheduleStartTime")) {
        setFieldError(document.getElementById("scheduleEndTime"), "End time must be later than the start time.");
        showMessage(message, "End time must be later than the start time.", "error");
        return;
    }

    button.disabled = true;
    showMessage(message, "Checking room, faculty and section availability...", "");

    try {

        const editing = editingScheduleId !== null;

        const response = await apiFetch(editing ? "update_schedule.php" : "add_schedule.php", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                id: editingScheduleId,
                section_id: value("scheduleSection"),
                subject_id: value("scheduleSubject"),
                faculty_id: value("scheduleTeacher"),
                day_of_week: value("scheduleDay"),
                start_time: value("scheduleStartTime"),
                end_time: value("scheduleEndTime"),
                room_id: value("scheduleRoom")
            })
        });

        const result = await readJson(response);

        /*
         * 409 = scheduling conflict → show it, do not save
         */
        if (response.status === 409 && result.conflicts) {
            showScheduleConflicts(result);
            showMessage(message, "", "");
            return;
        }

        if (!response.ok || !result.success) {
            showServerErrors(result.errors, SCHEDULE_FIELD_IDS);
            showMessage(message, result.message || "Unable to save the schedule.", "error");
            return;
        }

        if (editing) {
            cancelEditSchedule(true);
        } else {
            // Clear the time fields for the next entry and refresh the list
            document.getElementById("scheduleStartTime").value = "";
            document.getElementById("scheduleEndTime").value = "";
            loadSchedule();
        }

        showMessage(message, result.message || "Schedule saved successfully.", "success");

    } catch (error) {

        console.error("Add schedule error:", error);
        showMessage(message, "Unable to connect to the server.", "error");

    } finally {

        button.disabled = false;

    }

}


function showScheduleConflicts(result) {

    const box = document.getElementById("scheduleConflicts");
    const types = {};

    let items = "";
    result.conflicts.forEach(function(conflict) {
        types[conflict.type] = true;
        items +=
            "<li><strong>" + escapeHtml(CONFLICT_TITLES[conflict.type] || "Conflict") + ":</strong> " +
            escapeHtml(conflict.detail) + "</li>";
    });

    box.innerHTML =
        '<div class="conflict-title">' +
        '<span class="conflict-icon" aria-hidden="true">!</span>' +
        "<div><strong>Scheduling Conflict</strong>" +
        "<p>" + escapeHtml(result.message.replace(/^Scheduling Conflict:\s*/, "")) + "</p></div>" +
        "</div>" +
        "<ul>" + items + "</ul>" +
        "<small>The schedule was not saved.</small>";

    box.classList.remove("hidden");

    // Highlight the fields that cause the conflict
    if (types.room) document.getElementById("scheduleRoom").classList.add("input-conflict");
    if (types.faculty) document.getElementById("scheduleTeacher").classList.add("input-conflict");
    if (types.section) document.getElementById("scheduleSection").classList.add("input-conflict");
    ["scheduleDay", "scheduleStartTime", "scheduleEndTime"].forEach(function(id) {
        document.getElementById(id).classList.add("input-conflict");
    });

    box.scrollIntoView({ behavior: "smooth", block: "nearest" });

}


function hideScheduleConflicts() {

    const box = document.getElementById("scheduleConflicts");
    if (!box) return;

    box.classList.add("hidden");
    box.innerHTML = "";

    document.querySelectorAll("#scheduleForm .input-conflict").forEach(function(el) {
        el.classList.remove("input-conflict");
    });

}


/* Any change to the form clears the old conflict notice. */
document.addEventListener("change", function(event) {
    if (event.target.closest && event.target.closest("#scheduleForm")) {
        hideScheduleConflicts();
    }
});
