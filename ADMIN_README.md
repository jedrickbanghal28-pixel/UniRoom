# UNIROOM – Admin Website + API (Supabase + Vercel)

> The Android app in this repository still uses the old `php_api/` folder at a local address. The new API for the website is in `uniroom-api/`. The old `fastapi_backend/` reads tables (`department_id`, `faculty_directory`, ...) that are no longer in the Supabase database, so it no longer works.

This folder holds **two separate Vercel projects**:

| Folder | What it is | Vercel project |
|---|---|---|
| `uniroom-api/` | PHP API (all `.php` endpoints) that talks to Supabase Postgres | e.g. `uniroom-api` → `https://uniroom-api.vercel.app` |
| `uniroom-admin/` | Admin / faculty website (HTML, CSS, JS) | e.g. `uniroom-admin` → `https://uniroom-admin.vercel.app` |

`ADMIN_FEATURES.md` explains every new feature: what it does, the fields, the rules and which files implement it. You can use it in your documentation.

---

## 1. Set up Supabase (database)

1. Create a project at <https://supabase.com>. Save the **database password**.
2. Open **SQL Editor → New query** and run, in this order:
   - `uniroom-api/sql/01_base_schema.sql`. Run this **only on an empty database**. If you already moved your tables to Supabase, skip it.
   - `uniroom-api/sql/02_admin_features.sql`. This adds gender, age, department and the password-setup tables, and turns on Row Level Security so nobody can read your tables with the public key. It is safe to run more than once.
   - `uniroom-api/sql/03_faculty_and_subjects.sql`. This loads BSIT and BSMEDTECH, their sections, the Tech Hub rooms, the three faculty (Farah Diva Alvarado, Almie Joy Batiancila Ilustrisimo, Marvy P Segarra), the course each one handles in both programs, and the weekly class schedule. **First change the three faculty emails in section 6** to their real addresses. They log in with the temporary password `UniRoom2026` and must change it.
3. Make an admin account so you can log in. Run this in the SQL Editor, changing the email:
   ```sql
   INSERT INTO users (first_name, last_name, username, email, password_hash, role, must_change_password)
   VALUES ('System', 'Administrator', 'admin', 'admin@yourschool.edu',
           extensions.crypt('ChangeMe123', extensions.gen_salt('bf')), 'admin', TRUE);
   ```
   Log in with `ChangeMe123`. You will be asked to choose a new password right away. PHP's `password_verify` accepts the bcrypt hash that `crypt()` makes. `crypt()` comes from the `pgcrypto` extension, which Supabase installs by default in the `extensions` schema.
4. Get the connection details: **Project Settings → Database → Connection string → Transaction pooler**. You need:
   - host (e.g. `aws-0-ap-southeast-1.pooler.supabase.com`)
   - port `6543`
   - user (e.g. `postgres.abcdefghijkl`)
   - your database password

## 2. Set up Gmail for the account emails

1. Use a Gmail account for the system, for example `uniroom.notifications@gmail.com`.
2. Turn on **2-Step Verification** for it (Google Account → Security).
3. Go to **Google Account → Security → App passwords**, create one named "UNIROOM" and copy the 16-letter password. This goes in `SMTP_PASS`. Your normal Gmail password will **not** work.

Gmail lets a normal account send about 500 emails a day. That is enough for bulk creation of a few hundred users at a time.

## 3. Deploy the API (`uniroom-api`)

1. The code is in the GitHub repository `jedrickbanghal28-pixel/UniRoom`, in the folders `uniroom-api/` and `uniroom-admin/` (next to the Android app).
2. In Vercel, click **Add New → Project** and pick that repository. Set **Root Directory** to `uniroom-api`. Framework preset: **Other**. Leave the build settings empty.
3. Under **Environment Variables**, add these. They are also listed in `.env.example`:

   | Name | Example |
   |---|---|
   | `DB_HOST` | `aws-0-ap-southeast-1.pooler.supabase.com` |
   | `DB_PORT` | `6543` |
   | `DB_NAME` | `postgres` |
   | `DB_USER` | `postgres.abcdefghijkl` |
   | `DB_PASSWORD` | your Supabase database password |
   | `DB_SSLMODE` | `require` |
   | `APP_SECRET` | any long random text (signs the login tokens) |
   | `ADMIN_WEB_URL` | `https://uniroom-admin.vercel.app` (used in the email link) |
   | `ALLOWED_ORIGINS` | `https://uniroom-admin.vercel.app` |
   | `SMTP_HOST` / `SMTP_PORT` / `SMTP_SECURE` | `smtp.gmail.com` / `465` / `ssl` |
   | `SMTP_USER` | `uniroom.notifications@gmail.com` |
   | `SMTP_PASS` | the 16-letter App password |
   | `MAIL_FROM_NAME` | `UNIROOM` |

