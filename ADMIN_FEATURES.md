# UNIROOM Admin Website – New Features

This document explains each recommended feature as it is built in the system. You can reuse it for your documentation.

---

## 1. Individual and Bulk Account Creation

Administrators can create user accounts **individually** or through **bulk account creation**.

**Individual** (Account Creation → *Individual account*): the administrator fills in one form per user.

**Bulk** (Account Creation → *Bulk upload (CSV / Excel)*): the administrator registers many students or faculty members at once by uploading a **CSV or Excel (.xlsx)** file.

1. Download the CSV template (one account per row).
2. Fill in the required columns.
3. Choose how users get their password (setup link or temporary password).
4. Upload the file. The system checks every row **before** creating anything and shows a preview:
   - **Ready**: the row is complete and will be created.
   - **Needs fixing**: the reason is shown, for example:
     - *Missing First Name.*
     - *Missing Age.*
     - *Duplicate email in this file (also in row 6).*
     - *Email address is already registered.*
     - *Student ID is already registered.*
     - *Department "Engineering" does not exist.*
     - *Section BSMT-A1 is not part of BSIT.*
5. Click **Create N accounts**. Rows with problems are skipped. The results table shows each row as *created*, *email sent* or *skipped*.

The administrator can download:
- the **list of problems**, to fix the file and upload it again
- the **results report** (CSV) for records

The file is sent to the server in small batches, so large files (up to 1,000 rows) finish without timing out. The server checks every row again, so wrong data cannot be saved even if someone bypasses the website.

File columns (header names are not case-sensitive):

| Column | Required | Notes |
|---|---|---|
| `first_name` | Yes | A single `full name` column is also accepted ("Dela Cruz, Juan" or "Juan Dela Cruz") |
| `last_name` | Yes | |
| `email` | Yes | Must be valid and not already registered |
| `gender` | Yes | Male, Female or Prefer not to say (M / F also accepted) |
| `age` | Yes | Whole number from 15 to 100 |
| `role` | Yes | student or faculty |
| `department` | Yes | Department code (e.g. BSIT) or name |
| `section` | Students | Must belong to the department |
| `student_id` | Students | Must be unique |
| `employee_id` | No | Faculty |
| `contact_number` | No | |

**Files:**
- Website: `accounts.js`, `spreadsheet-reader.js`
- API: `admin_create_user.php`, `admin_bulk_create_users.php`, `lib/accounts.php`

---

## 2. Required User Information

During account creation the administrator must provide the user's required information. Fields marked with an asterisk (\*) are mandatory, and the account cannot be created until they are completed.

| Field | Required |
|---|---|
| First Name / Last Name (Full Name) | \* |
| Email Address | \* |
| Gender | \* |
| Age | \* |
| Role | \* |
| Department | \* |
| Section | \* for students |
| Student ID | \* for students |
| Employee ID | optional (faculty) |
| Contact Number | optional |

The Section and Student ID fields appear only when the role is **Student**, and Employee ID only for **Faculty**. The Section list shows only the sections of the chosen department.

**Database:** `02_admin_features.sql` adds `gender`, `age`, `department_id` and `employee_id` to the `users` table. It also adds checks so only valid genders and ages from 15 to 100 can be saved.

---

## 3. Email Notification and Temporary Password / Password Setup Link

After an account is created, the system sends an **account creation email** to the user's registered Gmail address. The email lists the account details (name, email, username, role, department, section or student ID). It then gives the user a way to get into the account. The administrator chooses one of two options:

| Option | How it works |
|---|---|
| **Password setup link** (recommended, default) | The email has a **Set my password** button. The link works **once** and **expires after 48 hours**. The user opens it, chooses their own password (at least 8 characters with letters and numbers) and can then log in. |
| **Temporary password** | The email contains a random 10-character password. On the **first login** the system requires the user to change it before they can continue. |

**Why the link is safer:** the password is never written in the email, so an old or forwarded email cannot be used to log in. The link expires and works only once. The server stores only a SHA-256 fingerprint of the link, never the link itself.

