/* =====================================================
   ACCOUNT CREATION
   =====================================================
   1. Individual account creation (form)
   2. Bulk account creation (CSV / Excel upload)

   Both send the account email with a password setup
   link (recommended) or a temporary password.
===================================================== */

let accountOptions = {
    departments: [],
    sections: [],
    loaded: false
};


/* =====================================================
   LOAD DEPARTMENTS AND SECTIONS
===================================================== */

async function loadAccountFormOptions() {

    const message =
        document.getElementById("accountMessage");

    try {

        const response = await apiFetch("admin_form_options.php");
        const result = await readJson(response);

        if (!response.ok || !result.success) {
            showMessage(message, result.message || "Unable to load departments.", "error");
            return;
        }

        accountOptions = {
            departments: result.departments || [],
            sections: result.sections || [],
            loaded: true
        };

        const department =
            document.getElementById("acDepartment");

        const previous = department.value;

        department.innerHTML =
            '<option value="">Select department</option>';

        accountOptions.departments.forEach(function(d) {
            department.innerHTML +=
                '<option value="' + d.id + '">' + escapeHtml(d.label) + "</option>";
        });

        department.value = previous;
        updateSectionOptions();

        // Warn the admin when Gmail is not set up on the API yet.
        document
            .getElementById("emailWarning")
            .classList.toggle("hidden", !!result.email_configured);

    } catch (error) {

        console.error("Account options error:", error);
        showMessage(message, "Unable to connect to the server.", "error");

    }

}


/* Show only the sections of the chosen department. */
function updateSectionOptions() {

    const departmentId =
        document.getElementById("acDepartment").value;

    const section =
        document.getElementById("acSection");

    const previous = section.value;

    const sections = accountOptions.sections.filter(function(s) {
        return String(s.department_id) === String(departmentId);
    });

    section.innerHTML = departmentId === ""
        ? '<option value="">Choose a department first</option>'
        : '<option value="">Select section</option>';

    sections.forEach(function(s) {
        section.innerHTML +=
            '<option value="' + s.id + '">' + escapeHtml(s.name) + "</option>";
    });

    section.value = sections.some(function(s) { return String(s.id) === previous; }) ? previous : "";

}


/* Student-only and faculty-only fields */
function updateRoleFields() {

    const role =
        document.getElementById("acRole").value;

    document.querySelectorAll("#accountForm .student-only").forEach(function(el) {
        el.classList.toggle("hidden", role !== "student");
    });

    document.querySelectorAll("#accountForm .faculty-only").forEach(function(el) {
        el.classList.toggle("hidden", role !== "faculty");
    });

}


/* Tabs: Individual | Bulk upload */
function showAccountTab(tabId, button) {

    document.querySelectorAll("#adminAccounts .tab-panel").forEach(function(panel) {
        panel.classList.toggle("hidden", panel.id !== tabId);
    });

    document.querySelectorAll("#adminAccounts .tab-button").forEach(function(b) {
        b.classList.toggle("active", b === button);
    });

}


function selectedPasswordMode(name) {
    const checked = document.querySelector('input[name="' + name + '"]:checked');
    return checked ? checked.value : "link";
}


/* =====================================================
   1. INDIVIDUAL ACCOUNT CREATION
===================================================== */

const ACCOUNT_FIELD_IDS = {
    first_name: "acFirstName",
    last_name: "acLastName",
    email: "acEmail",
    gender: "acGender",
    age: "acAge",
    role: "acRole",
    department: "acDepartment",
    section: "acSection",
    student_id: "acStudentId",
    employee_id: "acEmployeeId",
    contact_number: "acContact"
};

