"""
UNIROOM API (FastAPI).

Interactive documentation: /docs (Swagger UI) and /redoc.

The REST routes below are grouped by tag. The old PHP addresses
(/api/login.php, /api/notify_send.php, ...) still work through the
compatibility route at the bottom, so the admin website and the Android
app did not need to change.
"""
from typing import Literal, Optional

from fastapi import Depends, FastAPI, Path, Query, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, RedirectResponse, Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, ConfigDict, Field
from starlette.exceptions import HTTPException as StarletteHTTPException

from . import services as svc
from .core import ApiError, connect, read_token, settings

TAGS = [
    {"name": "user-auth", "description": "Sign in for the Android app (students and faculty) and password changes."},
    {"name": "admin-auth", "description": "Sign in for the admin website."},
    {"name": "dashboard", "description": "Counts and form options for the admin website."},
    {"name": "users", "description": "Table **users** – student, faculty and admin accounts."},
    {"name": "departments", "description": "Table **departments** – the programs (BSIT, BSMT). Each row is one program."},
    {"name": "sections", "description": "Table **sections** – sections of each program (e.g., BSIT-A1)."},
    {"name": "rooms", "description": "Table **rooms** – rooms of the Tech Hub."},
    {"name": "subjects", "description": "Table **subjects** – subjects (e.g., ITE 031 IT Laboratory)."},
    {"name": "faculty", "description": "Table **faculty** – faculty records (linked to a user account)."},
    {"name": "faculty_subjects", "description": "Table **faculty_subjects** – which subjects each faculty member handles per program."},
    {"name": "schedules", "description": "Table **schedules** – classes, with room, faculty and section conflict checking."},
    {"name": "section_rooms", "description": "Table **section_rooms** – home room of each section."},
    {"name": "notifications", "description": "Table **notifications** – room changes and announcements from faculty to students."},
    {"name": "notification_reads", "description": "Table **notification_reads** – which student has read which notification."},
    {"name": "password_tokens", "description": "Table **password_tokens** – one-time password setup links (admin only)."},
    {"name": "student-portal", "description": "What the Android app shows a student."},
    {"name": "faculty-portal", "description": "What the website shows a faculty member."},
    {"name": "default", "description": "Status check."},
]

app = FastAPI(
    title="UniRoom API",
    version="2.0",
    description="REST API of **UniRoom – Smart University Room Planner and Scheduling System** "
                "(Group SWU-002). Click **Authorize** and paste the `token` from a login response to try "
                "the protected endpoints.",
    openapi_tags=TAGS,
)

origins = [o.strip() for o in settings.allowed_origins.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if "*" in origins else origins,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Content-Type", "Authorization"],
    max_age=86400,
)

bearer = HTTPBearer(auto_error=False, description="Token from /api/login or /api/admin/login")


def current_token(creds: Optional[HTTPAuthorizationCredentials] = Depends(bearer)):
    return read_token(f"Bearer {creds.credentials}") if creds else None


# ------------------------------------------------------------------ error handling
@app.exception_handler(ApiError)
async def api_error_handler(request: Request, exc: ApiError):
    return JSONResponse(status_code=exc.status, content=jsonable_encoder(exc.payload))


@app.exception_handler(RequestValidationError)
async def validation_handler(request: Request, exc: RequestValidationError):
    errors = [{"field": str(e["loc"][-1]) if e.get("loc") else "", "message": e.get("msg", "Invalid value.")}
              for e in exc.errors()]
    return JSONResponse(status_code=422, content={"success": False, "message": svc.REQUIRED_MSG, "errors": errors})


@app.exception_handler(StarletteHTTPException)
async def http_error_handler(request: Request, exc: StarletteHTTPException):
    message = "Unknown API endpoint." if exc.status_code == 404 else str(exc.detail)
    return JSONResponse(status_code=exc.status_code, content={"success": False, "message": message})


@app.exception_handler(Exception)
async def unexpected_handler(request: Request, exc: Exception):
    return JSONResponse(status_code=500, content={"success": False, "message": "Server error."})


