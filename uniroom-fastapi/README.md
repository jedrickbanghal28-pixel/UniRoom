# UniRoom API (FastAPI)

REST API of UniRoom, written in Python with FastAPI and deployed on Vercel. It uses the same Supabase PostgreSQL database as before.

- **Swagger UI:** `/docs`
- **ReDoc:** `/redoc`
- **OpenAPI JSON:** `/openapi.json`

## Endpoint groups

There is one group (tag) for every table in the Supabase database, plus sign-in and portal groups.

| Tag (table) | Routes |
|---|---|
| users | `GET/POST /api/admin/users`, `POST /api/admin/users/bulk`, `GET/PUT/DELETE /api/admin/users/{id}`, `POST /api/admin/users/{id}/resend` |
| departments | `GET/POST /api/departments`, `GET/PUT/DELETE /api/departments/{id}`, `GET /api/departments/{id}/students` |
| sections | `GET/POST /api/sections`, `GET/PUT/DELETE /api/sections/{id}` |
| rooms | `GET/POST /api/rooms`, `GET/PUT/DELETE /api/rooms/{id}` |
| subjects | `GET/POST /api/subjects`, `GET/PUT/DELETE /api/subjects/{id}` |
| faculty | `GET/POST /api/faculty`, `GET/PUT/DELETE /api/faculty/{id}` |
| faculty_subjects | `GET/POST /api/faculty-subjects`, `DELETE /api/faculty-subjects/{faculty_id}/{subject_id}/{department_id}` |
| schedules | `GET/POST /api/schedules`, `GET /api/schedules/options`, `GET/PUT/DELETE /api/schedules/{id}` (room, faculty and section conflict check) |
| section_rooms | `GET/POST /api/section-rooms`, `GET/PUT/DELETE /api/section-rooms/{section_id}` |
| notifications | `POST/GET /api/notifications`, `GET /api/notifications/all`, `GET /api/notifications/sent`, `GET/PUT/DELETE /api/notifications/{id}`, `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all` |
| notification_reads | `GET/POST /api/notification-reads`, `DELETE /api/notification-reads/{notification_id}/{user_id}` |
| password_tokens | `GET /api/password-tokens`, `DELETE /api/password-tokens/{id}` (admin only, no secrets shown) |
| user-auth | `POST /api/login`, `POST /api/change-password`, `GET/POST /api/set-password` |
| admin-auth | `POST /api/admin/login` |
| dashboard | `GET /api/admin/dashboard`, `GET /api/admin/form-options` |
| student-portal | `GET /api/students/{user_id}/schedule`, `/room`, `/profile` |
| faculty-portal | `GET /api/faculty-portal/{user_id}/dashboard`, `/members`, `/schedule`, `/students` |
| default | `GET /health` |

Protected routes need `Authorization: Bearer <token>`. Get the token from `/api/login` (app) or `/api/admin/login` (website). In Swagger, click **Authorize** and paste it.

The old addresses (`/api/login.php`, `/api/notify_send.php`, ...) still answer the same way. The admin website and the Android app therefore work without any change.

## Deploy on Vercel
1. In the existing **uniroom-api** Vercel project, open **Settings → Build and Deployment → Root Directory** and change it from `uniroom-api` to `uniroom-fastapi`.
2. Keep **Framework Preset** on **Other**. The environment variables stay the same.
3. Redeploy, then open `https://uniroom-api.vercel.app/docs`.

To go back to the PHP version, set the Root Directory to `uniroom-api` again and redeploy.

## Run locally
```bash
cd uniroom-fastapi
pip install -r requirements.txt uvicorn
cp .env.example .env      # fill in the database details
uvicorn app.main:app --reload --port 8000
# open http://localhost:8000/docs
```

## Code layout
| File | What it does |
|---|---|
| `api/index.py` | Vercel entry point |
| `app/main.py` | FastAPI app, routes, request models, old *.php compatibility routes |
| `app/services.py` | Logic of every endpoint (SQL) |
| `app/accounts.py` | Account validation, creation and the account email (Gmail SMTP) |
| `app/core.py` | Settings, database connection (psycopg 3), login tokens, bcrypt passwords via pgcrypto |
