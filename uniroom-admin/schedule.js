/* =====================================================
   ADD SCHEDULE – with conflict detection
   =====================================================
   The API checks room, faculty and section availability
   before saving. If another class overlaps, it answers
   with the list of conflicts and nothing is saved.
===================================================== */

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

        const response = await apiFetch("add_schedule.php", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
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

        showMessage(message, result.message || "Schedule saved successfully.", "success");

        // Clear the time fields for the next entry and refresh the list
        document.getElementById("scheduleStartTime").value = "";
        document.getElementById("scheduleEndTime").value = "";
        loadSchedule();

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