async function createAccount() {

    const form = document.getElementById("accountForm");
    const message = document.getElementById("accountMessage");
    const resultBox = document.getElementById("accountResult");
    const button = document.getElementById("createAccountButton");
    const value = function(id) { return document.getElementById(id).value.trim(); };

    resultBox.innerHTML = "";

    /*
     * Required fields (marked with *)
     */
    if (!validateRequired(form)) {
        showMessage(message, "Please complete the required fields marked with *.", "error");
        return;
    }

    /*
     * Quick checks before sending
     */
    const email = value("acEmail");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setFieldError(document.getElementById("acEmail"), "Enter a valid email address.");
        showMessage(message, "Please fix the highlighted fields.", "error");
        return;
    }

    const age = Number(value("acAge"));
    if (!Number.isInteger(age) || age < 15 || age > 100) {
        setFieldError(document.getElementById("acAge"), "Age must be a whole number from 15 to 100.");
        showMessage(message, "Please fix the highlighted fields.", "error");
        return;
    }

    const role = value("acRole");
    const mode = selectedPasswordMode("acPasswordMode");

    const payload = {
        first_name: value("acFirstName"),
        last_name: value("acLastName"),
        email: email,
        gender: value("acGender"),
        age: value("acAge"),
        role: role,
        department_id: value("acDepartment"),
        section_id: role === "student" ? value("acSection") : "",
        student_id: role === "student" ? value("acStudentId") : "",
        employee_id: role === "faculty" ? value("acEmployeeId") : "",
        contact_number: value("acContact"),
        password_mode: mode
    };

    button.disabled = true;
    showMessage(message, "Creating account and sending email...", "");

    try {

        const response = await apiFetch("admin_create_user.php", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const result = await readJson(response);

        if (!response.ok || !result.success) {
            // 422 = missing / invalid, 409 = email or student ID already used
            showServerErrors(result.errors, ACCOUNT_FIELD_IDS);
            showMessage(message, result.message || "Unable to create the account.", "error");
            return;
        }

        if (result.email_sent) {

            showMessage(message, "", "");

            resultBox.innerHTML =
                '<div class="notice notice-success">' +
                "<strong>Account created for " + escapeHtml(result.user.name) + "</strong>" +
                "<p>Username: <b>" + escapeHtml(result.user.username) + "</b>. " +
                (mode === "link"
                    ? "A password setup link was emailed to "
                    : "A temporary password was emailed to ") +
                escapeHtml(result.user.email) +
                ". The user must set a new password before using the account.</p>" +
                "</div>";

        } else {

            showMessage(message, result.message, "warning");

            resultBox.innerHTML =
                '<div class="notice notice-warning">' +
                "<strong>Account created, but the email was not sent.</strong>" +
                "<p>" + escapeHtml(result.email_error || "") + "</p>" +
                "<p>Give these details to " + escapeHtml(result.user.name) + " another way:</p>" +
                credentialsHtml(result) +
                "</div>";

        }

        resetAccountForm(false);

    } catch (error) {

        console.error("Create account error:", error);
        showMessage(message, "Unable to connect to the server.", "error");

    } finally {

        button.disabled = false;

    }

}


function credentialsHtml(item) {

    let html = '<div class="copy-box">';
    html += "<div><span>Username</span><code>" + escapeHtml(item.username || (item.user && item.user.username)) + "</code></div>";

    if (item.temporary_password) {
        html += "<div><span>Temporary password</span><code>" + escapeHtml(item.temporary_password) + "</code></div>";
    }
    if (item.setup_link) {
        html += '<div><span>Setup link</span><code class="wrap">' + escapeHtml(item.setup_link) + "</code></div>";
    }

    return html + "</div>";

}


function resetAccountForm(clearMessages) {

    const form = document.getElementById("accountForm");

    form.querySelectorAll("input[type=text], input[type=email], input[type=number], input[type=tel]")
        .forEach(function(input) { input.value = ""; });

    form.querySelectorAll("select").forEach(function(select) { select.value = ""; });

    updateSectionOptions();
    updateRoleFields();
    clearFormErrors(form);

    if (clearMessages) {
        showMessage(document.getElementById("accountMessage"), "", "");
        document.getElementById("accountResult").innerHTML = "";
    }

}


/* =====================================================
   2. BULK ACCOUNT CREATION (CSV / EXCEL)
===================================================== */