4. Deploy, then open `https://<your-api>.vercel.app/`. You should see
   `"UNIROOM API is running and connected to the database."` with `"email_configured": true`.

PHP runs on Vercel through the community runtime `vercel-php` (set in `vercel.json`). No Composer packages are needed.

The whole API runs as **one** serverless function (`api/index.php`), because Vercel's free Hobby plan allows at most 12 functions per deployment. `vercel.json` sends every request to `api/index.php`, which runs the matching file in `routes/` (e.g. `/api/login.php` runs `routes/login.php`). The URLs are the same as before. To add a new endpoint, just add a file to `routes/`.

## 4. Deploy the admin website (`uniroom-admin`)

1. Edit `uniroom-admin/config.js` and set the API address:
   ```js
   window.UNIROOM_CONFIG = { API_BASE_URL: "https://uniroom-api.vercel.app/api" };
   ```
2. In Vercel, click **Add New → Project**, pick the repository and set **Root Directory** to `uniroom-admin`. Framework preset: **Other**. No build command.
3. Deploy. If your website URL differs from what you put in `ADMIN_WEB_URL` / `ALLOWED_ORIGINS`, update those two variables in the API project and redeploy the API.

## 5. Mobile app (if it uses this API)

- `login.php` now also returns `must_change_password`. When it is `true`, show a "change password" screen. That screen should call `change_password.php` with `{ login, current_password, new_password }`.
- Students who received a **setup link** set their password on the website page `set-password.html`, then log in to the app normally.
- If the app calls the API from Android, CORS does not apply. Nothing else changes for the app.

## 6. Running everything on your computer

```bash
# API (needs PHP 8 with pdo_pgsql)
cd uniroom-api
cp .env.example .env        # then fill in your Supabase or local Postgres details
php -S localhost:8000 api/index.php   # API is now at http://localhost:8000/api

# Website (any static server), in another terminal
cd uniroom-admin
python -m http.server 5500  # open http://localhost:5500
```

`config.js` already points to `http://localhost:8000/api`. For local use, add `http://localhost:5500` to `ALLOWED_ORIGINS` in `.env`.

## What changed in your existing files

| File | Change |
|---|---|
| all `.php` | moved to `uniroom-api/routes/` (shared helpers in `uniroom-api/lib/`). Database settings now come from environment variables (`lib/config.php`), not `config.php` with a hard-coded password |
| `admin_login.php` | returns a login token and `must_change_password` |
| `login.php` | returns `must_change_password` |
| `admin_dashboard.php`, `admin_departments.php`, `admin_users.php` | admin login required. The users list now shows section, gender, age and status |
| `add_schedule.php` | required-field checks and automatic conflict detection (room / faculty / section) |
| `faculty_*.php` | a faculty member's department can now come from the new `users.department_id` column |
| `generate_password.php`, `reset_faculty.php`, `test.json` | moved to `uniroom-api/dev-tools/` so they are **not** deployed. `reset_faculty.php` would let anyone reset a password |
| `script.js` | API address comes from `config.js`. Login token, first-login password change, new user table columns. `createAccount()` and `addSchedule()` were missing and now exist |
| `index.html` | new Account Creation page (individual + bulk), `*` on required fields, schedule conflict box, password-change window |

New files:
- **API:** `admin_create_user.php`, `admin_bulk_create_users.php`, `admin_form_options.php`, `admin_resend_invite.php`, `set_password.php`, `change_password.php`, `health.php`, `lib/*`, and `api/index.php` (the single entry point)
- **Website:** `accounts.js`, `schedule.js`, `validation.js`, `spreadsheet-reader.js`, `config.js`, `set-password.html`, `templates/bulk_accounts_template.csv`

## Still to do (outside this update)

- The `faculty_*.php`, `assign_room.php` and `room_*.php` endpoints still work as before. They trust the `user_id` sent by the page and do not check the login token, so the mobile app keeps working. When you're ready, add `require_role(['faculty', 'admin']);` near the top of each, the same way `add_schedule.php` does.
- `login.php` (mobile app) does not return a token yet. Only the website uses tokens.
