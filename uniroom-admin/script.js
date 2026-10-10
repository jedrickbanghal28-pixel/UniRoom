/* =====================================================
   API CONNECTION
   =====================================================
   The API address is set in config.js.
   apiFetch() adds the login token to every request.
===================================================== */

const API_BASE_URL = (
    (window.UNIROOM_CONFIG && window.UNIROOM_CONFIG.API_BASE_URL) ||
    "http://localhost:8000/api"
).replace(/\/+$/, "");

function getAuthToken() {
    return sessionStorage.getItem("uniroomToken") || "";
}

async function apiFetch(path, options) {
    options = options || {};
    const headers = Object.assign({}, options.headers || {});
    const token = getAuthToken();

    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }

    const response = await fetch(
        API_BASE_URL + "/" + path.replace(/^\/+/, ""),
        Object.assign({}, options, { headers: headers })
    );

    // Token expired or missing: go back to the login page.
    if (response.status === 401 && currentUser && path.indexOf("login") === -1) {
        logout();
        const message = document.getElementById("loginMessage");
        if (message) message.textContent = "Your session has expired. Please log in again.";
    }

    return response;
}

/* Reads a JSON response safely (returns {} if the server sent something else). */
async function readJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return {};
    }
}


/* =====================================================
   SAMPLE SYSTEM DATA
===================================================== */

const departments = {

    "Information Technology": {

        sections: [
            {
                name: "BSIT-A1",
                students: 35
            },
            {
                name: "BSIT-A2",
                students: 32
            },
            {
                name: "BSIT-B1",
                students: 30
            }
        ],

        faculty: [
            {
                name: "Farah Diva Alvarado",
                subject: "ITE 031 - IT Laboratory",
                schedule: "Wednesday 1:00 PM - 4:00 PM",
                room: "TH 309"
            },
            {
                name: "Almie Joy Batiancila Ilustrisimo",
                subject: "ITE 031 - Data Structure and Algorithms",
                schedule: "Friday 8:00 AM - 10:00 AM",
                room: "TH 308"
            },
            {
                name: "Marvy P Segarra",
                subject: "ITE 298 - Information Management",
                schedule: "Friday 10:30 AM - 12:30 PM",
                room: "TH 308"
            }
        ],

        students: [
            {
                id: "IT-001",
                name: "Stepin Kare",
                section: "BSIT-A1",
                email: "john@uniroom.com",
                status: "Active"
            },
            {
                id: "IT-002",
                name: "Keben Eleben",
                section: "BSIT-A1",
                email: "keben@uniroom.com",
                status: "Active"
            },
            {
                id: "IT-003",
                name: "Kai Sotto ginawang mundo ang dapat tao lang ;(",
                section: "BSIT-A2",
                email: "kai@uniroom.com",
                status: "Active"
            },
            {
                id: "IT-004",
                name: "Junmar Ni Bai",
                section: "BSIT-B1",
                email: "junmar@uniroom.com",
                status: "Active"
            }
        ],

        rooms: [
            {
                room: "Room 201",
                section: "BSIT-A1",
                status: "Assigned"
            },
            {
                room: "Room 204",
                section: "BSIT-A2",
                status: "Assigned"
            },
            {
                room: "Laboratory 1",
                section: "BSIT-B1",
                status: "Assigned"
            },
            {
                room: "Room 101",
                section: "-",
                status: "Available"
            }
        ],

        schedules: [
            {
                section: "BSIT-A1",
                subject: "ITE 031 - IT Laboratory",
                teacher: "Farah Diva Alvarado",
                day: "Wednesday",
                time: "1:00 PM - 4:00 PM",
                room: "TH 309"
            },
            {
                section: "BSIT-A1",
                subject: "ITE 031 - Data Structure and Algorithms",
                teacher: "Almie Joy Batiancila Ilustrisimo",
                day: "Friday",
                time: "8:00 AM - 10:00 AM",
                room: "TH 308"
            },
            {
                section: "BSIT-A1",
                subject: "ITE 298 - Information Management",
                teacher: "Marvy P Segarra",
                day: "Friday",
                time: "10:30 AM - 12:30 PM",
                room: "TH 308"
            }
        ]

    },


    "Medical Technology": {

        sections: [
            {
                name: "BSMLS-A1",
                students: 38
            },
            {
                name: "BSMLS-A2",
                students: 36
            }
        ],

        faculty: [
            {
                name: "Farah Diva Alvarado",
                subject: "ITE 031 - IT Laboratory",
                schedule: "Wednesday 8:00 AM - 12:00 PM",
                room: "TH 310, TH 311"
            },
            {
                name: "Almie Joy Batiancila Ilustrisimo",
                subject: "ITE 031 - Data Structure and Algorithms",
                schedule: "Monday and Thursday afternoons",
                room: "TH 305, TH 306"
            },
            {
                name: "Marvy P Segarra",
                subject: "ITE 298 - Information Management",
                schedule: "Monday and Thursday afternoons",
                room: "TH 307, TH 308"
            }
        ],

        students: [
            {
                id: "MED-001",
                name: "Alex Garcia",
                section: "BSMLS-A1",
                email: "alex@uniroom.com",
                status: "Active"
            },
            {
                id: "MED-002",
                name: "Maria Flores",
                section: "BSMLS-A1",
                email: "maria@uniroom.com",
                status: "Active"
            },
            {
                id: "MED-003",
                name: "Kevin Torres",
                section: "BSMLS-A2",
                email: "kevin@uniroom.com",
                status: "Active"
            }
        ],

        rooms: [
            {
                room: "Laboratory 2",
                section: "BSMLS-A1",
                status: "Assigned"
            },
            {
                room: "Laboratory 3",
                section: "BSMLS-A2",
                status: "Assigned"
            },
            {
                room: "Room 102",
                section: "-",
                status: "Available"
            }
        ],

        schedules: [
            {
                section: "BSMT-A1",
                subject: "ITE 031 - IT Laboratory",
                teacher: "Farah Diva Alvarado",
                day: "Wednesday",
                time: "8:00 AM - 10:00 AM",
                room: "TH 310"
            },
            {
                section: "BSMT-A1",
                subject: "ITE 031 - Data Structure and Algorithms",
                teacher: "Almie Joy Batiancila Ilustrisimo",
                day: "Monday",
                time: "1:00 PM - 3:00 PM",
                room: "TH 305"
            },
            {
                section: "BSMT-A2",
                subject: "ITE 298 - Information Management",
                teacher: "Marvy P Segarra",
                day: "Monday",
                time: "1:00 PM - 3:00 PM",
                room: "TH 307"
            }
        ]

    },


    "Nursing": {

        sections: [
            {
                name: "BSN-A1",
                students: 40
            },
            {
                name: "BSN-A2",
                students: 39
            }
        ],

        faculty: [
            {
                name: "Dr. Lisa Cruz",
                subject: "Anatomy",
                schedule: "Monday 10:00 AM - 12:00 PM",
                room: "Room 302"
            }
        ],

        students: [],

        rooms: [
            {
                room: "Room 302",
                section: "BSN-A1",
                status: "Assigned"
            }
        ],

        schedules: []

    },


    "Engineering": {

        sections: [
            {
                name: "BSME-A1",
                students: 33
            },
            {
                name: "BSEE-A1",
                students: 31
            }
        ],

        faculty: [
            {
                name: "Engr. Mark Diaz",
                subject: "Engineering Mathematics",
                schedule: "Wednesday 8:00 AM - 10:00 AM",
                room: "Room 305"
            }
        ],

        students: [],

        rooms: [
            {
                room: "Room 305",
                section: "BSME-A1",
                status: "Assigned"
            }
        ],

        schedules: []

    }

};