/*
 * Columns in the upload file. Headers are matched without caring about
 * capital letters, spaces, underscores or the * mark, and some common
 * alternative names are accepted too.
 */
const BULK_COLUMNS = [
    { key: "first_name",     label: "First Name",     required: true,  aliases: ["firstname", "given name"] },
    { key: "last_name",      label: "Last Name",      required: true,  aliases: ["lastname", "surname", "family name"] },
    { key: "email",          label: "Email Address",  required: true,  aliases: ["email address", "e-mail", "gmail"] },
    { key: "gender",         label: "Gender",         required: true,  aliases: ["sex"] },
    { key: "age",            label: "Age",            required: true,  aliases: [] },
    { key: "role",           label: "Role",           required: true,  aliases: ["type", "account type"] },
    { key: "department",     label: "Department",     required: true,  aliases: ["department code", "dept", "program", "course"] },
    { key: "section",        label: "Section",        required: false, aliases: ["section name", "block"] },
    { key: "student_id",     label: "Student ID",     required: false, aliases: ["student no", "student number", "id number"] },
    { key: "employee_id",    label: "Employee ID",    required: false, aliases: ["employee no", "employee number", "faculty id"] },
    { key: "contact_number", label: "Contact Number", required: false, aliases: ["contact", "contact no", "phone", "mobile", "mobile number"] }
];

const BULK_MAX_ROWS = 1000;
const BULK_VALIDATE_BATCH = 50;
const BULK_CREATE_BATCH = 20;

let bulkRows = [];      // [{ row_number, data: {...}, errors: [] }]
let bulkResults = [];   // results after creating


function normalizeHeader(text) {
    return String(text || "")
        .replace(/\*/g, "")
        .replace(/[_\-]+/g, " ")
        .replace(/\s+/g, " ")
        .trim()
        .toLowerCase();
}

function findColumnKey(header) {
    const h = normalizeHeader(header);
    if (h === "full name" || h === "name") return "full_name";
    for (const column of BULK_COLUMNS) {
        if (h === column.key.replace(/_/g, " ") || h === column.label.toLowerCase() || column.aliases.indexOf(h) !== -1) {
            return column.key;
        }
    }
    return null;
}


/* Download a ready-to-fill CSV template */
function downloadBulkTemplate() {

    const dept = accountOptions.departments[0];
    const section = dept && accountOptions.sections.find(function(s) { return s.department_id === dept.id; });
    const deptCode = dept ? dept.code : "BSIT";
    const sectionName = section ? section.name : "BSIT-A1";

    const lines = [
        BULK_COLUMNS.map(function(c) { return c.key; }).join(","),
        ["Juan", "Dela Cruz", "juan.delacruz@gmail.com", "Male", "19", "student", deptCode, sectionName, "2026-0001", "", "09171234567"].join(","),
        ["Maria", "Santos", "maria.santos@gmail.com", "Female", "34", "faculty", deptCode, "", "", "EMP-1001", "09181234567"].join(",")
    ];

    downloadText("uniroom_bulk_accounts_template.csv", lines.join("\r\n") + "\r\n");

}