def run(fn, *args, data=None, query=None, token=None, status=200, **kw):
    """status: 201 for routes that create a record, 200 for everything else."""
    with connect() as conn:
        result = fn(conn, data or {}, query or {}, token, *args, **kw)
    return JSONResponse(status_code=status, content=jsonable_encoder(result))


def body_of(model):
    return model.model_dump(exclude_none=True) if model is not None else {}


# ------------------------------------------------------------------ request models
class Model(BaseModel):
    model_config = ConfigDict(extra="allow")


class LoginIn(Model):
    username: str = Field(..., description="Username or email", examples=["carlo.reyes"])
    password: str = Field(..., examples=["NewPass2026"])


class AdminLoginIn(Model):
    email: str = Field(..., examples=["admin@example.edu"])
    password: str


class ChangePasswordIn(Model):
    login: str = Field(..., description="Username or email")
    current_password: str
    new_password: str = Field(..., description="At least 8 characters with letters and numbers")


class SetPasswordIn(Model):
    token: str
    new_password: str


class UserIn(Model):
    first_name: str = Field(..., examples=["Carlo"])
    last_name: str = Field(..., examples=["Reyes"])
    middle_initial: Optional[str] = Field(None, max_length=3, examples=["D"])
    email: str = Field(..., examples=["carlo.reyes@example.edu"])
    gender: str = Field(..., examples=["Male"])
    age: int = Field(..., ge=15, le=100, examples=[19])
    role: Literal["student", "faculty"] = "student"
    department: Optional[str] = Field(None, description="Code, name or id", examples=["BSIT"])
    department_id: Optional[int] = None
    section: Optional[str] = Field(None, description="Required for students", examples=["BSIT-A1"])
    section_id: Optional[int] = None
    student_id: Optional[str] = Field(None, examples=["2026-0101"])
    employee_id: Optional[str] = None
    contact_number: Optional[str] = None
    password_mode: Literal["temporary", "link"] = "temporary"


class UserUpdateIn(Model):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    middle_initial: Optional[str] = Field(None, max_length=3)
    email: Optional[str] = None
    gender: Optional[str] = None
    age: Optional[int] = Field(None, ge=15, le=100)
    role: Optional[Literal["student", "faculty"]] = None
    department_id: Optional[int] = None
    section_id: Optional[int] = None
    student_id: Optional[str] = None
    employee_id: Optional[str] = None
    contact_number: Optional[str] = None


class BulkIn(Model):
    rows: list[dict] = Field(..., description="Up to 50 rows with the same fields as a single account")
    password_mode: Literal["temporary", "link"] = "temporary"
    validate_only: bool = False


class ResendIn(Model):
    password_mode: Literal["temporary", "link"] = "temporary"


class DepartmentIn(Model):
    code: str = Field(..., examples=["BSIT"])
    name: Optional[str] = Field(None, examples=["Information Technology"])


class SectionIn(Model):
    name: str = Field(..., examples=["BSIT-A2"])
    department_id: int


class RoomIn(Model):
    room_name: str = Field(..., examples=["TH 312"])
    building: str = Field(..., examples=["Tech Hub"])
    floor: int = Field(..., ge=0, examples=[3])
    capacity: int = Field(..., gt=0, examples=[40])
    status: Literal["Available", "Occupied", "Reserved", "Maintenance"] = "Available"


class SubjectIn(Model):
    code: str = Field(..., examples=["ITE 031"])
    name: str = Field(..., examples=["IT Laboratory"])