/* =====================================================
   USERS
===================================================== */

let users = JSON.parse(
    localStorage.getItem("uniroomUsers")
) || [

    {
        name: "System Administrator",
        email: "admin@uniroom.com",
        password: "123456",
        role: "admin",
        department: "Administration",
        status: "Active"
    },

    {
        name: "IT Faculty",
        email: "faculty.it@uniroom.com",
        password: "123456",
        role: "faculty",
        department: "Information Technology",
        status: "Active"
    },

    {
        name: "MedTech Faculty",
        email: "faculty.medtech@uniroom.com",
        password: "123456",
        role: "faculty",
        department: "Medical Technology",
        status: "Active"
    }
];


let currentUser = null;


/* =====================================================
   SAVE USERS
===================================================== */

function saveUsers() {

    localStorage.setItem(
        "uniroomUsers",
        JSON.stringify(users)
    );

}

/* =====================================================
   LOGOUT
===================================================== */

function logout() {

    currentUser = null;

    sessionStorage.removeItem(
        "uniroomCurrentUser"
    );

    sessionStorage.removeItem(
        "uniroomToken"
    );


    // Hide Admin Panel
    const adminApp =
        document.getElementById("adminApp");

    if (adminApp) {
        adminApp.classList.add("hidden");
    }


    // Hide Faculty Panel
    const facultyApp =
        document.getElementById("facultyApp");

    if (facultyApp) {
        facultyApp.classList.add("hidden");
    }


    // Show Login Page
    const loginPage =
        document.getElementById("loginPage");

    if (loginPage) {
        loginPage.classList.remove("hidden");
    }


    // Clear login fields
    const email =
        document.getElementById("email");

    const password =
        document.getElementById("password");

    if (email) {
        email.value = "";
    }

    if (password) {
        password.value = "";
    }


    // Clear login message
    const message =
        document.getElementById("loginMessage");

    if (message) {
        message.textContent = "";
    }

}

/* =====================================================
   LOGIN
===================================================== */

