# UniRoom – Smart University Room Planner and Scheduling System

UniRoom keeps the class schedules and room assignments of the BSIT and BSMT (BS Medical Technology) programs of Southwestern University PHINMA in one place. It checks that no room, faculty member or section is booked twice at the same time, shows students and faculty their classes and rooms on their phones, and lets faculty send room changes and announcements to a section.

Group SWU-002:

| Member | Main work |
|---|---|
| Banghal, Jedrick B. | Project manager, admin website, database, documentation |
| Pepito, Jesyriel Miles A. | UI/UX design (Figma), website and app screens |
| Contridas, Jake Lawrence M. | API and database, SQL scripts, testing |
| Autentico, Zandrie Jose S. | API and database, interface design, testing |

## Parts of the system

| Folder | What it is | Used now? |
|---|---|---|
| `uniroom-fastapi/` | REST API (Python, FastAPI) deployed on Vercel at https://uniroom-api.vercel.app (docs at `/docs`) | Yes |
| `uniroom-admin/` | Admin and faculty website (HTML, CSS, JavaScript) deployed on Vercel | Yes |
| `uniroom-android/` | Android app for students and faculty (Java, XML) | Yes |
| `uniroom-api/sql/` | Database scripts `01` to `10`, run in the Supabase SQL Editor | Yes |
| `uniroom-api/postman/` | Postman collection for testing the API | Yes |
| `uniroom-api/` (other files) | First API, written in PHP. Replaced by `uniroom-fastapi/` | No |
| `php_api/`, `database/` | Early PHP API and local database dump from the first prototype | No |
| `app/`, `build/`, `fastapi_backend/`, root Gradle files | Early Android and API experiments before the folders above | No |

The old folders are kept only as a record of earlier versions. Nothing in them is deployed.

## Setup

### 1. Database (Supabase)
1. Create a Supabase project.
2. Open **SQL Editor** and run the files in `uniroom-api/sql/` in order (`01` to `10`). Files `06` to `09` are only for demos (`09` clears all data except the admin), so skip them on a new setup.
3. Create the first admin account (choose your own email and password):
   ```sql
   INSERT INTO users (first_name, last_name, username, email, password_hash, role, must_change_password)
   VALUES ('System', 'Administrator', 'admin', 'you@example.com',
           extensions.crypt('your-own-password', extensions.gen_salt('bf')), 'admin', FALSE);
   ```

### 2. API (`uniroom-fastapi`)
```bash
cd uniroom-fastapi
pip install -r requirements.txt uvicorn
cp .env.example .env      # fill in the database, APP_SECRET and Gmail details
uvicorn app.main:app --reload --port 8000
```
Open http://localhost:8000/docs. On Vercel, set the Root Directory to `uniroom-fastapi` and add the same environment variables.

### 3. Website (`uniroom-admin`)
Set the API address in `uniroom-admin/config.js`, then open `index.html` with a local server (for example the VS Code Live Server on port 5500) or deploy the folder on Vercel.

### 4. Android app (`uniroom-android`)
Open the project in Android Studio (Embedded JDK), let Gradle sync, and press **Run**. Android 8.0 or newer. The API address is in `data/ApiClient.java`.

## Accounts

There is no self-registration. The admin creates student and faculty accounts on the website, one at a time or from a `.csv` / `.xlsx` file. Each user gets an email with either a username and temporary password or a one-time password setup link. Passwords are never written in this repository.