function downloadText(fileName, text) {
    const blob = new Blob(["﻿" + text], { type: "text/csv;charset=utf-8" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(function() { URL.revokeObjectURL(link.href); }, 1000);
}

function csvCell(value) {
    const s = String(value === null || value === undefined ? "" : value);
    return /[",\r\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
}


/* Step 1: the admin picks a file */
async function handleBulkFile(input) {

    const message = document.getElementById("bulkMessage");
    const file = input.files && input.files[0];

    resetBulk();
    if (!file) return;

    document.getElementById("bulkFileName").textContent = file.name;

    if (!accountOptions.loaded) await loadAccountFormOptions();

    showMessage(message, "Reading " + file.name + "...", "");

    let sheet;
    try {
        sheet = await readSpreadsheet(file);
    } catch (error) {
        showMessage(message, error.message, "error");
        return;
    }

    // First non-empty row = column headers
    while (sheet.length && sheet[0].every(function(c) { return String(c).trim() === ""; })) sheet.shift();
    if (sheet.length < 2) {
        showMessage(message, "The file is empty. Add one account per row below the header row.", "error");
        return;
    }

    const headers = sheet[0];
    const keys = headers.map(findColumnKey);
    const hasFullName = keys.indexOf("full_name") !== -1;

    const missingColumns = BULK_COLUMNS.filter(function(c) {
        if (!c.required) return false;
        if ((c.key === "first_name" || c.key === "last_name") && hasFullName) return false;
        return keys.indexOf(c.key) === -1;
    });

    if (missingColumns.length) {
        showMessage(
            message,
            "The file is missing required column(s): " +
            missingColumns.map(function(c) { return c.label; }).join(", ") +
            ". Download the template to see the correct columns.",
            "error"
        );
        return;
    }

    // Read the rows (skip empty ones). Row numbers match the spreadsheet.
    const headerRowNumber = 1;
    for (let i = 1; i < sheet.length; i++) {
        const cells = sheet[i];
        if (!cells || cells.every(function(c) { return String(c).trim() === ""; })) continue;

        const data = {};
        keys.forEach(function(key, col) {
            if (key) data[key] = String(cells[col] === undefined ? "" : cells[col]).trim();
        });

        // "Full Name" column → first + last name
        if (data.full_name && !data.first_name && !data.last_name) {
            const full = data.full_name;
            if (full.indexOf(",") !== -1) {
                data.last_name = full.split(",")[0].trim();
                data.first_name = full.split(",").slice(1).join(",").trim();
            } else {
                const parts = full.split(/\s+/);
                data.last_name = parts.length > 1 ? parts.pop() : "";
                data.first_name = parts.join(" ");
            }
        }
        delete data.full_name;

        bulkRows.push({ row_number: i + headerRowNumber, data: data, errors: [] });
    }

    if (bulkRows.length === 0) {
        showMessage(message, "No accounts found in the file.", "error");
        return;
    }
    if (bulkRows.length > BULK_MAX_ROWS) {
        showMessage(message, "The file has " + bulkRows.length + " accounts. Upload at most " + BULK_MAX_ROWS + " at a time.", "error");
        bulkRows = [];
        return;
    }

    checkBulkRowsInBrowser();
    await checkBulkRowsOnServer();
    renderBulkPreview();

}


/* Step 2a: quick checks in the browser (missing info, duplicates inside the file) */
function checkBulkRowsInBrowser() {

    const emailRows = {};
    const studentIdRows = {};

    bulkRows.forEach(function(row) {
        const d = row.data;
        const role = (d.role || "").toLowerCase();

        BULK_COLUMNS.forEach(function(c) {
            if (c.required && !d[c.key]) row.errors.push("Missing " + c.label + ".");
        });
        if (role === "student" && !d.section) row.errors.push("Missing Section (required for students).");
        if (role === "student" && !d.student_id) row.errors.push("Missing Student ID (required for students).");

        if (d.email) {
            const key = d.email.toLowerCase();
            (emailRows[key] = emailRows[key] || []).push(row);
        }
        if (role === "student" && d.student_id) {
            (studentIdRows[d.student_id] = studentIdRows[d.student_id] || []).push(row);
        }
    });

    function markDuplicates(groups, label) {
        Object.keys(groups).forEach(function(key) {
            const rows = groups[key];
            if (rows.length < 2) return;
            rows.forEach(function(row) {
                const others = rows.filter(function(r) { return r !== row; }).map(function(r) { return r.row_number; });
                row.errors.push("Duplicate " + label + " in this file (also in row " + others.join(", ") + ").");
            });
        });
    }

    markDuplicates(emailRows, "email");
    markDuplicates(studentIdRows, "student ID");

}


/* Step 2b: ask the API to check the rest (department/section, already registered) */
async function checkBulkRowsOnServer() {

    const message = document.getElementById("bulkMessage");

    for (let i = 0; i < bulkRows.length; i += BULK_VALIDATE_BATCH) {

        const batch = bulkRows.slice(i, i + BULK_VALIDATE_BATCH);
        showMessage(message, "Checking rows " + (i + 1) + "–" + (i + batch.length) + " of " + bulkRows.length + "...", "");

        try {

            const response = await apiFetch("admin_bulk_create_users.php", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    validate_only: true,
                    rows: batch.map(function(r) { return Object.assign({ row_number: r.row_number }, r.data); })
                })
            });

            const result = await readJson(response);

            if (!response.ok || !result.success) {
                showMessage(message, result.message || "Unable to check the file.", "error");
                return;
            }

            result.results.forEach(function(res) {
                const row = batch.find(function(r) { return r.row_number === res.row_number; });
                if (!row) return;
                res.errors.forEach(function(e) {
                    // The browser already reported missing fields and file duplicates.
                    const text = e.message;
                    const alreadyListed = row.errors.some(function(existing) {
                        return existing === text ||
                            (/is required/.test(text)) ||
                            (/^Duplicate/.test(text) && existing.indexOf("Duplicate") === 0);
                    });
                    if (!alreadyListed) row.errors.push(text);
                });
            });

        } catch (error) {
            console.error("Bulk check error:", error);
            showMessage(message, "Unable to connect to the server.", "error");
            return;
        }

    }

    showMessage(message, "", "");

}