async function login() {

    const email =
        document.getElementById("email").value.trim();

    const password =
        document.getElementById("password").value.trim();

    const message =
        document.getElementById("loginMessage");


    /*
     * Required fields
     */
    if (!validateRequired(document.querySelector(".login-card"))) {

        message.textContent =
            "Please complete the required fields marked with *.";

        return;
    }


    message.textContent =
        "Logging in...";


    try {

        const response = await apiFetch(
            "admin_login.php",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: email,
                    password: password
                })
            }
        );


        const result =
            await readJson(response);


        if (!response.ok || !result.success) {

            message.textContent =
                result.message ||
                "Login failed.";

            return;
        }


        currentUser =
            result.user;


        sessionStorage.setItem(
            "uniroomCurrentUser",
            JSON.stringify(result.user)
        );

        sessionStorage.setItem(
            "uniroomToken",
            result.token || ""
        );


        message.textContent = "";


        /*
         * FIRST LOGIN WITH A TEMPORARY PASSWORD
         * The user must choose a new password before continuing.
         */
        if (result.must_change_password) {

            openChangePassword(email, password);

            return;
        }

        openHomeForRole();

    } catch (error) {

        console.error(error);

        message.textContent =
            "Unable to connect to the server.";

    }

}


/* =====================================================
   OPEN THE RIGHT PANEL AFTER LOGIN
===================================================== */

function openHomeForRole() {

    const message =
        document.getElementById("loginMessage");

    const result = {
        user: currentUser
    };


        /*
         * ADMIN LOGIN
         */
        if (result.user.role === "admin") {

            document
                .getElementById("loginPage")
                .classList.add("hidden");

            document
                .getElementById("adminApp")
                .classList.remove("hidden");

            document
                .getElementById("facultyApp")
                .classList.add("hidden");


            showAdminPage(
                "adminDashboard",
                null
            );

        }


        /*
         * FACULTY LOGIN
         */
        else if (result.user.role === "faculty") {

            document
                .getElementById("loginPage")
                .classList.add("hidden");

            document
                .getElementById("facultyApp")
                .classList.remove("hidden");

            document
                .getElementById("adminApp")
                .classList.add("hidden");


            showFacultyPage(
                "facultyDashboard",
                null
            );

        }


        /*
         * OTHER ROLES
         */
        else {

            message.textContent =
                "You are not allowed to access the admin website.";

        }

}


/* =====================================================
   FIRST-LOGIN PASSWORD CHANGE
===================================================== */

let pendingLogin = null;

function openChangePassword(login, temporaryPassword) {

    pendingLogin = {
        login: login,
        password: temporaryPassword
    };

    const modal =
        document.getElementById("changePasswordModal");

    clearFormErrors(modal);

    document.getElementById("cpNewPassword").value = "";
    document.getElementById("cpConfirmPassword").value = "";

    showMessage(
        document.getElementById("cpMessage"),
        "",
        ""
    );

    modal.classList.remove("hidden");

    document.getElementById("cpNewPassword").focus();

}

async function submitChangePassword() {

    const modal =
        document.getElementById("changePasswordModal");

    const message =
        document.getElementById("cpMessage");

    const newPassword =
        document.getElementById("cpNewPassword");

    const confirmPassword =
        document.getElementById("cpConfirmPassword");

    if (!validateRequired(modal)) {
        showMessage(message, "Please complete the required fields marked with *.", "error");
        return;
    }

    if (newPassword.value !== confirmPassword.value) {
        setFieldError(confirmPassword, "Passwords do not match.");
        showMessage(message, "Passwords do not match.", "error");
        return;
    }

    showMessage(message, "Saving...", "");

    try {

        const response = await apiFetch("change_password.php", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                login: pendingLogin.login,
                current_password: pendingLogin.password,
                new_password: newPassword.value
            })
        });

        const result = await readJson(response);

        if (!response.ok || !result.success) {
            showServerErrors(result.errors, { new_password: "cpNewPassword" });
            showMessage(message, result.message || "Unable to change password.", "error");
            return;
        }

        currentUser.must_change_password = false;
        pendingLogin = null;
        modal.classList.add("hidden");
        openHomeForRole();

    } catch (error) {

        console.error(error);
        showMessage(message, "Unable to connect to the server.", "error");

    }

}

function cancelChangePassword() {

    document
        .getElementById("changePasswordModal")
        .classList.add("hidden");

    pendingLogin = null;
    logout();

}


/* =====================================================
   ADMIN DASHBOARD
===================================================== */