class ScheduleIn(Model):
    section_id: int
    subject_id: int
    faculty_id: int
    room_id: int
    day_of_week: Literal["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    start_time: str = Field(..., examples=["08:00"])
    end_time: str = Field(..., examples=["10:00"])


class AssignRoomIn(Model):
    room_id: int


class SectionRoomIn(Model):
    section_id: int
    room_id: int


class FacultyIn(Model):
    first_name: str = Field(..., examples=["Farah Diva"])
    last_name: str = Field(..., examples=["Alvarado"])
    email: Optional[str] = Field(None, examples=["f.alvarado@university.edu"])
    user_id: Optional[int] = Field(None, description="Faculty user account (optional)")


class FacultySubjectIn(Model):
    faculty_id: int
    subject_id: int
    department_id: int


class NotificationUpdateIn(Model):
    title: str = Field(..., max_length=150)
    body: str = Field(..., max_length=500)
    kind: Optional[Literal["transfer", "announcement"]] = None


class NotificationIn(Model):
    program: str = Field(..., examples=["BSIT"])
    section: str = Field("", description="Empty = all sections of the program", examples=["BSIT-A1"])
    kind: Literal["transfer", "announcement"] = "announcement"
    title: str = Field(..., max_length=150, examples=["Room change: TH 309 → TH 310"])
    body: str = Field(..., max_length=500, examples=["Your class moves to TH 310 starting today. Same time."])


Token = Depends(current_token)


# ------------------------------------------------------------------ default
@app.get("/", include_in_schema=False)
def root():
    return RedirectResponse("/docs")


@app.get("/health", tags=["default"], summary="Health")
@app.get("/api/health", tags=["default"], summary="Health", include_in_schema=False)
def health():
    return run(svc.health)


# ------------------------------------------------------------------ user-auth
@app.post("/api/login", tags=["user-auth"], summary="Login (Android app)")
def login(body: LoginIn):
    return run(svc.login, data=body_of(body))


@app.post("/api/change-password", tags=["user-auth"], summary="Change temporary password")
def change_password(body: ChangePasswordIn):
    return run(svc.change_password, data=body_of(body))


@app.get("/api/set-password", tags=["user-auth"], summary="Check password setup link")
def check_setup_link(token: str = Query(...)):
    return run(svc.set_password, query={"token": token}, method="GET")


@app.post("/api/set-password", tags=["user-auth"], summary="Set password from link")
def set_password(body: SetPasswordIn):
    return run(svc.set_password, data=body_of(body), method="POST")


# ------------------------------------------------------------------ admin-auth
@app.post("/api/admin/login", tags=["admin-auth"], summary="Admin Login")
def admin_login(body: AdminLoginIn):
    return run(svc.admin_login, data=body_of(body))


# ------------------------------------------------------------------ admin-management
@app.get("/api/admin/users", tags=["users"], summary="List Users")
def list_users(role: Optional[str] = None, department: Optional[str] = Query(None, examples=["BSIT"]),
               section: Optional[str] = None, search: Optional[str] = None,
               sort: Literal["course", "surname", "email"] = "course", token=Token):
    q = {"role": role, "department": department, "section": section, "search": search, "sort": sort}
    return run(svc.admin_users, query={k: v for k, v in q.items() if v}, token=token)


@app.post("/api/admin/users", tags=["users"], summary="Create User", status_code=201)
def create_user(body: UserIn, token=Token):
    return run(svc.admin_create_user, data=body_of(body), token=token, status=201)


@app.post("/api/admin/users/bulk", tags=["users"], summary="Bulk Create Users")
def bulk_create_users(body: BulkIn, token=Token):
    return run(svc.admin_bulk_create_users, data=body_of(body), token=token)


@app.get("/api/admin/users/{user_id}", tags=["users"], summary="Get User")
def get_user(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.admin_get_user, user_id, token=token)


@app.put("/api/admin/users/{user_id}", tags=["users"], summary="Update User")
def update_user(body: UserUpdateIn, user_id: int = Path(..., ge=1), token=Token):
    return run(svc.admin_update_user, user_id, data=body_of(body), token=token)


@app.delete("/api/admin/users/{user_id}", tags=["users"], summary="Delete User")
def delete_user(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.admin_delete_user, user_id, token=token)


@app.post("/api/admin/users/{user_id}/resend", tags=["users"], summary="Resend Account Email")
def resend_invite(user_id: int = Path(..., ge=1), body: Optional[ResendIn] = None, token=Token):
    return run(svc.admin_resend_invite, data={**body_of(body or ResendIn()), "user_id": user_id}, token=token)


# ------------------------------------------------------------------ dashboard
@app.get("/api/admin/dashboard", tags=["dashboard"], summary="Dashboard")
def dashboard(token=Token):
    return run(svc.admin_dashboard, token=token)


@app.get("/api/admin/form-options", tags=["dashboard"], summary="Account Form Options")
def form_options(token=Token):
    return run(svc.admin_form_options, token=token)


# ------------------------------------------------------------------ departments
@app.get("/api/departments", tags=["departments"], summary="List Departments")
def list_departments(token=Token):
    return run(svc.departments_list, token=token)


@app.post("/api/departments", tags=["departments"], summary="Create Department", status_code=201)
def create_department(body: DepartmentIn, token=Token):
    return run(svc.department_save, data=body_of(body), token=token, status=201)


@app.get("/api/departments/{department_id}", tags=["departments"], summary="Get Department")
def get_department(department_id: int = Path(..., ge=1), token=Token):
    return run(svc.department_get, department_id, token=token)


@app.put("/api/departments/{department_id}", tags=["departments"], summary="Update Department")
def update_department(body: DepartmentIn, department_id: int = Path(..., ge=1), token=Token):
    return run(svc.department_save, department_id, data=body_of(body), token=token)


@app.delete("/api/departments/{department_id}", tags=["departments"], summary="Delete Department")
def delete_department(department_id: int = Path(..., ge=1), token=Token):
    return run(svc.department_delete, department_id, token=token)


@app.get("/api/departments/{department_id}/students", tags=["departments"], summary="Department Students")
def department_students(department_id: int = Path(..., ge=1), token=Token):
    return run(svc.admin_department_students, department_id, token=token)


# ------------------------------------------------------------------ sections
@app.get("/api/sections", tags=["sections"], summary="List Sections")
def list_sections(department_id: Optional[int] = None, token=Token):
    return run(svc.sections_list, query={"department_id": department_id} if department_id else {}, token=token)


@app.post("/api/sections", tags=["sections"], summary="Create Section", status_code=201)
def create_section(body: SectionIn, token=Token):
    return run(svc.section_save, data=body_of(body), token=token, status=201)


@app.get("/api/sections/{section_id}", tags=["sections"], summary="Get Section")
def get_section(section_id: int = Path(..., ge=1), token=Token):
    return run(svc.section_get, section_id, token=token)


@app.put("/api/sections/{section_id}", tags=["sections"], summary="Update Section")
def update_section(body: SectionIn, section_id: int = Path(..., ge=1), token=Token):
    return run(svc.section_save, section_id, data=body_of(body), token=token)


@app.delete("/api/sections/{section_id}", tags=["sections"], summary="Delete Section")
def delete_section(section_id: int = Path(..., ge=1), token=Token):
    return run(svc.section_delete, section_id, token=token)


# ------------------------------------------------------------------ rooms
@app.get("/api/rooms", tags=["rooms"], summary="List Rooms")
def list_rooms(search: Optional[str] = Query(None, examples=["TH 30"]), token=Token):
    return run(svc.rooms_list, query={"search": search} if search else {}, token=token)


@app.get("/api/rooms/today", tags=["rooms"], summary="Rooms with Today's Classes (live availability)")
def rooms_today(day: Optional[str] = Query(None, examples=["Monday"], description="Default: today"), token=Token):
    return run(svc.rooms_today, query={"day": day} if day else {}, token=token)


@app.post("/api/rooms", tags=["rooms"], summary="Create Room", status_code=201)
def create_room(body: RoomIn, token=Token):
    return run(svc.room_save, data=body_of(body), token=token, status=201)


@app.get("/api/rooms/{room_id}", tags=["rooms"], summary="Get Room")
def get_room(room_id: int = Path(..., ge=1), token=Token):
    return run(svc.room_get, room_id, token=token)


@app.put("/api/rooms/{room_id}", tags=["rooms"], summary="Update Room")
def update_room(body: RoomIn, room_id: int = Path(..., ge=1), token=Token):
    return run(svc.room_save, room_id, data=body_of(body), token=token)


@app.delete("/api/rooms/{room_id}", tags=["rooms"], summary="Delete Room")
def delete_room(room_id: int = Path(..., ge=1), token=Token):
    return run(svc.room_delete, room_id, token=token)


# ------------------------------------------------------------------ subjects
@app.get("/api/subjects", tags=["subjects"], summary="List Subjects")
def list_subjects(token=Token):
    return run(svc.subjects_list, token=token)


@app.post("/api/subjects", tags=["subjects"], summary="Create Subject", status_code=201)
def create_subject(body: SubjectIn, token=Token):
    return run(svc.subject_save, data=body_of(body), token=token, status=201)


@app.get("/api/subjects/{subject_id}", tags=["subjects"], summary="Get Subject")
def get_subject(subject_id: int = Path(..., ge=1), token=Token):
    return run(svc.subject_get, subject_id, token=token)


@app.put("/api/subjects/{subject_id}", tags=["subjects"], summary="Update Subject")
def update_subject(body: SubjectIn, subject_id: int = Path(..., ge=1), token=Token):
    return run(svc.subject_save, subject_id, data=body_of(body), token=token)


@app.delete("/api/subjects/{subject_id}", tags=["subjects"], summary="Delete Subject")
def delete_subject(subject_id: int = Path(..., ge=1), token=Token):
    return run(svc.subject_delete, subject_id, token=token)


# ------------------------------------------------------------------ faculty
@app.get("/api/faculty", tags=["faculty"], summary="List Faculty")
def list_faculty(token=Token):
    return run(svc.faculty_list, token=token)


@app.post("/api/faculty", tags=["faculty"], summary="Create Faculty", status_code=201)
def create_faculty(body: FacultyIn, token=Token):
    return run(svc.faculty_save, data=body_of(body), token=token, status=201)


@app.get("/api/faculty/{faculty_id}", tags=["faculty"], summary="Get Faculty")
def get_faculty(faculty_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_get, faculty_id, token=token)


@app.put("/api/faculty/{faculty_id}", tags=["faculty"], summary="Update Faculty")
def update_faculty(body: FacultyIn, faculty_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_save, faculty_id, data=body_of(body), token=token)


@app.delete("/api/faculty/{faculty_id}", tags=["faculty"], summary="Delete Faculty")
def delete_faculty(faculty_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_delete, faculty_id, token=token)


# ------------------------------------------------------------------ faculty_subjects
@app.get("/api/faculty-subjects", tags=["faculty_subjects"], summary="List Faculty Subjects")
def list_faculty_subjects(faculty_id: Optional[int] = None, subject_id: Optional[int] = None,
                          department_id: Optional[int] = None, token=Token):
    q = {"faculty_id": faculty_id, "subject_id": subject_id, "department_id": department_id}
    return run(svc.faculty_subjects_list, query={k: v for k, v in q.items() if v}, token=token)


@app.post("/api/faculty-subjects", tags=["faculty_subjects"], summary="Assign Subject to Faculty", status_code=201)
def create_faculty_subject(body: FacultySubjectIn, token=Token):
    return run(svc.faculty_subject_create, data=body_of(body), token=token, status=201)


@app.delete("/api/faculty-subjects/{faculty_id}/{subject_id}/{department_id}", tags=["faculty_subjects"],
            summary="Remove Subject from Faculty")
def delete_faculty_subject(faculty_id: int = Path(..., ge=1), subject_id: int = Path(..., ge=1),
                           department_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_subject_delete, faculty_id, subject_id, department_id, token=token)


# ------------------------------------------------------------------ schedules
@app.get("/api/schedules", tags=["schedules"], summary="List Schedules")
def list_schedules(section_id: Optional[int] = None, room_id: Optional[int] = None, faculty_id: Optional[int] = None,
                   subject_id: Optional[int] = None, department_id: Optional[int] = None,
                   day_of_week: Optional[str] = Query(None, examples=["Monday"]), token=Token):
    q = {"section_id": section_id, "room_id": room_id, "faculty_id": faculty_id, "subject_id": subject_id,
         "department_id": department_id, "day_of_week": day_of_week}
    return run(svc.schedules_list, query={k: v for k, v in q.items() if v}, token=token)


@app.get("/api/schedules/options", tags=["schedules"], summary="Schedule Form Options")
def schedule_options(token=Token):
    return run(svc.schedule_options, token=token)


@app.post("/api/schedules", tags=["schedules"], summary="Create Schedule (checks conflicts)", status_code=201)
def create_schedule(body: ScheduleIn, token=Token):
    return run(svc.schedule_save, data=body_of(body), token=token, status=201)


@app.get("/api/schedules/{schedule_id}", tags=["schedules"], summary="Get Schedule")
def get_schedule(schedule_id: int = Path(..., ge=1), token=Token):
    return run(svc.schedule_get, schedule_id, token=token)


@app.put("/api/schedules/{schedule_id}", tags=["schedules"], summary="Update Schedule (checks conflicts)")
def update_schedule(body: ScheduleIn, schedule_id: int = Path(..., ge=1), token=Token):
    return run(svc.schedule_save, schedule_id, data=body_of(body), token=token)


@app.delete("/api/schedules/{schedule_id}", tags=["schedules"], summary="Delete Schedule")
def delete_schedule(schedule_id: int = Path(..., ge=1), token=Token):
    return run(svc.schedule_delete, schedule_id, token=token)


# ------------------------------------------------------------------ section-rooms
@app.get("/api/section-rooms", tags=["section_rooms"], summary="List Home Rooms")
def list_section_rooms(token=Token):
    return run(svc.section_rooms_list, token=token)


@app.post("/api/section-rooms", tags=["section_rooms"], summary="Create Home Room", status_code=201)
def create_section_room(body: SectionRoomIn, token=Token):
    data = body_of(body)
    return run(svc.assign_room, int(data["section_id"]), data=data, token=token, status=201)


@app.get("/api/section-rooms/{section_id}", tags=["section_rooms"], summary="Get Home Room")
def get_section_room(section_id: int = Path(..., ge=1), token=Token):
    return run(svc.section_room_get, section_id, token=token)


@app.put("/api/section-rooms/{section_id}", tags=["section_rooms"], summary="Assign Home Room")
def assign_home_room(body: AssignRoomIn, section_id: int = Path(..., ge=1), token=Token):
    return run(svc.assign_room, section_id, data=body_of(body), token=token)


@app.delete("/api/section-rooms/{section_id}", tags=["section_rooms"], summary="Remove Home Room")
def remove_home_room(section_id: int = Path(..., ge=1), token=Token):
    return run(svc.unassign_room, section_id, token=token)


# ------------------------------------------------------------------ signed-in user (Android app)
@app.get("/api/me", tags=["user-auth"], summary="My Account (student or faculty)")
def my_account(token=Token):
    return run(svc.me, token=token)


@app.get("/api/me/schedule", tags=["user-auth"], summary="My Weekly Classes (student or faculty)")
def my_classes(token=Token):
    return run(svc.my_schedule, token=token)


# ------------------------------------------------------------------ student-portal
@app.get("/api/students/{user_id}/schedule", tags=["student-portal"], summary="Student Schedule")
def student_schedule(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.student_schedule, user_id, token=token)


@app.get("/api/students/{user_id}/room", tags=["student-portal"], summary="Student Home Room")
def student_room(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.student_room, user_id, token=token)


@app.get("/api/students/{user_id}/profile", tags=["student-portal"], summary="Student Profile")
def student_profile(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.profile, user_id, token=token)


# ------------------------------------------------------------------ faculty-portal
@app.get("/api/faculty-portal/{user_id}/dashboard", tags=["faculty-portal"], summary="Faculty Dashboard")
def faculty_dashboard(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_dashboard, user_id, token=token)


@app.get("/api/faculty-portal/{user_id}/members", tags=["faculty-portal"], summary="Faculty Members of Department")
def faculty_members(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_members, user_id, token=token)


@app.get("/api/faculty-portal/{user_id}/schedule", tags=["faculty-portal"], summary="Department Schedule")
def faculty_schedule(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_schedule, user_id, token=token)


@app.get("/api/faculty-portal/{user_id}/students", tags=["faculty-portal"], summary="Department Students")
def faculty_students(user_id: int = Path(..., ge=1), token=Token):
    return run(svc.faculty_students, user_id, token=token)


# ------------------------------------------------------------------ notifications
@app.post("/api/notifications", tags=["notifications"], summary="Send Notification (faculty)", status_code=201)
def send_notification(body: NotificationIn, token=Token):
    return run(svc.notify_send, data=body_of(body), token=token, status=201)


@app.get("/api/notifications", tags=["notifications"], summary="My Notifications (student)")
def my_notifications(token=Token):
    return run(svc.notifications, token=token)


@app.get("/api/notifications/sent", tags=["notifications"], summary="Sent Notifications (faculty)")
def sent_notifications(token=Token):
    return run(svc.notifications_sent, token=token)


@app.get("/api/notifications/all", tags=["notifications"], summary="List All Notifications (admin/faculty)")
def all_notifications(department_id: Optional[int] = None, section_id: Optional[int] = None, token=Token):
    q = {"department_id": department_id, "section_id": section_id}
    return run(svc.notifications_all, query={k: v for k, v in q.items() if v}, token=token)


@app.get("/api/notifications/{notification_id}", tags=["notifications"], summary="Get Notification")
def get_notification(notification_id: int = Path(..., ge=1), token=Token):
    return run(svc.notification_get, notification_id, token=token)


@app.put("/api/notifications/{notification_id}", tags=["notifications"], summary="Update Notification (sender)")
def update_notification(body: NotificationUpdateIn, notification_id: int = Path(..., ge=1), token=Token):
    return run(svc.notification_update, notification_id, data=body_of(body), token=token)


@app.post("/api/notifications/read-all", tags=["notifications"], summary="Mark All as Read")
def read_all(token=Token):
    return run(svc.notifications_read, data={"all": True}, token=token)


@app.post("/api/notifications/{notification_id}/read", tags=["notifications"], summary="Mark as Read")
def read_one(notification_id: int = Path(..., ge=1), token=Token):
    return run(svc.notifications_read, notification_id, token=token)


@app.delete("/api/notifications/{notification_id}", tags=["notifications"], summary="Delete Notification")
def delete_notification(notification_id: int = Path(..., ge=1), token=Token):
    return run(svc.notification_delete, notification_id, token=token)


# ------------------------------------------------------------------ notification_reads
@app.get("/api/notification-reads", tags=["notification_reads"], summary="List Read Records")
def list_notification_reads(notification_id: Optional[int] = None, user_id: Optional[int] = None, token=Token):
    q = {"notification_id": notification_id, "user_id": user_id}
    return run(svc.notification_reads_list, query={k: v for k, v in q.items() if v}, token=token)


@app.post("/api/notification-reads", tags=["notification_reads"], summary="Create Read Record (mark as read)")
def create_notification_read(body: dict, token=Token):
    return run(svc.notifications_read, data={"id": body.get("notification_id")}, token=token)


@app.delete("/api/notification-reads/{notification_id}/{user_id}", tags=["notification_reads"],
            summary="Delete Read Record (mark as unread)")
def delete_notification_read(notification_id: int = Path(..., ge=1), user_id: int = Path(..., ge=1), token=Token):
    return run(svc.notification_read_delete, notification_id, user_id, token=token)


# ------------------------------------------------------------------ password_tokens
@app.get("/api/password-tokens", tags=["password_tokens"], summary="List Setup Links (admin)")
def list_password_tokens(token=Token):
    return run(svc.password_tokens_list, token=token)


@app.delete("/api/password-tokens/{token_id}", tags=["password_tokens"], summary="Delete Setup Link (admin)")
def delete_password_token(token_id: int = Path(..., ge=1), token=Token):
    return run(svc.password_token_delete, token_id, token=token)


# ------------------------------------------------------------------ old PHP addresses
def _legacy_room(conn, data, query, token, method, action):
    if action == "delete":
        return svc.room_delete(conn, data, query, token, svc._i(data, "id"))
    if action == "update":
        if svc._i(data, "id") <= 0:
            raise ApiError(400, "Id is required.")
        out = svc.room_save(conn, data, query, token, svc._i(data, "id"))
        out.pop("room", None)
        return out
    return svc.room_save(conn, data, query, token)


def _need_id(data):
    i = svc._i(data, "id")
    if i <= 0:
        raise ApiError(400, "Id is required.")
    return i


LEGACY = {
    "health": lambda c, d, q, t, m: svc.health(c, d, q, t),
    "test_connection": lambda c, d, q, t, m: svc.test_connection(c, d, q, t),
    "login": lambda c, d, q, t, m: svc.login(c, d, q, t),
    "admin_login": lambda c, d, q, t, m: svc.admin_login(c, d, q, t),
    "change_password": lambda c, d, q, t, m: svc.change_password(c, d, q, t),
    "set_password": lambda c, d, q, t, m: svc.set_password(c, d, q, t, method=m),
    "admin_dashboard": lambda c, d, q, t, m: svc.admin_dashboard(c, d, q, t),
    "admin_departments": lambda c, d, q, t, m: svc.departments_list(c, d, q, t),
    "admin_department_students": lambda c, d, q, t, m: svc.admin_department_students(c, d, q, t),
    "admin_users": lambda c, d, q, t, m: svc.admin_users(c, d, q, t),
    "admin_form_options": lambda c, d, q, t, m: svc.admin_form_options(c, d, q, t),
    "admin_create_user": lambda c, d, q, t, m: svc.admin_create_user(c, d, q, t),
    "admin_bulk_create_users": lambda c, d, q, t, m: svc.admin_bulk_create_users(c, d, q, t),
    "admin_resend_invite": lambda c, d, q, t, m: svc.admin_resend_invite(c, d, q, t),
    "schedule_options": lambda c, d, q, t, m: svc.schedule_options(c, d, q, t),
    "add_schedule": lambda c, d, q, t, m: svc.schedule_save(c, d, q, t),
    "assign_room": lambda c, d, q, t, m: svc.assign_room(c, d, q, t),
    "faculty_dashboard": lambda c, d, q, t, m: svc.faculty_dashboard(c, d, q, t),
    "faculty_members": lambda c, d, q, t, m: svc.faculty_members(c, d, q, t),
    "faculty_schedule": lambda c, d, q, t, m: svc.faculty_schedule(c, d, q, t),
    "faculty_students": lambda c, d, q, t, m: svc.faculty_students(c, d, q, t),
    "schedule": lambda c, d, q, t, m: svc.student_schedule(c, d, q, t),
    "student_room": lambda c, d, q, t, m: svc.student_room(c, d, q, t),
    "profile": lambda c, d, q, t, m: svc.profile(c, d, q, t),
    "rooms": lambda c, d, q, t, m: svc.rooms_list(c, d, q, t),
    "room_create": lambda c, d, q, t, m: _legacy_room(c, d, q, t, m, "create"),
    "room_update": lambda c, d, q, t, m: _legacy_room(c, d, q, t, m, "update"),
    "room_delete": lambda c, d, q, t, m: _legacy_room(c, d, q, t, m, "delete"),
    "notify_send": lambda c, d, q, t, m: svc.notify_send(c, d, q, t),
    "notifications": lambda c, d, q, t, m: svc.notifications(c, d, q, t),
    "notifications_read": lambda c, d, q, t, m: svc.notifications_read(c, d, q, t),
    # Android app (signed-in user)
    "me": lambda c, d, q, t, m: svc.me(c, d, q, t),
    "my_schedule": lambda c, d, q, t, m: svc.my_schedule(c, d, q, t),
    "rooms_today": lambda c, d, q, t, m: svc.rooms_today(c, d, q, t),
    # website: edit / delete a class
    "update_schedule": lambda c, d, q, t, m: svc.schedule_save(c, d, q, t, _need_id(d)),
    "delete_schedule": lambda c, d, q, t, m: svc.schedule_delete(c, d, q, t, _need_id(d)),
}


@app.api_route("/api", methods=["GET"], include_in_schema=False)
@app.api_route("/api/", methods=["GET"], include_in_schema=False)
def api_root():
    return run(svc.health)


@app.api_route("/api/{name}.php", methods=["GET", "POST"], include_in_schema=False)
async def legacy(name: str, request: Request):
    handler = LEGACY.get(name)
    if handler is None:
        return JSONResponse(status_code=404, content={"success": False, "message": f"Unknown API endpoint: {name}"})
    data = {}
    if request.method == "POST":
        try:
            parsed = await request.json()
            data = parsed if isinstance(parsed, dict) else {}
        except Exception:
            data = {}
    token = read_token(request.headers.get("authorization"))
    query = dict(request.query_params)
    with connect() as conn:
        result = handler(conn, data, query, token, request.method)
    return JSONResponse(content=jsonable_encoder(result))