/* Step 3: show what will happen */
function renderBulkPreview() {

    const ready = bulkRows.filter(function(r) { return r.errors.length === 0; });
    const problems = bulkRows.length - ready.length;

    document.getElementById("bulkSummary").innerHTML =
        '<div class="bulk-stats">' +
        "<div><strong>" + bulkRows.length + "</strong><span>rows in file</span></div>" +
        '<div class="ok"><strong>' + ready.length + "</strong><span>ready to create</span></div>" +
        '<div class="bad"><strong>' + problems + "</strong><span>need fixing</span></div>" +
        "</div>" +
        (problems
            ? '<div class="notice notice-warning"><strong>' + problems + " row(s) have missing or duplicate information.</strong>" +
              "<p>They are listed below and will be skipped. Fix them in your file and upload it again, " +
              "or create the ready accounts now.</p></div>"
            : '<div class="notice notice-success"><strong>All rows look good.</strong><p>Click “Create accounts” to continue.</p></div>');

    let html =
        '<div class="table-container"><table class="bulk-table"><thead><tr>' +
        "<th>Row</th><th>Name</th><th>Email</th><th>Role</th><th>Department</th><th>Section</th><th>Status</th>" +
        "</tr></thead><tbody>";

    // Rows with problems first
    bulkRows.slice().sort(function(a, b) {
        return (b.errors.length > 0) - (a.errors.length > 0) || a.row_number - b.row_number;
    }).forEach(function(row) {
        const d = row.data;
        html += "<tr" + (row.errors.length ? ' class="row-problem"' : "") + ">" +
            "<td>" + row.row_number + "</td>" +
            "<td>" + escapeHtml(((d.first_name || "") + " " + (d.last_name || "")).trim() || "—") + "</td>" +
            "<td>" + escapeHtml(d.email || "—") + "</td>" +
            "<td>" + escapeHtml(d.role || "—") + "</td>" +
            "<td>" + escapeHtml(d.department || "—") + "</td>" +
            "<td>" + escapeHtml(d.section || "—") + "</td>" +
            "<td>" + (row.errors.length
                ? '<span class="badge badge-red">Needs fixing</span><ul class="issue-list">' +
                  row.errors.map(function(e) { return "<li>" + escapeHtml(e) + "</li>"; }).join("") + "</ul>"
                : '<span class="badge badge-green">Ready</span>') +
            "</td></tr>";
    });

    document.getElementById("bulkPreview").innerHTML = html + "</tbody></table></div>";

    const createButton = document.getElementById("bulkCreateButton");
    createButton.disabled = ready.length === 0;
    createButton.textContent = "Create " + ready.length + " account" + (ready.length === 1 ? "" : "s");
    document.getElementById("bulkActions").classList.remove("hidden");
    document.getElementById("bulkProblemReportButton").classList.toggle("hidden", problems === 0);

}