async function loadAdminDashboard() {

    try {

        const response = await apiFetch(
            "admin_dashboard.php"
        );


        const result =
            await response.json();


        if (!response.ok || !result.success) {

            console.error(
                result.message ||
                "Unable to load dashboard."
            );

            return;
        }


        /*
         * Dashboard statistics
         */

        const totalStudents =
            document.getElementById(
                "totalStudents"
            );

        if (totalStudents) {

            totalStudents.textContent =
                result.statistics.students;

        }


        const totalStaff =
            document.getElementById(
                "totalStaff"
            );

        if (totalStaff) {

            totalStaff.textContent =
                result.statistics.faculty;

        }


        const totalDepartments =
            document.getElementById(
                "totalDepartments"
            );

        if (totalDepartments) {

            totalDepartments.textContent =
                result.statistics.departments;

        }


        const totalSections =
            document.getElementById(
                "totalSections"
            );

        if (totalSections) {

            totalSections.textContent =
                result.statistics.sections;

        }


        const activeUsers =
            document.getElementById(
                "activeUsers"
            );

        if (activeUsers) {

            activeUsers.textContent =
                result.statistics.active_users;

        }


        const facultyAccounts =
            document.getElementById(
                "facultyAccounts"
            );

        if (facultyAccounts) {

            facultyAccounts.textContent =
                result.statistics.faculty;

        }


        const studentAccounts =
            document.getElementById(
                "studentAccounts"
            );

        if (studentAccounts) {

            studentAccounts.textContent =
                result.statistics.students;

        }


        /*
         * Department Overview
         */

        const container =
            document.getElementById(
                "adminDepartmentOverview"
            );


        if (!container) {

            console.warn(
                "Department overview container not found."
            );

            return;
        }


        container.innerHTML = "";


        if (
            !result.departments ||
            result.departments.length === 0
        ) {

            container.innerHTML = `
                <p>No departments found.</p>
            `;

            return;
        }


        result.departments.forEach(
            function(department) {

                container.innerHTML += `

                    <div class="department-row">

                        <div>

                            <strong>
                                ${department.code}
                            </strong>

                            <small>
                                ${department.sections}
                                sections
                            </small>

                        </div>


                        <div class="department-count">

                            <strong>
                                ${department.students}
                            </strong>

                            <small>
                                students
                            </small>

                        </div>

                    </div>

                `;

            }
        );


    } catch (error) {

        console.error(
            "Dashboard error:",
            error
        );

    }

}


/* =====================================================
   ADMIN NAVIGATION
===================================================== */

function showAdminPage(
    pageId,
    button
) {

    const pages =
        document.querySelectorAll(
            "#adminApp .page"
        );


    pages.forEach(
        function(page) {

            page.classList.remove(
                "active-page"
            );

        }
    );


    const selectedPage =
        document.getElementById(
            pageId
        );


    if (!selectedPage) {

        console.error(
            "Admin page not found:",
            pageId
        );

        return;
    }


    selectedPage.classList.add(
        "active-page"
    );


    const buttons =
        document.querySelectorAll(
            "#adminApp .nav-button"
        );


    buttons.forEach(
        function(item) {

            item.classList.remove(
                "active"
            );

        }
    );


    if (button) {

        button.classList.add(
            "active"
        );

    }


    const title =
        document.getElementById(
            "adminPageTitle"
        );


    const titles = {

        adminDashboard:
            "Admin Dashboard",

        adminDepartments:
            "Departments",

        adminUsers:
            "User Management",

        adminAccounts:
            "Account Creation"

    };


    if (title) {

        title.textContent =
            titles[pageId] ||
            "Admin Panel";

    }

    /*
     * Load Dashboard
     */
    if (
        pageId ===
        "adminDashboard"
    ) {

        loadAdminDashboard();

    }


    /*
     * Load Users
     */
    if (
        pageId ===
        "adminUsers"
    ) {

        loadUsersTable();

    }


    /*
     * Load Account Creation (departments, sections)
     */
    if (
        pageId ===
        "adminAccounts"
    ) {

        loadAccountFormOptions();

    }
    /*
 * Load Departments
 */
if (
    pageId ===
    "adminDepartments"
) {

    loadDepartments();

}

}

/* =====================================================
   ADMIN USER MANAGEMENT
===================================================== */

