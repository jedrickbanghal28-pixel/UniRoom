/* =====================================================
   ACADEMIC SETUP – sections, subjects and rooms
   =====================================================
   Admin page for adding, editing and removing the
   records that accounts and schedules use. Each tab
   calls the REST endpoints of the UniRoom API:
     GET/POST /sections   PUT/DELETE /sections/{id}
     GET/POST /subjects   PUT/DELETE /subjects/{id}
     GET/POST /rooms      PUT/DELETE /rooms/{id}
   The API refuses to delete a record that is still
   used (e.g. a room that has classes) and says why.
===================================================== */

const SETUP = {
    section: {
        path: "sections",
        form: "sectionForm",
        title: "sectionFormTitle",
        save: "secSaveButton",
        cancel: "secCancelButton",
        message: "sectionMessage",
        noun: "Section",
        fields: { name: "secName", department_id: "secDepartment" },
        editingId: null,
        rows: []
    },
    subject: {
        path: "subjects",
        form: "subjectForm",
        title: "subjectFormTitle",
        save: "subSaveButton",
        cancel: "subCancelButton",
        message: "subjectMessage",
        noun: "Subject",
        fields: { code: "subCode", name: "subName" },
        editingId: null,
        rows: []
    },
    room: {
        path: "rooms",
        form: "roomSetupForm",
        title: "roomFormTitle",
        save: "rmSaveButton",
        cancel: "rmCancelButton",
        message: "roomSetupMessage",
        noun: "Room",
        fields: { room_name: "rmName", building: "rmBuilding", floor: "rmFloor", capacity: "rmCapacity", status: "rmStatus" },
        editingId: null,
        rows: []
    }
};


function showSetupTab(tabId, button) {

    document.querySelectorAll("#adminSetup .tab-panel").forEach(function(panel) {
        panel.classList.toggle("hidden", panel.id !== tabId);
    });

    document.querySelectorAll("#adminSetup .tab-button").forEach(function(b) {
        b.classList.toggle("active", b === button);
    });

}


/* Called when the Academic Setup page opens. */
function loadSetup() {
    loadSetupDepartments();
    loadSections();
    loadSubjects();
    loadRooms();
}


async function setupRequest(path, method, body) {
    const options = { method: method || "GET", headers: {} };
    if (body) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }
    const response = await apiFetch(path, options);
    const result = await readJson(response);
    return { ok: response.ok && result.success !== false, result: result };
}


function setupTableMessage(tbodyId, columns, text) {
    document.getElementById(tbodyId).innerHTML =
        '<tr><td colspan="' + columns + '">' + escapeHtml(text) + "</td></tr>";
}


function actionButtons(kind, id) {
    return (
        '<td class="row-actions">' +
        '<button class="link-btn" onclick="editSetup(\'' + kind + "', " + id + ')">Edit</button>' +
        '<button class="link-btn danger-link" onclick="deleteSetup(\'' + kind + "', " + id + ', this)">Delete</button>' +
        "</td>"
    );
}


/* ---------------- departments (for the section form) ---------------- */

async function loadSetupDepartments() {
    const select = document.getElementById("secDepartment");
    try {
        const r = await setupRequest("departments");
        const previous = select.value;
        select.innerHTML = '<option value="">Select program</option>' +
            (r.result.departments || []).map(function(d) {
                return '<option value="' + d.id + '">' + escapeHtml(d.code + (d.name ? " – " + d.name : "")) + "</option>";
            }).join("");
        select.value = previous;
    } catch (error) {
        console.error("Departments error:", error);
    }
}


/* ---------------- sections ---------------- */

