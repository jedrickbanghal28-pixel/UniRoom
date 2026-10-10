# UniRoom

**Smart University Room Planner and Scheduling System**

UniRoom keeps the class schedules and room assignments of the BS-IT and BS-MedTech sections in one database. The administrator manages accounts, rooms and schedules on a website. Students and faculty use an Android app to see their classes and rooms and to send or receive room-change notifications.

**Group SWU-002 (BSIT-B2)**
- Autentico, Zandrie Jose S.
- Banghal, Jedrick B.
- Contridas, Jake Lawrence
- Pepito, Jesyriel Miles A.

## Live links

| Part | Address |
|---|---|
| API | https://uniroom-api.vercel.app/api (Swagger docs: https://uniroom-api.vercel.app/docs) |
| Admin website | https://uniroom-admin.vercel.app |

## Repository layout

| Folder | What it is |
|---|---|
| `uniroom-fastapi/` | FastAPI (Python) REST API with Swagger docs at `/docs`. This is the current API. |
| `uniroom-api/` | Previous PHP 8 version of the same API (no framework). `api/index.php` is the single entry point, `routes/` holds the endpoints and `lib/` the database, token and mail helpers. |
| `uniroom-api/sql/` | Database scripts for Supabase/PostgreSQL. Run them in order: 01 to 05. |
| `uniroom-api/postman/` | Postman collection for every endpoint. |
| `uniroom-admin/` | Admin and faculty website (HTML, CSS, JavaScript). |
| `uniroom-android/` | Android Studio project of the student and faculty app (Java and XML). |
| `app/`, `php_api/`, `fastapi_backend/` | Earlier prototype versions, kept for reference. |

## How to run

### 1. Database (Supabase)
1. Create a Supabase project.
2. Open **SQL Editor** and run these files from `uniroom-api/sql/` in order:
   - `01_base_schema.sql` (only on an empty database)
   - `02_admin_features.sql`
   - `03_faculty_and_subjects.sql`
   - `04_section_rooms.sql`
   - `05_notifications.sql`
3. Create an admin account (see `ADMIN_README.md`, step 1.3).

### 2. API (Vercel)
1. Import this repository into Vercel and set **Root Directory** to `uniroom-api`.
2. Add the environment variables listed in `uniroom-api/.env.example` (database, `APP_SECRET`, Gmail SMTP).
3. Deploy, then open `/api/health.php`. It should say the API is connected to the database.

To run the API locally instead: `cd uniroom-api && cp .env.example .env` (fill it in), then `php -S localhost:8000 api/index.php`.

### 3. Admin website (Vercel)
1. Set `API_BASE_URL` in `uniroom-admin/config.js`.
2. Import the repository into Vercel again, this time with **Root Directory** set to `uniroom-admin`.

### 4. Android app
1. Open the `uniroom-android` folder in Android Studio and let Gradle sync.
2. The API address is in `app/src/main/java/com/uniroom/app/data/ApiClient.java`.
3. Run the app on a phone or emulator with internet access.

## How accounts work
There is no sign-up. The administrator creates every account. Each user gets an email with their username and a temporary password, and they must change that password on their first sign-in.

More detail on the admin features is in `ADMIN_README.md` and `ADMIN_FEATURES.md`.