/* Step 4: create the ready accounts */
async function createBulkAccounts() {

    const ready = bulkRows.filter(function(r) { return r.errors.length === 0; });
    if (!ready.length) return;

    const mode = selectedPasswordMode("bulkPasswordMode");
    const how = mode === "link" ? "a password setup link" : "a temporary password";
    if (!confirm("Create " + ready.length + " account(s)? Each user will receive an email with " + how + ".")) {
        return;
    }

    const message = document.getElementById("bulkMessage");
    const createButton = document.getElementById("bulkCreateButton");
    const fileInput = document.getElementById("bulkFile");
    const progress = document.getElementById("bulkProgress");
    const bar = progress.querySelector(".progress-bar span");
    const label = progress.querySelector(".progress-label");

    createButton.disabled = true;
    fileInput.disabled = true;
    progress.classList.remove("hidden");
    bulkResults = [];

    for (let i = 0; i < ready.length; i += BULK_CREATE_BATCH) {

        const batch = ready.slice(i, i + BULK_CREATE_BATCH);
        label.textContent = "Creating accounts and sending emails... " + i + " / " + ready.length;
        bar.style.width = Math.round((i / ready.length) * 100) + "%";

        try {

            const response = await apiFetch("admin_bulk_create_users.php", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    password_mode: mode,
                    rows: batch.map(function(r) { return Object.assign({ row_number: r.row_number }, r.data); })
                })
            });

            const result = await readJson(response);

            if (!response.ok || !result.success) {
                batch.forEach(function(r) {
                    bulkResults.push({ row_number: r.row_number, status: "failed", errors: [{ message: result.message || "Server error." }] });
                });
                continue;
            }

            bulkResults = bulkResults.concat(result.results);

        } catch (error) {

            console.error("Bulk create error:", error);
            batch.forEach(function(r) {
                bulkResults.push({ row_number: r.row_number, status: "failed", errors: [{ message: "Unable to connect to the server." }] });
            });

        }

    }

    bar.style.width = "100%";
    label.textContent = "Finished.";
    fileInput.disabled = false;
    fileInput.value = "";

    renderBulkResults(mode);

}


function renderBulkResults(mode) {

    const byRow = {};
    bulkRows.forEach(function(r) { byRow[r.row_number] = r; });

    const created = bulkResults.filter(function(r) { return r.status === "created"; });
    const notCreated = bulkResults.filter(function(r) { return r.status !== "created"; });
    const emailFailed = created.filter(function(r) { return !r.email_sent; });
    const skipped = bulkRows.filter(function(r) { return r.errors.length > 0; });

    let html =
        '<div class="bulk-stats">' +
        '<div class="ok"><strong>' + created.length + "</strong><span>accounts created</span></div>" +
        "<div><strong>" + (created.length - emailFailed.length) + "</strong><span>emails sent</span></div>" +
        '<div class="bad"><strong>' + (skipped.length + notCreated.length) + "</strong><span>not created</span></div>" +
        "</div>";

    if (emailFailed.length) {
        html += '<div class="notice notice-warning"><strong>' + emailFailed.length +
            " account(s) were created but their email could not be sent.</strong>" +
            "<p>Download the results report – it contains their " +
            (mode === "link" ? "setup links" : "temporary passwords") +
            " so you can give them to the users another way. Keep that file private.</p></div>";
    }

    html += '<div class="table-container"><table class="bulk-table"><thead><tr>' +
        "<th>Row</th><th>Name</th><th>Email</th><th>Username</th><th>Result</th></tr></thead><tbody>";

    bulkResults.forEach(function(res) {
        const d = (byRow[res.row_number] || { data: {} }).data;
        let resultCell;
        if (res.status === "created") {
            resultCell = res.email_sent
                ? '<span class="badge badge-green">Created · email sent</span>'
                : '<span class="badge badge-yellow">Created · email failed</span>';
        } else {
            resultCell = '<span class="badge badge-red">Not created</span><ul class="issue-list">' +
                (res.errors || []).map(function(e) { return "<li>" + escapeHtml(e.message) + "</li>"; }).join("") + "</ul>";
        }
        html += "<tr><td>" + res.row_number + "</td>" +
            "<td>" + escapeHtml(((d.first_name || "") + " " + (d.last_name || "")).trim()) + "</td>" +
            "<td>" + escapeHtml(d.email || "") + "</td>" +
            "<td>" + escapeHtml(res.username || "—") + "</td>" +
            "<td>" + resultCell + "</td></tr>";
    });

    // Rows that were skipped before sending (missing / duplicate information)
    skipped.forEach(function(row) {
        const d = row.data;
        html += '<tr class="row-problem"><td>' + row.row_number + "</td>" +
            "<td>" + escapeHtml(((d.first_name || "") + " " + (d.last_name || "")).trim() || "—") + "</td>" +
            "<td>" + escapeHtml(d.email || "—") + "</td>" +
            "<td>—</td>" +
            '<td><span class="badge badge-red">Skipped</span><ul class="issue-list">' +
            row.errors.map(function(e) { return "<li>" + escapeHtml(e) + "</li>"; }).join("") +
            "</ul></td></tr>";
    });

    html += "</tbody></table></div>";

    document.getElementById("bulkSummary").innerHTML = "";
    document.getElementById("bulkPreview").innerHTML = "";
    document.getElementById("bulkActions").classList.add("hidden");
    document.getElementById("bulkResults").innerHTML = html;
    document.getElementById("bulkResultsActions").classList.remove("hidden");

    showMessage(
        document.getElementById("bulkMessage"),
        created.length + " account(s) created." + (skipped.length ? " " + skipped.length + " row(s) were skipped because of missing or duplicate information." : ""),
        created.length ? "success" : "error"
    );

}