Other rules:
- Until the user sets their password, User Management shows the account as **Pending setup**. The administrator can click **Resend email**, which sends a new link and makes the old one stop working.
- If the email cannot be sent (for example, Gmail is not set up yet), the account is still created. The administrator sees the temporary password or setup link to give to the user another way.
- Emails are sent through Gmail SMTP using a Google App Password.

**Files:**
- API: `lib/mailer.php`, `set_password.php`, `change_password.php`, `admin_resend_invite.php`
- Website: `set-password.html`, the password window in `index.html`
- Table: `password_tokens`. Column: `users.must_change_password`

---

## 4. Automatic Schedule Conflict Detection

Before saving a schedule, the system checks **room, faculty and section availability**. If any of them is already assigned to another class during an overlapping time on the same day, the system shows a notification and **does not save** the schedule.

| Conflict | Rule |
|---|---|
| **Room conflict** | Two classes cannot be assigned to the same room at overlapping times. |
| **Faculty conflict** | A faculty member cannot be assigned to two classes at overlapping times. |
| **Section conflict** | A section cannot have two scheduled classes at overlapping times. |

Two times overlap when `existing start < new end` **and** `new start < existing end`. A class from 8:00 to 10:00 does **not** conflict with one that starts at 10:00.

Example notification:

> **Scheduling Conflict**
> The selected room is already occupied during this time. Please choose another room or schedule.
> - Room conflict: TH 309 is used by ITE 031 IT Laboratory (BSIT-A1) on Monday 8:00 AM – 10:00 AM.
> - Faculty conflict: Farah Diva Alvarado teaches ITE 031 IT Laboratory (BSIT-A1) in TH 309 on Monday 8:00 AM – 10:00 AM.
>
> *The schedule was not saved.*

The check runs on the server, so it also protects schedules added from other tools. It also checks that the end time is later than the start time.

**Files:**
- API: `add_schedule.php`
- Website: `schedule.js`

---

## 5. Required Field Validation

Mandatory input fields are marked with an asterisk (\*). When a user or administrator submits a form with missing information:
- each incomplete field is **highlighted in red**, with a message under it (e.g. *Age is required.*)
- the cursor moves to the first incomplete field
- the message *"Please complete the required fields marked with \*."* appears
- the red highlight disappears as soon as the field is filled in

This applies to:
- Login
- Account Creation
- Add Schedule
- Assign Room
- Set Password
- the first-login password change

The API checks the same fields again and returns the exact field that is wrong.

**Files:**
- Website: `validation.js`
- API: each endpoint

---

## 6. Updated System Features Summary

The UniRoom system includes:
- individual and bulk account creation (CSV or Excel)
- required user information such as gender and age
- email notifications for newly created accounts
- a secure password setup link or a temporary password that must be changed at first login
- mandatory field validation
- automatic schedule conflict detection

These features support more organized user management and help prevent duplicate room, faculty and section assignments.

---

## Faculty and courses (BSIT and BSMEDTECH)

| Faculty | Course handled | Programs |
|---|---|---|
| Farah Diva Alvarado | ITE 031 – IT Laboratory | BSIT, BSMEDTECH |
| Almie Joy Batiancila Ilustrisimo | ITE 031 – Data Structure and Algorithms | BSIT, BSMEDTECH |
| Marvy P Segarra | ITE 298 – Information Management | BSIT, BSMEDTECH |

These are loaded by `sql/03_faculty_and_subjects.sql` and stored in the `faculty_subjects` table. The **Faculty Members** page shows each person's course, class schedule and rooms for the program being viewed.

---

## Security notes

- Admin pages and actions need a signed login token. Only the **admin** role can create accounts. **Admin and faculty** can add schedules.
- Passwords are stored only as bcrypt hashes (`password_hash`).
- Row Level Security is turned on for every table. The public Supabase key therefore cannot read the `users` table (password hashes) through Supabase's REST API. The PHP API connects as the database owner and is not affected.
- Database passwords, the Gmail App Password and the token secret are stored as Vercel environment variables, not in the code.