async function loadUsersTable() {

    const table =
        document.getElementById(
            "usersTable"
        );


    if (!table) {

        console.error(
            "usersTable not found."
        );

        return;
    }


    table.innerHTML = `
        <tr>
            <td colspan="8">
                Loading users...
            </td>
        </tr>
    `;


    try {

        const response =
            await apiFetch(
                "admin_users.php"
            );


        const result =
            await readJson(response);


        if (
            !response.ok ||
            !result.success
        ) {

            table.innerHTML = `
                <tr>
                    <td colspan="8">
                        Unable to load users.
                    </td>
                </tr>
            `;


            console.error(
                result.message ||
                "Unable to load users."
            );


            return;
        }


        table.innerHTML = "";


        if (
            !result.users ||
            result.users.length === 0
        ) {

            table.innerHTML = `
                <tr>
                    <td colspan="8">
                        No users found.
                    </td>
                </tr>
            `;


            return;
        }


        result.users.forEach(
            function(user) {

                const fullName =
                    (user.first_name || "") +
                    " " +
                    (user.last_name || "");

                const pending =
                    user.status === "Pending setup";

                const statusBadge = pending
                    ? '<span class="badge badge-yellow">Pending setup</span>'
                    : '<span class="badge badge-green">Active</span>';

                const resendButton = pending
                    ? '<button class="link-btn" onclick="resendInvite(' + Number(user.id) + ', this)">Resend email</button>'
                    : "";

                table.innerHTML += `

                    <tr>

                        <td>${escapeHtml(fullName.trim())}</td>

                        <td>${escapeHtml(user.email || "—")}</td>

                        <td>${escapeHtml(capitalize(user.role || "—"))}</td>

                        <td>${escapeHtml(user.department || "—")}</td>

                        <td>${escapeHtml(user.section || "—")}</td>

                        <td>${escapeHtml(user.gender || "—")}</td>

                        <td>${escapeHtml(user.age || "—")}</td>

                        <td>${statusBadge} ${resendButton}</td>

                    </tr>

                `;

            }
        );


    } catch (error) {

        console.error(
            "User management error:",
            error
        );


        table.innerHTML = `
            <tr>
                <td colspan="8">
                    Unable to connect to the server.
                </td>
            </tr>
        `;

    }

}
/* Send the account email again (new setup link) */
async function resendInvite(userId, button) {

    if (!confirm("Send a new password setup link to this user? The old link will stop working.")) {
        return;
    }

    button.disabled = true;
    button.textContent = "Sending...";

    try {

        const response = await apiFetch("admin_resend_invite.php", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ user_id: userId, password_mode: "link" })
        });

        const result = await readJson(response);

        if (!response.ok || !result.success) {
            alert(result.message || "Unable to resend the email.");
        } else if (!result.email_sent) {
            alert(
                "The email could not be sent (" + (result.email_error || "unknown error") + ").\n\n" +
                "Give the user this setup link instead:\n" + (result.setup_link || "")
            );
        } else {
            alert(result.message);
        }

    } catch (error) {

        console.error(error);
        alert("Unable to connect to the server.");

    }

    button.disabled = false;
    button.textContent = "Resend email";

}

/* =====================================================
   ADMIN DEPARTMENTS
===================================================== */

async function loadDepartments() {

    const container =
        document.getElementById(
            "departmentCards"
        );


    if (!container) {

        console.error(
            "departmentCards not found."
        );

        return;
    }


    container.innerHTML = `
        <p>Loading departments...</p>
    `;


    try {

        const response =
            await apiFetch(
                "admin_departments.php"
            );


        const result =
            await response.json();


        if (
            !response.ok ||
            !result.success
        ) {

            container.innerHTML = `
                <p>Unable to load departments.</p>
            `;

            console.error(
                result.message ||
                "Unable to load departments."
            );

            return;
        }


        container.innerHTML = "";


        if (
            !result.departments ||
            result.departments.length === 0
        ) {

            container.innerHTML = `
                <p>No departments found.</p>
            `;

            return;
        }


        result.departments.forEach(
            function(department) {

                container.innerHTML += `

                    <div class="department-card">

                        <h3>
                            ${department.code}
                        </h3>

                        <p>
                            ${department.sections}
                            section(s)
                        </p>

                        <p>
                            ${department.students}
                            student(s)
                        </p>

                    </div>

                `;

            }
        );


    } catch (error) {

        console.error(
            "Department error:",
            error
        );


        container.innerHTML = `
            <p>Unable to connect to the server.</p>
        `;

    }

}

/* =====================================================
   FACULTY DASHBOARD
===================================================== */