async function loadSections() {
    setupTableMessage("sectionsTable", 5, "Loading sections...");
    try {
        const r = await setupRequest("sections");
        if (!r.ok) return setupTableMessage("sectionsTable", 5, r.result.message || "Unable to load sections.");
        SETUP.section.rows = r.result.sections || [];
        if (!SETUP.section.rows.length) return setupTableMessage("sectionsTable", 5, "No sections yet.");
        document.getElementById("sectionsTable").innerHTML = SETUP.section.rows.map(function(s) {
            return "<tr" + (s.id === SETUP.section.editingId ? ' class="is-editing"' : "") + ">" +
                "<td><strong>" + escapeHtml(s.name) + "</strong></td>" +
                "<td>" + escapeHtml(s.department) + "</td>" +
                "<td>" + escapeHtml(s.students) + "</td>" +
                "<td>" + escapeHtml(s.home_room || "—") + "</td>" +
                actionButtons("section", s.id) +
                "</tr>";
        }).join("");
    } catch (error) {
        setupTableMessage("sectionsTable", 5, "Unable to connect to the server.");
    }
}

function saveSection() {
    saveSetup("section", {
        name: valueOf("secName"),
        department_id: Number(valueOf("secDepartment"))
    });
}


/* ---------------- subjects ---------------- */

async function loadSubjects() {
    setupTableMessage("subjectsTable", 3, "Loading subjects...");
    try {
        const r = await setupRequest("subjects");
        if (!r.ok) return setupTableMessage("subjectsTable", 3, r.result.message || "Unable to load subjects.");
        SETUP.subject.rows = r.result.subjects || [];
        if (!SETUP.subject.rows.length) return setupTableMessage("subjectsTable", 3, "No subjects yet.");
        document.getElementById("subjectsTable").innerHTML = SETUP.subject.rows.map(function(s) {
            return "<tr" + (s.id === SETUP.subject.editingId ? ' class="is-editing"' : "") + ">" +
                "<td><strong>" + escapeHtml(s.code) + "</strong></td>" +
                "<td>" + escapeHtml(s.name) + "</td>" +
                actionButtons("subject", s.id) +
                "</tr>";
        }).join("");
    } catch (error) {
        setupTableMessage("subjectsTable", 3, "Unable to connect to the server.");
    }
}

function saveSubject() {
    saveSetup("subject", { code: valueOf("subCode"), name: valueOf("subName") });
}


/* ---------------- rooms ---------------- */

const STATUS_BADGE = { Available: "badge-green", Reserved: "badge-yellow", Occupied: "badge-red", Maintenance: "badge-red" };

async function loadRooms() {
    setupTableMessage("roomsTable", 6, "Loading rooms...");
    try {
        const r = await setupRequest("rooms");
        if (!r.ok) return setupTableMessage("roomsTable", 6, r.result.message || "Unable to load rooms.");
        SETUP.room.rows = r.result.data || [];
        if (!SETUP.room.rows.length) return setupTableMessage("roomsTable", 6, "No rooms yet.");
        document.getElementById("roomsTable").innerHTML = SETUP.room.rows.map(function(room) {
            return "<tr" + (room.id === SETUP.room.editingId ? ' class="is-editing"' : "") + ">" +
                "<td><strong>" + escapeHtml(room.room_name) + "</strong></td>" +
                "<td>" + escapeHtml(room.building) + "</td>" +
                "<td>" + escapeHtml(room.floor) + "</td>" +
                "<td>" + escapeHtml(room.capacity) + "</td>" +
                '<td><span class="badge ' + (STATUS_BADGE[room.status] || "") + '">' + escapeHtml(room.status) + "</span></td>" +
                actionButtons("room", room.id) +
                "</tr>";
        }).join("");
    } catch (error) {
        setupTableMessage("roomsTable", 6, "Unable to connect to the server.");
    }
}

function saveRoom() {
    saveSetup("room", {
        room_name: valueOf("rmName"),
        building: valueOf("rmBuilding"),
        floor: Number(valueOf("rmFloor")),
        capacity: Number(valueOf("rmCapacity")),
        status: valueOf("rmStatus")
    });
}


/* ---------------- shared: save, edit, delete ---------------- */

function reloadSetup(kind) {
    if (kind === "section") loadSections();
    if (kind === "subject") loadSubjects();
    if (kind === "room") loadRooms();
}