/* Problem report: rows that need fixing */
function downloadBulkProblemReport() {

    const lines = [["row", "first_name", "last_name", "email", "problems"].join(",")];

    bulkRows.filter(function(r) { return r.errors.length; }).forEach(function(r) {
        lines.push([r.row_number, r.data.first_name, r.data.last_name, r.data.email, r.errors.join(" | ")].map(csvCell).join(","));
    });

    downloadText("uniroom_bulk_problems.csv", lines.join("\r\n") + "\r\n");

}


/* Results report: every row and what happened */
function downloadBulkResults() {

    const byRow = {};
    bulkResults.forEach(function(r) { byRow[r.row_number] = r; });

    const lines = [["row", "first_name", "last_name", "email", "username", "result", "email_sent", "temporary_password", "setup_link", "problems"].join(",")];

    bulkRows.forEach(function(r) {
        const res = byRow[r.row_number];
        const problems = res ? (res.errors || []).map(function(e) { return e.message; }) : r.errors;
        lines.push([
            r.row_number, r.data.first_name, r.data.last_name, r.data.email,
            res && res.username ? res.username : "",
            res && res.status === "created" ? "created" : "not created",
            res && res.status === "created" ? (res.email_sent ? "yes" : "no") : "",
            res && res.temporary_password ? res.temporary_password : "",
            res && res.setup_link ? res.setup_link : "",
            problems.join(" | ")
        ].map(csvCell).join(","));
    });

    downloadText("uniroom_bulk_results.csv", lines.join("\r\n") + "\r\n");

}


function resetBulk() {

    bulkRows = [];
    bulkResults = [];

    ["bulkSummary", "bulkPreview", "bulkResults"].forEach(function(id) {
        document.getElementById(id).innerHTML = "";
    });

    document.getElementById("bulkActions").classList.add("hidden");
    document.getElementById("bulkResultsActions").classList.add("hidden");
    document.getElementById("bulkProgress").classList.add("hidden");
    document.getElementById("bulkFileName").textContent = "No file chosen";
    showMessage(document.getElementById("bulkMessage"), "", "");

}

function startNewBulkUpload() {
    resetBulk();
    const input = document.getElementById("bulkFile");
    input.value = "";
    input.click();
}