async function loadFacultyDashboard() {

    if (!currentUser) {

        console.error(
            "No faculty user is logged in."
        );

        return;
    }


    try {

        const response =
            await apiFetch(
                "faculty_dashboard.php?user_id=" +
                currentUser.id
            );


        const result =
            await response.json();


        if (
            !response.ok ||
            !result.success
        ) {

            console.error(
                result.message ||
                "Unable to load faculty dashboard."
            );

            return;
        }


        /*
         * Faculty name
         */

        const facultyName =
            result.faculty.first_name +
            " " +
            result.faculty.last_name;


        const department =
            result.department.code;


        /*
         * Update faculty information
         */

        const nameElement =
            document.getElementById(
                "facultyName"
            );

        if (nameElement) {

            nameElement.textContent =
                facultyName;

        }


        const departmentElement =
            document.getElementById(
                "facultyDepartment"
            );

        if (departmentElement) {

            departmentElement.textContent =
                department;

        }


        /*
         * Update avatars
         */

        const initial =
            result.faculty.first_name
                .charAt(0)
                .toUpperCase();


        const topAvatar =
            document.getElementById(
                "facultyTopAvatar"
            );

        if (topAvatar) {

            topAvatar.textContent =
                initial;

        }


        const avatar =
            document.getElementById(
                "facultyAvatar"
            );

        if (avatar) {

            avatar.textContent =
                initial;

        }


        /*
         * Dashboard title
         */

        const dashboardTitle =
            document.getElementById(
                "facultyDashboardTitle"
            );

        if (dashboardTitle) {

            dashboardTitle.textContent =
                department +
                " Dashboard";

        }


        /*
         * Calculate student total
         */

        let totalStudents = 0;


        result.sections.forEach(
            function(section) {

                totalStudents +=
                    Number(section.students);

            }
        );


        /*
         * Update statistics
         */

        const studentTotal =
            document.getElementById(
                "facultyStudentTotal"
            );

        if (studentTotal) {

            studentTotal.textContent =
                totalStudents;

        }


        const sectionTotal =
            document.getElementById(
                "facultySectionTotal"
            );

        if (sectionTotal) {

            sectionTotal.textContent =
                result.sections.length;

        }


        /*
         * Faculty members and rooms
         * will be connected later.
         */

        const facultyTotal =
            document.getElementById(
                "facultyMemberTotal"
            );

        if (facultyTotal) {

            facultyTotal.textContent =
                "—";

        }


        const roomTotal =
            document.getElementById(
                "facultyRoomTotal"
            );

        if (roomTotal) {

            roomTotal.textContent =
                "—";

        }


        /*
         * Department sections
         */

        const sectionContainer =
            document.getElementById(
                "facultySectionOverview"
            );


        if (sectionContainer) {

            sectionContainer.innerHTML = "";


            if (
                !result.sections ||
                result.sections.length === 0
            ) {

                sectionContainer.innerHTML =
                    "<p>No sections found.</p>";

            } else {

                result.sections.forEach(
                    function(section) {

                        sectionContainer.innerHTML += `

                            <div class="department-row">

                                <div>

                                    <strong>
                                        ${section.name}
                                    </strong>

                                    <small>
                                        Department Section
                                    </small>

                                </div>

                                <div class="department-count">

                                    <strong>
                                        ${section.students}
                                    </strong>

                                    <small>
                                        students
                                    </small>

                                </div>

                            </div>

                        `;

                    }
                );

            }

        }


        /*
         * Faculty section cards
         */

        const sectionCards =
            document.getElementById(
                "facultySectionCards"
            );


        if (sectionCards) {

            sectionCards.innerHTML = "";


            result.sections.forEach(
                function(section) {

                    sectionCards.innerHTML += `

                        <div class="section-card">

                            <h3>
                                ${section.name}
                            </h3>

                            <p class="section-info">
                                ${section.students}
                                enrolled students
                            </p>

                        </div>

                    `;

                }
            );

        }


        /*
         * Today's classes
         * Schedules will be connected later.
         */

        const todayClasses =
            document.getElementById(
                "todayClasses"
            );

        if (todayClasses) {

            todayClasses.innerHTML =
                "<p>No schedules available.</p>";

        }


    } catch (error) {

        console.error(
            "Faculty dashboard error:",
            error
        );

    }

}

/* =====================================================
   FACULTY NAVIGATION
===================================================== */

function showFacultyPage(
    pageId,
    button
) {

    const pages =
        document.querySelectorAll(
            "#facultyApp .page"
        );


    pages.forEach(
        function(page) {

            page.classList.remove(
                "active-page"
            );

        }
    );


    const selectedPage =
        document.getElementById(
            pageId
        );


    if (!selectedPage) {

        console.error(
            "Faculty page not found:",
            pageId
        );

        return;
    }


    selectedPage.classList.add(
        "active-page"
    );


    const buttons =
        document.querySelectorAll(
            "#facultyApp .nav-button"
        );


    buttons.forEach(
        function(item) {

            item.classList.remove(
                "active"
            );

        }
    );


    if (button) {

        button.classList.add(
            "active"
        );

    }


    const title =
        document.getElementById(
            "facultyPageTitle"
        );


    const titles = {

        facultyDashboard:
            "Faculty Dashboard",

        facultySections:
            "Sections",

        facultyMembers:
            "Faculty Members",

        facultySchedule:
            "Schedule",

        facultyRooms:
            "Room Tagging",

        facultyStudents:
            "Student Records"

    };

if (title) {

    title.textContent =
        titles[pageId] ||
        "Faculty Panel";

}


if (
    pageId === "facultySchedule" ||
    pageId === "facultyRooms"
) {
    loadScheduleOptions();
}

if (pageId === "facultySchedule") {
    loadSchedule();
}


/*
 * Load database data
 */

if (pageId === "facultyDashboard") {
    loadFacultyDashboard();
}

if (pageId === "facultyStudents") {
    loadFacultyStudents();
}

if (pageId === "facultyMembers") {
    loadFacultyMembers();
}
}

/* =====================================================
   FACULTY STUDENT RECORDS
===================================================== */

async function loadFacultyStudents() {

    const table = document.getElementById("studentRecordsTable");

    if (!table) {
        console.error("studentRecordsTable not found.");
        return;
    }

    table.innerHTML = `
        <tr>
            <td colspan="5">Loading students...</td>
        </tr>
    `;

    try {

        const response = await apiFetch(
            "faculty_students.php?user_id=" +
            currentUser.id
        );

        const result = await response.json();

        if (!response.ok || !result.success) {

            table.innerHTML = `
                <tr>
                    <td colspan="5">
                        ${result.message || "Unable to load students."}
                    </td>
                </tr>
            `;

            console.error(result.message);

            return;
        }

        table.innerHTML = "";

        if (!result.students || result.students.length === 0) {

            table.innerHTML = `
                <tr>
                    <td colspan="5">
                        No student records found.
                    </td>
                </tr>
            `;

            return;
        }

        result.students.forEach(function(student) {

            const fullName =
                (student.first_name || "") +
                " " +
                (student.last_name || "");

            table.innerHTML += `
                <tr>

                    <td>
                        ${student.student_id || "—"}
                    </td>

                    <td>
                        ${fullName.trim()}
                    </td>

                    <td>
                        ${student.section || "—"}
                    </td>

                    <td>
                        ${student.email || "—"}
                    </td>

                    <td>
                        <span class="badge badge-green">
                            Active
                        </span>
                    </td>

                </tr>
            `;

        });

    } catch (error) {

        console.error(
            "Faculty student records error:",
            error
        );

        table.innerHTML = `
            <tr>
                <td colspan="5">
                    Unable to connect to the server.
                </td>
            </tr>
        `;

    }

}