async function saveSetup(kind, body) {

    const cfg = SETUP[kind];
    const form = document.getElementById(cfg.form);
    const message = document.getElementById(cfg.message);
    const button = document.getElementById(cfg.save);

    if (!validateRequired(form)) {
        showMessage(message, "Please complete the required fields marked with *.", "error");
        return;
    }

    if (kind === "room" && (body.floor < 0 || !Number.isInteger(body.floor))) {
        setFieldError(document.getElementById("rmFloor"), "Floor must be a whole number (0 or more).");
        showMessage(message, "Please check the highlighted fields.", "error");
        return;
    }
    if (kind === "room" && (body.capacity < 1 || !Number.isInteger(body.capacity))) {
        setFieldError(document.getElementById("rmCapacity"), "Capacity must be at least 1.");
        showMessage(message, "Please check the highlighted fields.", "error");
        return;
    }

    const editing = cfg.editingId !== null;
    button.disabled = true;
    showMessage(message, "Saving...", "");

    try {
        const r = await setupRequest(cfg.path + (editing ? "/" + cfg.editingId : ""), editing ? "PUT" : "POST", body);

        if (!r.ok) {
            showServerErrors(r.result.errors, cfg.fields);
            showMessage(message, r.result.message || "Unable to save.", "error");
            return;
        }

        resetSetupForm(kind, true);
        showMessage(message, r.result.message || cfg.noun + " saved.", "success");
        reloadSetup(kind);
        if (kind !== "subject") loadScheduleOptions();   // keep the schedule form lists up to date

    } catch (error) {
        showMessage(message, "Unable to connect to the server.", "error");
    } finally {
        button.disabled = false;
    }

}


function editSetup(kind, id) {

    const cfg = SETUP[kind];
    const row = cfg.rows.find(function(item) { return item.id === id; });
    if (!row) return;

    cfg.editingId = id;
    clearFormErrors(document.getElementById(cfg.form));

    Object.keys(cfg.fields).forEach(function(field) {
        const input = document.getElementById(cfg.fields[field]);
        input.value = row[field] === null || row[field] === undefined ? "" : row[field];
    });

    document.getElementById(cfg.title).textContent = "Edit " + cfg.noun;
    document.getElementById(cfg.save).textContent = "Update " + cfg.noun;
    document.getElementById(cfg.cancel).classList.remove("hidden");
    showMessage(document.getElementById(cfg.message), "", "");
    reloadSetup(kind);
    document.getElementById(cfg.form).scrollIntoView({ behavior: "smooth", block: "start" });

}


function resetSetupForm(kind, keepMessage) {

    const cfg = SETUP[kind];
    const form = document.getElementById(cfg.form);

    cfg.editingId = null;
    clearFormErrors(form);
    Object.values(cfg.fields).forEach(function(id) {
        const input = document.getElementById(id);
        input.value = "";
    });
    if (kind === "room") {
        document.getElementById("rmBuilding").value = "Tech Hub";
        document.getElementById("rmStatus").value = "Available";
    }

    document.getElementById(cfg.title).textContent = "Add " + cfg.noun;
    document.getElementById(cfg.save).textContent = "Save " + cfg.noun;
    document.getElementById(cfg.cancel).classList.add("hidden");
    if (!keepMessage) showMessage(document.getElementById(cfg.message), "", "");
    reloadSetup(kind);

}


async function deleteSetup(kind, id, button) {

    const cfg = SETUP[kind];
    const row = cfg.rows.find(function(item) { return item.id === id; }) || {};
    const name = row.name || row.code || row.room_name || "this record";

    if (!confirm("Delete " + cfg.noun.toLowerCase() + " " + name + "? This cannot be undone.")) return;

    button.disabled = true;

    try {
        const r = await setupRequest(cfg.path + "/" + id, "DELETE");
        const message = document.getElementById(cfg.message);

        if (!r.ok) {
            showMessage(message, r.result.message || "Unable to delete.", "error");
            button.disabled = false;
            return;
        }

        if (cfg.editingId === id) resetSetupForm(kind, true);
        showMessage(message, r.result.message || cfg.noun + " deleted.", "success");
        reloadSetup(kind);

    } catch (error) {
        alert("Unable to connect to the server.");
        button.disabled = false;
    }

}