/* =====================================================
   FACULTY SECTIONS
===================================================== */

function loadFacultySections(department) {

    const container =
        document.getElementById(
            "facultySectionCards"
        );


    container.innerHTML = "";


    department.sections
        .forEach(function(section) {

            const schedules =
                department.schedules
                    .filter(function(item) {

                        return item.section ===
                            section.name;

                    });


            let scheduleHTML = "";


            schedules.forEach(function(schedule) {

                scheduleHTML += `

                    <div class="section-detail">

                        <span>
                            ${schedule.subject}
                        </span>

                        <strong>
                            ${schedule.room}
                        </strong>

                    </div>

                    <div class="section-detail">

                        <span>
                            Teacher
                        </span>

                        <strong>
                            ${schedule.teacher}
                        </strong>

                    </div>

                    <div class="section-detail">

                        <span>
                            Schedule
                        </span>

                        <strong>
                            ${schedule.day},
                            ${schedule.time}
                        </strong>

                    </div>

                `;

            });


            container.innerHTML += `

                <div class="section-card">

                    <h3>
                        ${section.name}
                    </h3>

                    <p class="section-info">
                        ${section.students}
                        enrolled students
                    </p>

                    ${scheduleHTML}

                </div>

            `;

        });


    populateSectionDropdowns(department);

}


/* =====================================================
   FACULTY MEMBERS
===================================================== */

async function loadFacultyMembers() {

    const table =
        document.getElementById("facultyMembersTable");

    if (!table) {
        console.error("facultyMembersTable not found.");
        return;
    }

    table.innerHTML = `
        <tr>
            <td colspan="4">
                Loading faculty members...
            </td>
        </tr>
    `;

    try {

        const response = await apiFetch(
            "faculty_members.php?user_id=" +
            currentUser.id
        );

        const result = await response.json();

        if (!response.ok || !result.success) {

            table.innerHTML = `
                <tr>
                    <td colspan="4">
                        ${result.message ||
                        "Unable to load faculty members."}
                    </td>
                </tr>
            `;

            console.error(result.message);

            return;
        }

        table.innerHTML = "";

        if (!result.faculty || result.faculty.length === 0) {

            table.innerHTML = `
                <tr>
                    <td colspan="4">
                        No faculty members found.
                    </td>
                </tr>
            `;

            return;
        }

        result.faculty.forEach(function(member) {

            const fullName =
                (member.first_name || "") +
                " " +
                (member.last_name || "");

            const schedule =
                (member.schedule || "—")
                    .split("; ")
                    .map(escapeHtml)
                    .join("<br>");

            table.innerHTML += `
                <tr>

                    <td>
                        <strong>${escapeHtml(fullName.trim())}</strong>
                    </td>

                    <td>
                        ${escapeHtml(member.subjects || "—")}
                    </td>

                    <td>
                        ${schedule}
                    </td>

                    <td>
                        ${escapeHtml(member.rooms || "—")}
                    </td>

                </tr>
            `;

        });

    } catch (error) {

        console.error(
            "Faculty members error:",
            error
        );

        table.innerHTML = `
            <tr>
                <td colspan="4">
                    Unable to connect to the server.
                </td>
            </tr>
        `;

    }

}

/* =====================================================
   SCHEDULE
===================================================== */

async function loadSchedule() {

    const container =
        document.getElementById("scheduleList");

    if (!container) {
        console.error("scheduleList not found.");
        return;
    }

    container.innerHTML = `
        <p>Loading schedules...</p>
    `;

    try {

        const response = await apiFetch(
            "faculty_schedule.php?user_id=" +
            currentUser.id
        );

        const result = await response.json();

        if (!response.ok || !result.success) {

            container.innerHTML = `
                <p>
                    ${result.message ||
                    "Unable to load schedules."}
                </p>
            `;

            console.error(result.message);

            return;
        }

        container.innerHTML = "";

        if (!result.schedules || result.schedules.length === 0) {

            container.innerHTML =
                "<p>No schedules found.</p>";

            return;
        }

        result.schedules.forEach(function(schedule) {

            container.innerHTML += `
                <div class="schedule-row">

                    <strong>
                        ${schedule.section}
                    </strong>

                    <span>
                        ${schedule.subject}
                    </span>

                    <span>
                        ${schedule.faculty}
                    </span>

                    <span>
                        ${schedule.day_of_week}
                        ${formatTime(schedule.start_time)}
                        -
                        ${formatTime(schedule.end_time)}
                    </span>

                    <span>
                        ${schedule.room}
                    </span>

                </div>
            `;

        });

    } catch (error) {

        console.error(
            "Faculty schedule error:",
            error
        );

        container.innerHTML = `
            <p>
                Unable to connect to the server.
            </p>
        `;

    }

}

/* =====================================================
   ROOM TAGGING
===================================================== */

async function assignRoom() {

    const sectionId =
        document.getElementById("roomSection").value;

    const roomId =
        document.getElementById("roomRoom").value;

    const roomMessage =
        document.getElementById("roomMessage");

    if (!validateRequired(document.getElementById("roomForm"))) {

        showMessage(
            roomMessage,
            "Please complete the required fields marked with *.",
            "error"
        );

        return;
    }

    try {

        const response = await apiFetch(
            "assign_room.php",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    section_id: sectionId,
                    room_id: roomId
                })
            }
        );

        const result =
            await response.json();

        if (!response.ok || !result.success) {

            alert(
                result.message ||
                "Unable to assign room."
            );

            return;
        }

        alert("Room assigned successfully.");

    } catch (error) {

        console.error(
            "Assign room error:",
            error
        );

        alert(
            "Unable to connect to the server."
        );

    }

}

/* =====================================================
   STUDENT RECORDS
===================================================== */

function loadStudentRecords(department) {

    const table =
        document.getElementById(
            "studentRecordsTable"
        );


    table.innerHTML = "";


    if (department.students.length === 0) {

        table.innerHTML = `

            <tr>

                <td colspan="5">
                    No student records available.
                </td>

            </tr>

        `;

        return;
    }


    department.students
        .forEach(function(student) {

            table.innerHTML += `

                <tr>

                    <td>
                        ${student.id}
                    </td>

                    <td>
                        ${student.name}
                    </td>

                    <td>
                        ${student.section}
                    </td>

                    <td>
                        ${student.email}
                    </td>

                    <td>

                        <span
                            class="badge badge-green"
                        >
                            ${student.status}
                        </span>

                    </td>

                </tr>

            `;

        });

}


/* =====================================================
   SECTION DROPDOWNS
===================================================== */

async function loadScheduleOptions() {

    try {

        const response = await apiFetch(
            "schedule_options.php"
        );

        const result = await response.json();

        if (!result.success) {
            console.error(
                "Unable to load schedule options."
            );
            return;
        }


        const scheduleSection =
            document.getElementById(
                "scheduleSection"
            );

        const scheduleSubject =
            document.getElementById(
                "scheduleSubject"
            );

        const scheduleTeacher =
            document.getElementById(
                "scheduleTeacher"
            );

        const scheduleRoom =
            document.getElementById(
                "scheduleRoom"
            );

        const roomSection =
            document.getElementById(
                "roomSection"
            );

        const roomRoom =
            document.getElementById(
                "roomRoom"
            );


        /* =========================
           SECTIONS
        ========================= */

        /*
         * Every list starts with an empty "Select ..." option so the
         * required-field check can tell when nothing was chosen.
         */
        function fillSelect(select, placeholder, items, valueOf, labelOf) {

            const previous = select.value;

            select.innerHTML =
                '<option value="">' + placeholder + '</option>';

            items.forEach(function(item) {
                select.innerHTML +=
                    '<option value="' + escapeHtml(valueOf(item)) + '">' +
                    escapeHtml(labelOf(item)) +
                    '</option>';
            });

            select.value = previous;
        }

        const id = function(item) { return item.id; };

        fillSelect(scheduleSection, "Select section", result.sections, id,
            function(section) { return section.name; });

        fillSelect(roomSection, "Select section", result.sections, id,
            function(section) { return section.name; });

        fillSelect(scheduleSubject, "Select subject", result.subjects, id,
            function(subject) { return subject.code + " - " + subject.name; });

        fillSelect(scheduleTeacher, "Select faculty", result.faculty, id,
            function(member) { return member.first_name + " " + member.last_name; });

        fillSelect(scheduleRoom, "Select room", result.rooms, id,
            function(room) { return room.room_name; });

        fillSelect(roomRoom, "Select room", result.rooms, id,
            function(room) { return room.room_name; });

    } catch (error) {

        console.error(
            "Schedule options error:",
            error
        );

    }

}


/* =====================================================
   HELPER
===================================================== */

/* "13:30:00" → "1:30 PM" */
function formatTime(value) {

    const match = /^(\d{1,2}):(\d{2})/.exec(String(value || ""));
    if (!match) return value || "";

    const hours = Number(match[1]);
    const period = hours >= 12 ? "PM" : "AM";

    return ((hours % 12) || 12) + ":" + match[2] + " " + period;

}

function capitalize(text) {

    return text.charAt(0).toUpperCase() +
        text.slice(1);

}
