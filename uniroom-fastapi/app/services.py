"""
Business logic of every endpoint. Each function takes (conn, data, query, token)
and returns the JSON answer (a dict) or raises ApiError. The REST routes in
main.py and the old *.php addresses both call these, so they answer the same.
"""
import hashlib
import re
from datetime import datetime, timezone, time as dtime

import psycopg

from . import accounts as acc
from .core import (ApiError, all_rows, hash_password, make_token, one, password_problem,
                   random_password, require_role, scalar, verify_password)

REQUIRED_MSG = "Please complete the required fields marked with *."
DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
CLASS_DAYS = DAYS[:6]          # classes run Monday to Saturday (same CHECK as the database)
ROOM_STATUSES = ["Available", "Occupied", "Reserved", "Maintenance"]
TIME_RE = re.compile(r"^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$")


def _s(data, key):
    v = data.get(key)
    return "" if v is None else str(v).strip()


def _i(data, key):
    try:
        return int(str(data.get(key, 0)).strip() or 0)
    except ValueError:
        return 0


ANY_ROLE = ["admin", "faculty", "student"]


def require_login(token):
    """Any signed-in user (admin, faculty or student)."""
    return require_role(token, ANY_ROLE)


def _own_id(token, user_id, roles):
    """
    The user id an endpoint may read. Students and faculty may only read their
    own records; the admin may read anyone's. With no user_id given, the
    signed-in user's own id is used.
    """
    tok = require_role(token, roles)
    me = int(tok["uid"])
    if not user_id:
        return me
    if tok["role"] != "admin" and int(user_id) != me:
        raise ApiError(403, "You can only view your own records.")
    return int(user_id)


def _db_error(e, conflict_msg, default="Database error."):
    if isinstance(e, psycopg.errors.UniqueViolation):
        raise ApiError(409, conflict_msg)
    if isinstance(e, psycopg.errors.ForeignKeyViolation):
        raise ApiError(409, "This record is still used by other records and cannot be removed.")
    raise ApiError(500, default)


# =================================================================== health
def health(conn, data, query, token):
    one(conn, "SELECT 1")
    return {"success": True, "message": "UNIROOM API is running and connected to the database.",
            "email_configured": acc.Mailer.is_configured()}


def test_connection(conn, data, query, token):
    one(conn, "SELECT 1")
    return {"success": True, "message": "PostgreSQL connection successful!"}


# =================================================================== auth
def login(conn, data, query, token):
    """Android app sign-in (students and faculty). Username or email."""
    login_ = _s(data, "username") or _s(data, "login") or _s(data, "email")
    password = str(data.get("password") or "")
    if not login_ or not password:
        raise ApiError(400, "Please enter username/email and password.")
    user = one(conn, """
        SELECT u.id, u.username, u.email, u.first_name, u.last_name, u.student_id, u.contact_number,
               u.role, u.section_id, s.name AS section_name, d.code AS program,
               u.must_change_password, u.password_hash
        FROM users u
        LEFT JOIN sections s ON s.id = u.section_id
        LEFT JOIN departments d ON d.id = COALESCE(u.department_id, s.department_id)
        WHERE lower(u.username) = lower(%(l)s) OR lower(u.email) = lower(%(l)s)
        LIMIT 1""", {"l": login_})
    if not user or not verify_password(conn, password, user["password_hash"]):
        raise ApiError(401, "Invalid username/email or password.")
    user.pop("password_hash")
    user["must_change_password"] = bool(user["must_change_password"])
    user["section_id"] = user["section_id"] or 0
    out = {"success": True,
           "message": "Please change your temporary password to continue." if user["must_change_password"] else "Login successful.",
           "user": user}
    if not user["must_change_password"]:          # token only after the temporary password is replaced
        out["token"] = make_token(user, 24 * 7)
    return out


def admin_login(conn, data, query, token):
    email = _s(data, "email")
    password = str(data.get("password") or "")
    if not email or not password:
        raise ApiError(400, "Please enter your email and password.")
    user = one(conn, """
        SELECT id, first_name, last_name, student_id, username, email, contact_number, password_hash,
               role, section_id, must_change_password
        FROM users WHERE lower(email) = lower(%(e)s) LIMIT 1""", {"e": email})
    if not user or not verify_password(conn, password, user["password_hash"]):
        raise ApiError(401, "Invalid email or password.")
    if user["role"] not in ("admin", "faculty"):
        raise ApiError(403, "You do not have permission to access this page.")
    user.pop("password_hash")
    user["must_change_password"] = bool(user["must_change_password"])
    return {"success": True, "message": "Login successful.", "user": user, "token": make_token(user),
            "must_change_password": user["must_change_password"]}


def change_password(conn, data, query, token):
    login_ = _s(data, "login") or _s(data, "email")
    current = str(data.get("current_password") or "")
    new = str(data.get("new_password") or "")
    errors = []
    if not login_:
        errors.append({"field": "login", "message": "Email or username is required."})
    if not current:
        errors.append({"field": "current_password", "message": "Current password is required."})
    if not new:
        errors.append({"field": "new_password", "message": "New password is required."})
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    if problem := password_problem(new):
        raise ApiError(422, problem, errors=[{"field": "new_password", "message": problem}])
    if new == current:
        raise ApiError(422, "Your new password must be different from the temporary password.",
                       errors=[{"field": "new_password", "message": "Use a different password."}])
    user = one(conn, "SELECT id, password_hash FROM users WHERE lower(email) = lower(%(l)s) OR lower(username) = lower(%(l)s) LIMIT 1",
               {"l": login_})
    if not user or not verify_password(conn, current, user["password_hash"]):
        raise ApiError(401, "Current password is incorrect.",
                       errors=[{"field": "current_password", "message": "Current password is incorrect."}])
    conn.execute("UPDATE users SET password_hash = %(h)s, must_change_password = FALSE WHERE id = %(id)s",
                 {"h": hash_password(conn, new), "id": user["id"]})
    return {"success": True, "message": "Password changed successfully."}


def set_password(conn, data, query, token, method="GET"):
    src = data if method == "POST" else query
    tok = _s(src, "token")
    if not tok or not re.fullmatch(r"[0-9a-fA-F]+", tok):
        raise ApiError(400, "This password setup link is not valid.")
    row = one(conn, """
        SELECT t.id AS token_id, t.expires_at, t.used_at, u.id, u.first_name, u.email, u.role
        FROM password_tokens t JOIN users u ON u.id = t.user_id
        WHERE t.token_hash = %(h)s LIMIT 1""", {"h": hashlib.sha256(tok.encode()).hexdigest()})
    if not row:
        raise ApiError(404, "This password setup link is not valid.")
    if row["used_at"] is not None:
        raise ApiError(410, "This link has already been used. Ask your administrator for a new one.")
    if row["expires_at"] < datetime.now(timezone.utc):
        raise ApiError(410, "This link has expired. Ask your administrator for a new one.")
    if method != "POST":
        return {"success": True, "user": {"first_name": row["first_name"], "email": row["email"], "role": row["role"]}}
    pw = str(data.get("new_password") or "")
    if not pw:
        raise ApiError(422, "New password is required.", errors=[{"field": "new_password", "message": "New password is required."}])
    if problem := password_problem(pw):
        raise ApiError(422, problem, errors=[{"field": "new_password", "message": problem}])
    with conn.transaction():
        conn.execute("UPDATE users SET password_hash = %(h)s, must_change_password = FALSE WHERE id = %(id)s",
                     {"h": hash_password(conn, pw), "id": row["id"]})
        conn.execute("UPDATE password_tokens SET used_at = now() WHERE user_id = %(u)s AND used_at IS NULL", {"u": row["id"]})
    return {"success": True, "message": "Your password has been set. You can now log in."}


# =================================================================== admin: dashboard + options
def admin_dashboard(conn, data, query, token):
    require_role(token, ["admin"])
    count = lambda sql: int(scalar(conn, sql))
    return {"success": True,
            "statistics": {"students": count("SELECT COUNT(*) FROM users WHERE role = 'student'"),
                           "faculty": count("SELECT COUNT(*) FROM users WHERE role = 'faculty'"),
                           "departments": count("SELECT COUNT(*) FROM departments"),
                           "sections": count("SELECT COUNT(*) FROM sections"),
                           "active_users": count("SELECT COUNT(*) FROM users")},
            "departments": _department_counts(conn)}


def _department_counts(conn):
    return all_rows(conn, """
        SELECT d.id, d.code, d.name, COUNT(DISTINCT s.id) AS sections,
               COUNT(DISTINCT u.id) AS students
        FROM departments d
        LEFT JOIN sections s ON s.department_id = d.id
        LEFT JOIN users u ON u.section_id = s.id AND u.role = 'student'
        GROUP BY d.id, d.code, d.name ORDER BY d.code""")


def admin_form_options(conn, data, query, token):
    require_role(token, ["admin"])
    lookup = acc.load_lookup(conn)
    return {"success": True,
            "departments": [{"id": d["id"], "code": d["code"], "name": d.get("name"), "label": acc.department_label(d)}
                            for d in lookup["departments"]],
            "sections": [{"id": s["id"], "name": s["name"], "department_id": s["department_id"]} for s in lookup["sections"]],
            "genders": acc.GENDERS, "age_range": [acc.MIN_AGE, acc.MAX_AGE],
            "email_configured": acc.Mailer.is_configured()}


# =================================================================== admin: users
USER_COLUMNS = """
    u.id, u.first_name, u.last_name, u.student_id, u.employee_id, u.username, u.email, u.contact_number,
    u.role, u.gender, u.age, u.must_change_password, u.section_id, s.name AS section,
    COALESCE(u.department_id, s.department_id) AS department_id, COALESCE(d.code, ds.code) AS department,
    u.created_at"""
USER_FROM = """
    FROM users u
    LEFT JOIN sections s ON s.id = u.section_id
    LEFT JOIN departments d ON d.id = u.department_id
    LEFT JOIN departments ds ON ds.id = s.department_id"""


def _user_out(u):
    u["must_change_password"] = bool(u["must_change_password"])
    u["status"] = "Pending setup" if u["must_change_password"] else "Active"
    return u


def admin_users(conn, data, query, token):
    require_role(token, ["admin"])
    where, params = ["TRUE"], {}
    if _s(query, "role"):
        where.append("u.role = %(role)s"); params["role"] = _s(query, "role").lower()
    if _s(query, "department"):
        where.append("upper(COALESCE(d.code, ds.code)) = upper(%(dept)s)"); params["dept"] = _s(query, "department")
    if _s(query, "section"):
        where.append("lower(s.name) = lower(%(sec)s)"); params["sec"] = _s(query, "section")
    if _s(query, "search"):
        where.append("(u.first_name ILIKE %(q)s OR u.last_name ILIKE %(q)s OR u.email ILIKE %(q)s OR u.username ILIKE %(q)s)")
        params["q"] = f"%{_s(query, 'search')}%"
    order = {"email": "lower(u.email)", "surname": "lower(u.last_name), lower(u.first_name), lower(u.email)"}.get(
        _s(query, "sort"),
        "COALESCE(d.code, ds.code) NULLS LAST, s.name NULLS LAST, lower(u.last_name), lower(u.first_name), lower(u.email)")
    rows = all_rows(conn, f"SELECT {USER_COLUMNS} {USER_FROM} WHERE {' AND '.join(where)} ORDER BY {order}", params)
    return {"success": True, "users": [_user_out(u) for u in rows]}


def admin_get_user(conn, data, query, token, user_id):
    require_role(token, ["admin"])
    u = one(conn, f"SELECT {USER_COLUMNS} {USER_FROM} WHERE u.id = %(id)s", {"id": user_id})
    if not u:
        raise ApiError(404, "User not found.")
    return {"success": True, "user": _user_out(u)}


def _create_response(result, account, mode, sent, mail_error):
    out = {"success": True,
           "message": f"Account created. An email was sent to {account['email']}." if sent
           else "Account created, but the email could not be sent.",
           "user": {"id": result["user"]["id"], "username": result["user"]["username"],
                    "email": result["user"]["email"], "name": f"{account['first_name']} {account['last_name']}"},
           "password_mode": mode, "email_sent": sent}
    if not sent:
        out["email_error"] = mail_error
        if result["temporary_password"]:
            out["temporary_password"] = result["temporary_password"]
        if result["setup_link"]:
            out["setup_link"] = result["setup_link"]
    return out


def admin_create_user(conn, data, query, token):
    admin = require_role(token, ["admin"])
    mode = "link" if data.get("password_mode") == "link" else "temporary"
    lookup = acc.load_lookup(conn)
    account, errors = acc.validate_account(data, lookup)
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    dups = acc.find_duplicates(conn, account)
    if dups:
        raise ApiError(409, dups[0]["message"], errors=dups)
    try:
        result = acc.create_account(conn, account, mode, int(admin["uid"]))
    except psycopg.Error as e:
        _db_error(e, "This email or student ID is already registered.", "Account could not be created (database error).")
    mailer = acc.Mailer()
    sent, mail_error = acc.send_account_email(mailer, result["user"], lookup, result["temporary_password"], result["setup_link"])
    mailer.close()
    return _create_response(result, account, mode, sent, mail_error)


def admin_bulk_create_users(conn, data, query, token):
    admin = require_role(token, ["admin"])
    rows = data.get("rows") or []
    mode = "link" if data.get("password_mode") == "link" else "temporary"
    validate_only = bool(data.get("validate_only"))
    if not isinstance(rows, list) or not rows:
        raise ApiError(400, "The file has no account rows.")
    if len(rows) > 50:
        raise ApiError(400, "Send at most 50 rows per request.")
    lookup = acc.load_lookup(conn)
    mailer = None
    results, seen_emails, seen_ids = [], {}, {}
    summary = {"total": len(rows), "valid": 0, "created": 0, "invalid": 0, "failed": 0, "emails_sent": 0}
    for i, raw in enumerate(rows):
        raw = raw if isinstance(raw, dict) else {}
        row_number = _i(raw, "row_number") or (i + 2)
        account, errors = acc.validate_account(raw, lookup)
        if account["email"]:
            if account["email"] in seen_emails:
                errors.append({"field": "email", "message": f"Duplicate email – also used in row {seen_emails[account['email']]}."})
            else:
                seen_emails[account["email"]] = row_number
        if account["student_id"]:
            if account["student_id"] in seen_ids:
                errors.append({"field": "student_id", "message": f"Duplicate student ID – also used in row {seen_ids[account['student_id']]}."})
            else:
                seen_ids[account["student_id"]] = row_number
        errors += acc.find_duplicates(conn, account)
        if errors:
            results.append({"row_number": row_number, "status": "invalid", "errors": errors}); summary["invalid"] += 1
            continue
        if validate_only:
            results.append({"row_number": row_number, "status": "valid", "errors": []}); summary["valid"] += 1
            continue
        try:
            created = acc.create_account(conn, account, mode, int(admin["uid"]))
        except psycopg.Error as e:
            msg = "Email or student ID is already registered." if isinstance(e, psycopg.errors.UniqueViolation) else "Database error while saving."
            results.append({"row_number": row_number, "status": "failed", "errors": [{"field": "", "message": msg}]})
            summary["failed"] += 1
            continue
        mailer = mailer or acc.Mailer()
        sent, mail_error = acc.send_account_email(mailer, created["user"], lookup, created["temporary_password"], created["setup_link"])
        row = {"row_number": row_number, "status": "created", "errors": [], "username": created["user"]["username"],
               "email": created["user"]["email"], "email_sent": sent}
        if not sent:
            row["email_error"] = mail_error
            if created["temporary_password"]:
                row["temporary_password"] = created["temporary_password"]
            if created["setup_link"]:
                row["setup_link"] = created["setup_link"]
        results.append(row)
        summary["created"] += 1
        summary["emails_sent"] += 1 if sent else 0
    if mailer:
        mailer.close()
    return {"success": True, "results": results, "summary": summary}


def admin_update_user(conn, data, query, token, user_id):
    require_role(token, ["admin"])
    existing = one(conn, "SELECT * FROM users WHERE id = %(id)s", {"id": user_id})
    if not existing:
        raise ApiError(404, "User not found.")
    if existing["role"] == "admin":
        raise ApiError(403, "Administrator accounts cannot be edited here.")
    merged = {k: existing.get(k) for k in ("first_name", "last_name", "email", "gender", "age", "role",
                                           "department_id", "section_id", "student_id", "employee_id", "contact_number")}
    merged.update({k: v for k, v in data.items() if v is not None})
    lookup = acc.load_lookup(conn)
    account, errors = acc.validate_account(merged, lookup)
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    dups = acc.find_duplicates(conn, account, exclude_id=user_id)
    if dups:
        raise ApiError(409, dups[0]["message"], errors=dups)
    try:
        conn.execute("""
            UPDATE users SET first_name=%(first_name)s, last_name=%(last_name)s, email=%(email)s, gender=%(gender)s,
                   age=%(age)s, role=%(role)s, department_id=%(department_id)s, section_id=%(section_id)s,
                   student_id=%(student_id)s, employee_id=%(employee_id)s, contact_number=%(contact_number)s
            WHERE id=%(id)s""", {**account, "student_id": account["student_id"] or None,
                                  "employee_id": account["employee_id"] or None,
                                  "contact_number": account["contact_number"] or None, "id": user_id})
    except psycopg.Error as e:
        _db_error(e, "This email or student ID is already registered.")
    return admin_get_user(conn, data, query, token, user_id) | {"message": "User updated successfully."}


def admin_delete_user(conn, data, query, token, user_id):
    admin = require_role(token, ["admin"])
    if int(admin["uid"]) == user_id:
        raise ApiError(409, "You cannot delete your own account.")
    try:
        with conn.transaction():
            conn.execute("UPDATE faculty SET user_id = NULL WHERE user_id = %(id)s", {"id": user_id})
            conn.execute("UPDATE notifications SET sender_id = NULL WHERE sender_id = %(id)s", {"id": user_id})
            conn.execute("DELETE FROM password_tokens WHERE user_id = %(id)s", {"id": user_id})
            cur = conn.execute("DELETE FROM users WHERE id = %(id)s", {"id": user_id})
    except psycopg.Error as e:
        _db_error(e, "User could not be deleted.")
    if cur.rowcount == 0:
        raise ApiError(404, "User not found.")
    return {"success": True, "message": "User deleted successfully."}


def admin_resend_invite(conn, data, query, token):
    require_role(token, ["admin"])
    user_id = _i(data, "user_id")
    mode = "temporary" if data.get("password_mode") == "temporary" else "link"
    if user_id <= 0:
        raise ApiError(400, "User ID is required.")
    user = one(conn, "SELECT * FROM users WHERE id = %(id)s", {"id": user_id})
    if not user:
        raise ApiError(404, "User not found.")
    if not user["must_change_password"]:
        raise ApiError(409, "This user has already set their own password.")
    temporary = setup_link = None
    with conn.transaction():
        if mode == "temporary":
            temporary = random_password(10)
            conn.execute("UPDATE users SET password_hash = %(h)s WHERE id = %(id)s", {"h": hash_password(conn, temporary), "id": user_id})
            conn.execute("UPDATE password_tokens SET used_at = now() WHERE user_id = %(u)s AND used_at IS NULL", {"u": user_id})
        else:
            setup_link = acc.new_setup_link(conn, user_id)
    mailer = acc.Mailer()
    sent, mail_error = acc.send_account_email(mailer, user, acc.load_lookup(conn), temporary, setup_link)
    mailer.close()
    out = {"success": True, "email_sent": sent, "username": user["username"],
           "message": f"A new email was sent to {user['email']}." if sent else "The email could not be sent."}
    if not sent:
        out["email_error"] = mail_error
        if temporary:
            out["temporary_password"] = temporary
        if setup_link:
            out["setup_link"] = setup_link
    return out


# =================================================================== departments
def departments_list(conn, data, query, token):
    require_role(token, ["admin"])
    return {"success": True, "departments": _department_counts(conn)}


def department_get(conn, data, query, token, dept_id):
    require_role(token, ["admin", "faculty"])
    d = one(conn, "SELECT id, code, name FROM departments WHERE id = %(id)s", {"id": dept_id})
    if not d:
        raise ApiError(404, "Department not found.")
    d["sections"] = all_rows(conn, "SELECT id, name FROM sections WHERE department_id = %(id)s ORDER BY name", {"id": dept_id})
    return {"success": True, "department": d}


def department_save(conn, data, query, token, dept_id=None):
    require_role(token, ["admin"])
    code, name = _s(data, "code").upper(), _s(data, "name")
    if not code:
        raise ApiError(422, REQUIRED_MSG, errors=[{"field": "code", "message": "Department code is required."}])
    try:
        if dept_id is None:
            row = one(conn, "INSERT INTO departments (code, name) VALUES (%(c)s, %(n)s) RETURNING id, code, name",
                      {"c": code, "n": name or None})
            return {"success": True, "message": "Department added successfully.", "department": row}
        row = one(conn, "UPDATE departments SET code = %(c)s, name = %(n)s WHERE id = %(id)s RETURNING id, code, name",
                  {"c": code, "n": name or None, "id": dept_id})
    except psycopg.Error as e:
        _db_error(e, "Department code already exists.")
    if not row:
        raise ApiError(404, "Department not found.")
    return {"success": True, "message": "Department updated successfully.", "department": row}


def department_delete(conn, data, query, token, dept_id):
    require_role(token, ["admin"])
    try:
        cur = conn.execute("DELETE FROM departments WHERE id = %(id)s", {"id": dept_id})
    except psycopg.Error as e:
        _db_error(e, "", "Delete operation failed.")
    if cur.rowcount == 0:
        raise ApiError(404, "Department not found.")
    return {"success": True, "message": "Department deleted successfully."}


def admin_department_students(conn, data, query, token, dept_id=None):
    require_role(token, ["admin"])
    dept_id = dept_id if dept_id is not None else _i(query, "department_id")
    if dept_id <= 0:
        raise ApiError(400, "Department is required.")
    dept = one(conn, "SELECT id, code, name FROM departments WHERE id = %(id)s", {"id": dept_id})
    if not dept:
        raise ApiError(404, "Department not found.")
    rows = all_rows(conn, """
        SELECT u.id, u.first_name, u.last_name, u.email, u.student_id, s.name AS section, u.must_change_password
        FROM users u LEFT JOIN sections s ON s.id = u.section_id
        WHERE u.role = 'student' AND COALESCE(u.department_id, s.department_id) = %(d)s
        ORDER BY s.name NULLS LAST, lower(u.last_name), lower(u.first_name), lower(u.email)""", {"d": dept_id})
    for r in rows:
        r["status"] = "Pending setup" if r.pop("must_change_password") else "Active"
    return {"success": True, "department": dept, "students": rows}


# =================================================================== sections
def sections_list(conn, data, query, token):
    require_login(token)
    params, where = {}, "TRUE"
    if _i(query, "department_id"):
        where, params = "s.department_id = %(d)s", {"d": _i(query, "department_id")}
    rows = all_rows(conn, f"""
        SELECT s.id, s.name, s.department_id, d.code AS department, r.id AS room_id, r.room_name AS home_room,
               (SELECT COUNT(*) FROM users u WHERE u.section_id = s.id AND u.role = 'student') AS students
        FROM sections s JOIN departments d ON d.id = s.department_id
        LEFT JOIN section_rooms sr ON sr.section_id = s.id LEFT JOIN rooms r ON r.id = sr.room_id
        WHERE {where} ORDER BY s.name""", params)
    return {"success": True, "sections": rows}


def section_get(conn, data, query, token, section_id):
    rows = [s for s in sections_list(conn, data, {}, token)["sections"] if s["id"] == section_id]
    if not rows:
        raise ApiError(404, "Section not found.")
    return {"success": True, "section": rows[0]}


def section_save(conn, data, query, token, section_id=None):
    require_role(token, ["admin"])
    name, dept = _s(data, "name").upper(), _i(data, "department_id")
    errors = []
    if not name:
        errors.append({"field": "name", "message": "Section name is required."})
    if dept <= 0:
        errors.append({"field": "department_id", "message": "Department is required."})
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    if not one(conn, "SELECT 1 FROM departments WHERE id = %(d)s", {"d": dept}):
        raise ApiError(404, "Department not found.")
    try:
        if section_id is None:
            row = one(conn, "INSERT INTO sections (name, department_id) VALUES (%(n)s, %(d)s) RETURNING id, name, department_id",
                      {"n": name, "d": dept})
            return {"success": True, "message": "Section added successfully.", "section": row}
        row = one(conn, "UPDATE sections SET name = %(n)s, department_id = %(d)s WHERE id = %(id)s RETURNING id, name, department_id",
                  {"n": name, "d": dept, "id": section_id})
    except psycopg.Error as e:
        _db_error(e, "Section name already exists.")
    if not row:
        raise ApiError(404, "Section not found.")
    return {"success": True, "message": "Section updated successfully.", "section": row}


def section_delete(conn, data, query, token, section_id):
    require_role(token, ["admin"])
    try:
        with conn.transaction():
            conn.execute("DELETE FROM section_rooms WHERE section_id = %(id)s", {"id": section_id})
            cur = conn.execute("DELETE FROM sections WHERE id = %(id)s", {"id": section_id})
    except psycopg.Error as e:
        _db_error(e, "", "Delete operation failed.")
    if cur.rowcount == 0:
        raise ApiError(404, "Section not found.")
    return {"success": True, "message": "Section deleted successfully."}


# =================================================================== rooms
ROOM_COLS = "id, room_name, building, floor, capacity, status"


def rooms_list(conn, data, query, token):
    require_login(token)
    q = _s(query, "search")
    if not q:
        rows = all_rows(conn, f"SELECT {ROOM_COLS} FROM rooms ORDER BY building, room_name")
    else:
        rows = all_rows(conn, f"""SELECT {ROOM_COLS} FROM rooms
            WHERE room_name ILIKE %(q)s OR building ILIKE %(q)s OR CAST(floor AS TEXT) ILIKE %(q)s
            ORDER BY building, room_name""", {"q": f"%{q}%"})
    return {"success": True, "data": rows}


def room_get(conn, data, query, token, room_id):
    require_login(token)
    r = one(conn, f"SELECT {ROOM_COLS} FROM rooms WHERE id = %(id)s", {"id": room_id})
    if not r:
        raise ApiError(404, "Room not found.")
    return {"success": True, "room": r}


def room_save(conn, data, query, token, room_id=None):
    require_role(token, ["admin"])
    fields = ["room_name", "building", "floor", "capacity", "status"]
    for f in fields:
        if _s(data, f) == "":
            raise ApiError(400, f"{f.replace('_', ' ').title()} is required.")
    floor, capacity, status = _i(data, "floor"), _i(data, "capacity"), _s(data, "status")
    if floor < 0 or capacity <= 0:
        raise ApiError(400, "Floor and capacity must be valid numbers.")
    if status not in ROOM_STATUSES:
        raise ApiError(400, "Invalid room status.")
    p = {"n": _s(data, "room_name"), "b": _s(data, "building"), "f": floor, "c": capacity, "s": status}
    try:
        if room_id is None:
            row = one(conn, f"INSERT INTO rooms (room_name, building, floor, capacity, status) "
                            f"VALUES (%(n)s, %(b)s, %(f)s, %(c)s, %(s)s) RETURNING {ROOM_COLS}", p)
            return {"success": True, "message": "Room added successfully.", "room": row}
        row = one(conn, f"UPDATE rooms SET room_name=%(n)s, building=%(b)s, floor=%(f)s, capacity=%(c)s, status=%(s)s "
                        f"WHERE id=%(id)s RETURNING {ROOM_COLS}", {**p, "id": room_id})
    except psycopg.Error as e:
        _db_error(e, "Room name already exists.", "Save operation failed.")
    if not row:
        raise ApiError(404, "Room not found.")
    return {"success": True, "message": "Room updated successfully.", "room": row}


def room_delete(conn, data, query, token, room_id):
    require_role(token, ["admin"])
    try:
        cur = conn.execute("DELETE FROM rooms WHERE id = %(id)s", {"id": room_id})
    except psycopg.errors.ForeignKeyViolation:
        raise ApiError(409, "This room is used by a schedule and cannot be deleted.")
    except psycopg.Error:
        raise ApiError(500, "Delete operation failed.")
    if cur.rowcount == 0:
        raise ApiError(404, "Room not found.")
    return {"success": True, "message": "Room deleted successfully."}


# =================================================================== subjects
def subjects_list(conn, data, query, token):
    require_login(token)
    return {"success": True, "subjects": all_rows(conn, "SELECT id, code, name FROM subjects ORDER BY code, name")}


def subject_get(conn, data, query, token, subject_id):
    require_login(token)
    s = one(conn, "SELECT id, code, name FROM subjects WHERE id = %(id)s", {"id": subject_id})
    if not s:
        raise ApiError(404, "Subject not found.")
    return {"success": True, "subject": s}


def subject_save(conn, data, query, token, subject_id=None):
    require_role(token, ["admin"])
    code, name = _s(data, "code").upper(), _s(data, "name")
    errors = [{"field": f, "message": m} for f, v, m in (("code", code, "Subject code is required."),
                                                         ("name", name, "Subject name is required.")) if not v]
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    if one(conn, "SELECT 1 FROM subjects WHERE upper(code) = %(c)s AND lower(name) = lower(%(n)s) AND id <> %(id)s",
           {"c": code, "n": name, "id": subject_id or 0}):
        raise ApiError(409, "A subject with this code and name already exists.",
                       errors=[{"field": "code", "message": "This subject already exists."}])
    try:
        if subject_id is None:
            row = one(conn, "INSERT INTO subjects (code, name) VALUES (%(c)s, %(n)s) RETURNING id, code, name", {"c": code, "n": name})
            return {"success": True, "message": "Subject added successfully.", "subject": row}
        row = one(conn, "UPDATE subjects SET code=%(c)s, name=%(n)s WHERE id=%(id)s RETURNING id, code, name",
                  {"c": code, "n": name, "id": subject_id})
    except psycopg.Error as e:
        _db_error(e, "A subject with this code and name already exists.", "Save operation failed.")
    if not row:
        raise ApiError(404, "Subject not found.")
    return {"success": True, "message": "Subject updated successfully.", "subject": row}


def subject_delete(conn, data, query, token, subject_id):
    require_role(token, ["admin"])
    try:
        with conn.transaction():
            conn.execute("DELETE FROM faculty_subjects WHERE subject_id = %(id)s", {"id": subject_id})
            cur = conn.execute("DELETE FROM subjects WHERE id = %(id)s", {"id": subject_id})
    except psycopg.errors.ForeignKeyViolation:
        raise ApiError(409, "This subject is used by a schedule and cannot be deleted.")
    if cur.rowcount == 0:
        raise ApiError(404, "Subject not found.")
    return {"success": True, "message": "Subject deleted successfully."}


# =================================================================== faculty records
def faculty_list(conn, data, query, token):
    require_login(token)
    rows = all_rows(conn, """SELECT f.id, f.first_name, f.last_name, COALESCE(u.email, f.email) AS email, f.user_id
                             FROM faculty f LEFT JOIN users u ON u.id = f.user_id ORDER BY f.last_name, f.first_name""")
    return {"success": True, "faculty": rows}


def faculty_get(conn, data, query, token, faculty_id):
    require_login(token)
    f = one(conn, """SELECT f.id, f.first_name, f.last_name, COALESCE(u.email, f.email) AS email, f.user_id
                     FROM faculty f LEFT JOIN users u ON u.id = f.user_id WHERE f.id = %(id)s""", {"id": faculty_id})
    if not f:
        raise ApiError(404, "Faculty member not found.")
    f["subjects"] = all_rows(conn, """SELECT sub.id, sub.code, sub.name, d.code AS department FROM faculty_subjects fs
        JOIN subjects sub ON sub.id = fs.subject_id JOIN departments d ON d.id = fs.department_id
        WHERE fs.faculty_id = %(id)s ORDER BY sub.code, d.code""", {"id": faculty_id})
    return {"success": True, "faculty": f}


# =================================================================== schedules
SCHEDULE_SELECT = """
    SELECT sc.id, sc.section_id, sec.name AS section, sc.subject_id, sub.code AS subject_code, sub.name AS subject,
           sc.room_id, r.room_name AS room, r.building, sc.faculty_id,
           COALESCE(f.first_name || ' ' || f.last_name, 'TBA') AS faculty, sc.day_of_week,
           TO_CHAR(sc.start_time, 'HH24:MI') AS start_time, TO_CHAR(sc.end_time, 'HH24:MI') AS end_time
    FROM schedules sc
    JOIN sections sec ON sec.id = sc.section_id
    JOIN subjects sub ON sub.id = sc.subject_id
    JOIN rooms r ON r.id = sc.room_id
    LEFT JOIN faculty f ON f.id = sc.faculty_id"""
DAY_ORDER = "array_position(ARRAY['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'], sc.day_of_week)"


def schedules_list(conn, data, query, token):
    require_login(token)
    where, params = ["TRUE"], {}
    for key in ("section_id", "room_id", "faculty_id", "subject_id"):
        if _i(query, key):
            where.append(f"sc.{key} = %({key})s"); params[key] = _i(query, key)
    if _s(query, "day_of_week"):
        where.append("lower(sc.day_of_week) = lower(%(day)s)"); params["day"] = _s(query, "day_of_week")
    if _i(query, "department_id"):
        where.append("sec.department_id = %(dept)s"); params["dept"] = _i(query, "department_id")
    rows = all_rows(conn, f"{SCHEDULE_SELECT} WHERE {' AND '.join(where)} ORDER BY {DAY_ORDER}, sc.start_time, sec.name", params)
    return {"success": True, "schedules": rows}


def schedule_get(conn, data, query, token, schedule_id):
    require_login(token)
    s = one(conn, f"{SCHEDULE_SELECT} WHERE sc.id = %(id)s", {"id": schedule_id})
    if not s:
        raise ApiError(404, "Schedule not found.")
    return {"success": True, "schedule": s}


def schedule_options(conn, data, query, token):
    require_login(token)
    return {"success": True,
            "sections": all_rows(conn, "SELECT id, name FROM sections ORDER BY name"),
            "subjects": all_rows(conn, "SELECT id, code, name FROM subjects ORDER BY code"),
            "faculty": all_rows(conn, "SELECT id, first_name, last_name FROM faculty ORDER BY last_name, first_name"),
            "rooms": all_rows(conn, "SELECT id, room_name, building, floor FROM rooms ORDER BY room_name")}


def _fmt12(t):
    h, m = (int(x) for x in t.split(":")[:2])
    return f"{(h % 12) or 12}:{m:02d} {'AM' if h < 12 else 'PM'}"


def schedule_save(conn, data, query, token, schedule_id=None):
    """Create or update a class after checking room, faculty and section conflicts."""
    require_role(token, ["admin", "faculty"])
    sec, sub, room, fac = (_i(data, k) for k in ("section_id", "subject_id", "room_id", "faculty_id"))
    day = _s(data, "day_of_week").lower().capitalize()
    start, end = _s(data, "start_time"), _s(data, "end_time")
    errors = []
    if sec <= 0: errors.append({"field": "section_id", "message": "Section is required."})
    if sub <= 0: errors.append({"field": "subject_id", "message": "Subject is required."})
    if fac <= 0: errors.append({"field": "faculty_id", "message": "Faculty is required."})
    if day not in CLASS_DAYS: errors.append({"field": "day_of_week", "message": "Day must be Monday to Saturday."})
    if not TIME_RE.match(start): errors.append({"field": "start_time", "message": "Start time is required."})
    if not TIME_RE.match(end): errors.append({"field": "end_time", "message": "End time is required."})
    if room <= 0: errors.append({"field": "room_id", "message": "Room is required."})
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    if dtime.fromisoformat(end if len(end) > 5 else end + ":00") <= dtime.fromisoformat(start if len(start) > 5 else start + ":00"):
        raise ApiError(422, "End time must be later than the start time.",
                       errors=[{"field": "end_time", "message": "End time must be later than the start time."}])
    overlaps = all_rows(conn, """
        SELECT sc.id, sc.section_id, sc.room_id, sc.faculty_id, sec.name AS section, sub.code AS subject_code,
               sub.name AS subject, r.room_name AS room, CONCAT(f.first_name, ' ', f.last_name) AS faculty,
               sc.day_of_week, TO_CHAR(sc.start_time, 'HH24:MI') AS start_time, TO_CHAR(sc.end_time, 'HH24:MI') AS end_time
        FROM schedules sc
        JOIN sections sec ON sec.id = sc.section_id JOIN subjects sub ON sub.id = sc.subject_id
        JOIN rooms r ON r.id = sc.room_id LEFT JOIN faculty f ON f.id = sc.faculty_id
        WHERE lower(sc.day_of_week) = lower(%(day)s)
          AND sc.start_time < CAST(%(end)s AS time) AND CAST(%(start)s AS time) < sc.end_time
          AND (sc.room_id = %(room)s OR sc.faculty_id = %(fac)s OR sc.section_id = %(sec)s)
          AND sc.id <> %(self)s
        ORDER BY sc.start_time""", {"day": day, "start": start, "end": end, "room": room, "fac": fac, "sec": sec,
                                    "self": schedule_id or 0})
    if overlaps:
        conflicts = []
        for c in overlaps:
            when = f"{c['day_of_week']} {_fmt12(c['start_time'])} – {_fmt12(c['end_time'])}"
            cls = f"{c['subject_code']} {c['subject']} ({c['section']})"
            if c["room_id"] == room:
                conflicts.append({"type": "room", "message": "The selected room is already occupied during this time.",
                                  "detail": f"{c['room']} is used by {cls} on {when}."})
            if c["faculty_id"] == fac:
                conflicts.append({"type": "faculty", "message": "The selected faculty member already has a class during this time.",
                                  "detail": f"{(c['faculty'] or '').strip()} teaches {cls} in {c['room']} on {when}."})
            if c["section_id"] == sec:
                conflicts.append({"type": "section", "message": "The selected section already has a class during this time.",
                                  "detail": f"{c['section']} has {c['subject_code']} {c['subject']} in {c['room']} on {when}."})
        headline = {
            "room": "The selected room is already occupied during this time. Please choose another room or schedule.",
            "faculty": "The selected faculty member already has a class during this time. Please choose another faculty member or schedule.",
            "section": "The selected section already has a class during this time. Please choose another schedule.",
        }[conflicts[0]["type"]]
        raise ApiError(409, "Scheduling Conflict: " + headline, conflict=True, conflicts=conflicts)
    p = {"sec": sec, "sub": sub, "room": room, "fac": fac, "day": day, "start": start, "end": end}
    try:
        if schedule_id is None:
            new_id = scalar(conn, """INSERT INTO schedules (section_id, subject_id, room_id, faculty_id, day_of_week, start_time, end_time)
                VALUES (%(sec)s, %(sub)s, %(room)s, %(fac)s, %(day)s, %(start)s, %(end)s) RETURNING id""", p)
            return {"success": True, "message": "Schedule saved successfully. No conflicts found.", "schedule_id": new_id}
        cur = conn.execute("""UPDATE schedules SET section_id=%(sec)s, subject_id=%(sub)s, room_id=%(room)s, faculty_id=%(fac)s,
               day_of_week=%(day)s, start_time=%(start)s, end_time=%(end)s WHERE id=%(id)s""", {**p, "id": schedule_id})
    except psycopg.errors.ForeignKeyViolation:
        raise ApiError(404, "The selected section, subject, room or faculty member does not exist.")
    if cur.rowcount == 0:
        raise ApiError(404, "Schedule not found.")
    return {"success": True, "message": "Schedule updated successfully. No conflicts found.", "schedule_id": schedule_id}


def schedule_delete(conn, data, query, token, schedule_id):
    require_role(token, ["admin", "faculty"])
    cur = conn.execute("DELETE FROM schedules WHERE id = %(id)s", {"id": schedule_id})
    if cur.rowcount == 0:
        raise ApiError(404, "Schedule not found.")
    return {"success": True, "message": "Schedule deleted successfully."}


# =================================================================== section home rooms
def section_rooms_list(conn, data, query, token):
    require_login(token)
    return {"success": True, "section_rooms": all_rows(conn, """
        SELECT s.id AS section_id, s.name AS section, r.id AS room_id, r.room_name AS room, r.building, r.floor
        FROM section_rooms sr JOIN sections s ON s.id = sr.section_id JOIN rooms r ON r.id = sr.room_id
        ORDER BY s.name""")}


def assign_room(conn, data, query, token, section_id=None):
    require_role(token, ["admin", "faculty"])
    section_id = section_id if section_id is not None else _i(data, "section_id")
    room_id = _i(data, "room_id")
    if section_id <= 0 or room_id <= 0:
        raise ApiError(400, "Section and room are required.")
    if not one(conn, "SELECT id FROM sections WHERE id = %(s)s", {"s": section_id}):
        raise ApiError(404, "Section not found.")
    if not one(conn, "SELECT id FROM rooms WHERE id = %(r)s", {"r": room_id}):
        raise ApiError(404, "Room not found.")
    conn.execute("""INSERT INTO section_rooms (section_id, room_id) VALUES (%(s)s, %(r)s)
                    ON CONFLICT (section_id) DO UPDATE SET room_id = EXCLUDED.room_id""", {"s": section_id, "r": room_id})
    return {"success": True, "message": "Room assigned successfully."}


def unassign_room(conn, data, query, token, section_id):
    require_role(token, ["admin"])
    cur = conn.execute("DELETE FROM section_rooms WHERE section_id = %(s)s", {"s": section_id})
    if cur.rowcount == 0:
        raise ApiError(404, "This section has no home room.")
    return {"success": True, "message": "Home room removed."}


# =================================================================== student portal (Android app)
def student_schedule(conn, data, query, token, user_id=None):
    uid = _own_id(token, user_id if user_id is not None else _i(query, "user_id"), ANY_ROLE)
    rows = all_rows(conn, """
        SELECT sc.id, sub.code AS subject_code, sub.name AS subject_name, sc.day_of_week,
               TO_CHAR(sc.start_time, 'HH24:MI') AS start_time, TO_CHAR(sc.end_time, 'HH24:MI') AS end_time,
               r.room_name, r.building, COALESCE(f.first_name || ' ' || f.last_name, 'TBA') AS faculty_name
        FROM schedules sc JOIN subjects sub ON sub.id = sc.subject_id JOIN rooms r ON r.id = sc.room_id
        LEFT JOIN faculty f ON f.id = sc.faculty_id JOIN sections sec ON sec.id = sc.section_id
        JOIN users u ON u.section_id = sec.id
        WHERE u.id = %(i)s ORDER BY sc.start_time""", {"i": uid})
    return {"success": True, "data": rows}


def student_room(conn, data, query, token, user_id=None):
    uid = _own_id(token, user_id if user_id is not None else _i(query, "user_id"), ANY_ROLE)
    if uid <= 0:
        raise ApiError(400, "User ID is required.")
    room = one(conn, """
        SELECT s.id AS section_id, s.name AS section_name, r.id AS room_id, r.room_name, r.building, r.floor, r.capacity, r.status
        FROM users u JOIN sections s ON s.id = u.section_id
        LEFT JOIN section_rooms sr ON sr.section_id = s.id LEFT JOIN rooms r ON r.id = sr.room_id
        WHERE u.id = %(i)s AND u.role = 'student' LIMIT 1""", {"i": uid})
    if not room:
        raise ApiError(404, "Student account not found.")
    if room["room_id"] is None:
        return {"success": False, "message": "No room has been assigned to your section yet."}
    return {"success": True, "message": "Assigned room loaded.", "room": room}


def profile(conn, data, query, token, user_id=None):
    uid = _own_id(token, user_id if user_id is not None else _i(query, "user_id"), ANY_ROLE)
    u = one(conn, """
        SELECT u.id, u.first_name, u.last_name, u.student_id, u.username, u.email, u.contact_number, u.role,
               s.name AS section_name, d.code AS program
        FROM users u LEFT JOIN sections s ON s.id = u.section_id
        LEFT JOIN departments d ON d.id = COALESCE(u.department_id, s.department_id)
        WHERE u.id = %(i)s""", {"i": uid})
    if not u:
        raise ApiError(404, "User not found.")
    return {"success": True, "message": "Profile loaded.", "user": u}


# =================================================================== faculty portal (website)
def _faculty_department(conn, uid):
    if uid <= 0:
        raise ApiError(400, "Invalid user ID.")
    row = one(conn, """SELECT COALESCE(u.department_id, s.department_id) AS department_id
                       FROM users u LEFT JOIN sections s ON s.id = u.section_id
                       WHERE u.id = %(u)s AND u.role = 'faculty' LIMIT 1""", {"u": uid})
    if not row or row["department_id"] is None:
        raise ApiError(404, "Faculty account not found.")
    return row["department_id"]


def faculty_dashboard(conn, data, query, token, user_id=None):
    uid = _own_id(token, user_id if user_id is not None else _i(query, "user_id"), ["faculty", "admin"])
    if uid <= 0:
        raise ApiError(400, "Invalid user ID.")
    f = one(conn, """
        SELECT u.id, u.first_name, u.last_name, u.username, u.email, u.role, s.id AS section_id, s.name AS section_name,
               d.id AS department_id, d.code AS department
        FROM users u LEFT JOIN sections s ON s.id = u.section_id
        LEFT JOIN departments d ON d.id = COALESCE(u.department_id, s.department_id)
        WHERE u.id = %(u)s AND u.role = 'faculty' LIMIT 1""", {"u": uid})
    if not f:
        raise ApiError(404, "Faculty account not found.")
    sections = all_rows(conn, """
        SELECT s.id, s.name, COUNT(u.id) AS students FROM sections s
        LEFT JOIN users u ON u.section_id = s.id AND u.role = 'student'
        WHERE s.department_id = %(d)s GROUP BY s.id, s.name ORDER BY s.id""", {"d": f["department_id"]})
    return {"success": True,
            "faculty": {k: f[k] for k in ("id", "first_name", "last_name", "username", "email", "role")},
            "department": {"id": f["department_id"], "code": f["department"]},
            "assigned_section": {"id": f["section_id"], "name": f["section_name"]},
            "sections": sections}


def faculty_members(conn, data, query, token, user_id=None):
    dept = _faculty_department(conn, _own_id(token, user_id if user_id is not None else _i(query, "user_id"),
                                             ["faculty", "admin"]))
    rows = all_rows(conn, """
        SELECT f.id, f.first_name, f.last_name, COALESCE(u.email, f.email) AS email,
          (SELECT string_agg(DISTINCT sub.code || ' – ' || sub.name, ', ') FROM faculty_subjects fs
             JOIN subjects sub ON sub.id = fs.subject_id WHERE fs.faculty_id = f.id AND fs.department_id = %(d)s) AS subjects,
          (SELECT string_agg(sc.day_of_week || ' ' || TO_CHAR(sc.start_time, 'FMHH12:MI AM') || ' – ' ||
                             TO_CHAR(sc.end_time, 'FMHH12:MI AM') || ' (' || sec.name || ')', '; '
                             ORDER BY array_position(ARRAY['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'],
                                                     sc.day_of_week), sc.start_time)
             FROM schedules sc JOIN sections sec ON sec.id = sc.section_id
            WHERE sc.faculty_id = f.id AND sec.department_id = %(d)s) AS schedule,
          (SELECT string_agg(DISTINCT r.room_name, ', ') FROM schedules sc JOIN sections sec ON sec.id = sc.section_id
             JOIN rooms r ON r.id = sc.room_id WHERE sc.faculty_id = f.id AND sec.department_id = %(d)s) AS rooms
        FROM faculty f LEFT JOIN users u ON u.id = f.user_id LEFT JOIN sections us ON us.id = u.section_id
        WHERE EXISTS (SELECT 1 FROM faculty_subjects fs WHERE fs.faculty_id = f.id AND fs.department_id = %(d)s)
           OR EXISTS (SELECT 1 FROM schedules sc JOIN sections sec ON sec.id = sc.section_id
                      WHERE sc.faculty_id = f.id AND sec.department_id = %(d)s)
           OR COALESCE(u.department_id, us.department_id) = %(d)s
        ORDER BY f.last_name, f.first_name""", {"d": dept})
    return {"success": True, "faculty": rows}


def faculty_schedule(conn, data, query, token, user_id=None):
    dept = _faculty_department(conn, _own_id(token, user_id if user_id is not None else _i(query, "user_id"),
                                             ["faculty", "admin"]))
    rows = all_rows(conn, """
        SELECT s.id, s.section_id, sec.name AS section, s.subject_id, sub.code AS subject_code, sub.name AS subject,
               s.room_id, r.room_name AS room, s.faculty_id, CONCAT(f.first_name, ' ', f.last_name) AS faculty,
               s.day_of_week, TO_CHAR(s.start_time, 'HH24:MI') AS start_time, TO_CHAR(s.end_time, 'HH24:MI') AS end_time
        FROM schedules s JOIN sections sec ON sec.id = s.section_id JOIN subjects sub ON sub.id = s.subject_id
        JOIN rooms r ON r.id = s.room_id JOIN faculty f ON f.id = s.faculty_id
        WHERE sec.department_id = %(d)s
        ORDER BY array_position(ARRAY['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'], s.day_of_week),
                 s.start_time, sec.name""", {"d": dept})
    return {"success": True, "schedules": rows}


def faculty_students(conn, data, query, token, user_id=None):
    dept = _faculty_department(conn, _own_id(token, user_id if user_id is not None else _i(query, "user_id"),
                                             ["faculty", "admin"]))
    rows = all_rows(conn, """
        SELECT u.id, u.student_id, u.first_name, u.last_name, u.email, s.name AS section
        FROM users u JOIN sections s ON s.id = u.section_id
        WHERE u.role = 'student' AND s.department_id = %(d)s ORDER BY s.id, u.last_name, u.first_name""", {"d": dept})
    return {"success": True, "students": rows}


# =================================================================== notifications
def notify_send(conn, data, query, token):
    tok = require_role(token, ["faculty"])          # only faculty send notices (UC-04)
    program = _s(data, "program").upper()
    program = "BSMT" if program in ("BSMEDTECH", "MEDTECH") else program
    section_name = _s(data, "section")
    kind = "transfer" if data.get("kind") == "transfer" else "announcement"
    title, body = _s(data, "title"), _s(data, "body")
    errors = [{"field": f, "message": m} for f, v, m in (("program", program, "Program is required."),
                                                         ("title", title, "Title is required."),
                                                         ("body", body, "Message is required.")) if not v]
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    if len(title) > 150 or len(body) > 500:
        raise ApiError(422, "The message is too long.")
    dept = one(conn, "SELECT id, code FROM departments WHERE upper(code) = %(c)s", {"c": program})
    if not dept:
        raise ApiError(404, f'Program "{program}" does not exist.')
    section_id = None
    if section_name:
        section_id = scalar(conn, "SELECT id FROM sections WHERE lower(name) = lower(%(n)s) AND department_id = %(d)s",
                            {"n": section_name, "d": dept["id"]})
        if not section_id:
            raise ApiError(404, f"Section {section_name} is not part of {dept['code']}.")
    sender = one(conn, "SELECT first_name, last_name FROM users WHERE id = %(id)s", {"id": tok["uid"]})
    if not sender:
        raise ApiError(401, "Your session has expired. Please log in again.")
    new_id = scalar(conn, """INSERT INTO notifications (sender_id, sender_name, department_id, section_id, kind, title, body)
        VALUES (%(s)s, %(sn)s, %(d)s, %(sec)s, %(k)s, %(t)s, %(b)s) RETURNING id""",
                    {"s": tok["uid"], "sn": f"{sender['first_name']} {sender['last_name']}".strip(), "d": dept["id"],
                     "sec": section_id, "k": kind, "t": title, "b": body})
    count = int(scalar(conn, """SELECT COUNT(*) FROM users u LEFT JOIN sections s ON s.id = u.section_id
        WHERE u.role = 'student' AND COALESCE(u.department_id, s.department_id) = %(d)s
          AND (CAST(%(sec)s AS INTEGER) IS NULL OR u.section_id = CAST(%(sec)s AS INTEGER))""",
                           {"d": dept["id"], "sec": section_id}))
    return {"success": True, "message": f"Notification sent to {count} student{'' if count == 1 else 's'}.",
            "id": new_id, "recipients": count}


def notifications(conn, data, query, token):
    tok = require_role(token, ["student"])
    me = one(conn, """SELECT u.section_id, COALESCE(u.department_id, s.department_id) AS department_id
                      FROM users u LEFT JOIN sections s ON s.id = u.section_id WHERE u.id = %(id)s""", {"id": tok["uid"]})
    if not me:
        raise ApiError(401, "Your session has expired. Please log in again.")
    rows = all_rows(conn, """
        SELECT n.id, n.kind, n.title, n.body, n.sender_name AS "from", COALESCE(sec.name, 'All sections') AS section,
               to_char(n.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS created_at,
               (r.user_id IS NULL) AS unread
        FROM notifications n LEFT JOIN sections sec ON sec.id = n.section_id
        LEFT JOIN notification_reads r ON r.notification_id = n.id AND r.user_id = %(me)s
        WHERE n.department_id = %(d)s AND (n.section_id IS NULL OR n.section_id = %(sec)s)
        ORDER BY n.created_at DESC, n.id DESC LIMIT 100""",
                    {"me": tok["uid"], "d": me["department_id"], "sec": me["section_id"]})
    return {"success": True, "unread": sum(1 for r in rows if r["unread"]), "data": rows}


def notifications_read(conn, data, query, token, notification_id=None):
    tok = require_role(token, ["student"])
    if data.get("all"):
        conn.execute("""
            INSERT INTO notification_reads (notification_id, user_id)
            SELECT n.id, u.id FROM users u LEFT JOIN sections s ON s.id = u.section_id
            JOIN notifications n ON n.department_id = COALESCE(u.department_id, s.department_id)
             AND (n.section_id IS NULL OR n.section_id = u.section_id)
            WHERE u.id = %(me)s ON CONFLICT DO NOTHING""", {"me": tok["uid"]})
    else:
        nid = notification_id if notification_id is not None else _i(data, "id")
        if nid <= 0:
            raise ApiError(400, "Notification ID is required.")
        conn.execute("INSERT INTO notification_reads (notification_id, user_id) VALUES (%(n)s, %(me)s) ON CONFLICT DO NOTHING",
                     {"n": nid, "me": tok["uid"]})
    return {"success": True, "message": "Marked as read."}


def notifications_sent(conn, data, query, token):
    tok = require_role(token, ["faculty", "admin"])
    rows = all_rows(conn, """
        SELECT n.id, n.kind, n.title, n.body, d.code AS program, COALESCE(sec.name, 'All sections') AS section,
               to_char(n.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS created_at,
               (SELECT COUNT(*) FROM notification_reads r WHERE r.notification_id = n.id) AS read_count
        FROM notifications n JOIN departments d ON d.id = n.department_id LEFT JOIN sections sec ON sec.id = n.section_id
        WHERE n.sender_id = %(me)s OR %(admin)s
        ORDER BY n.created_at DESC LIMIT 100""", {"me": tok["uid"], "admin": tok["role"] == "admin"})
    return {"success": True, "data": rows}


def notification_delete(conn, data, query, token, notification_id):
    tok = require_role(token, ["faculty", "admin"])
    cur = conn.execute("DELETE FROM notifications WHERE id = %(id)s AND (sender_id = %(me)s OR %(admin)s)",
                       {"id": notification_id, "me": tok["uid"], "admin": tok["role"] == "admin"})
    if cur.rowcount == 0:
        raise ApiError(404, "Notification not found.")
    return {"success": True, "message": "Notification deleted."}


# =================================================================== faculty (create / update / delete)
def faculty_save(conn, data, query, token, faculty_id=None):
    require_role(token, ["admin"])
    first, last, email = _s(data, "first_name"), _s(data, "last_name"), _s(data, "email").lower()
    user_id = _i(data, "user_id") or None
    errors = [{"field": f, "message": m} for f, v, m in (("first_name", first, "First name is required."),
                                                         ("last_name", last, "Last name is required.")) if not v]
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    if user_id and not one(conn, "SELECT 1 FROM users WHERE id = %(u)s AND role = 'faculty'", {"u": user_id}):
        raise ApiError(404, "user_id must be an existing faculty account.")
    p = {"f": first, "l": last, "e": email or None, "u": user_id}
    if faculty_id is None:
        row = one(conn, "INSERT INTO faculty (first_name, last_name, email, user_id) VALUES (%(f)s, %(l)s, %(e)s, %(u)s) "
                        "RETURNING id, first_name, last_name, email, user_id", p)
        return {"success": True, "message": "Faculty member added successfully.", "faculty": row}
    row = one(conn, "UPDATE faculty SET first_name=%(f)s, last_name=%(l)s, email=%(e)s, user_id=%(u)s WHERE id=%(id)s "
                    "RETURNING id, first_name, last_name, email, user_id", {**p, "id": faculty_id})
    if not row:
        raise ApiError(404, "Faculty member not found.")
    return {"success": True, "message": "Faculty member updated successfully.", "faculty": row}


def faculty_delete(conn, data, query, token, faculty_id):
    require_role(token, ["admin"])
    try:
        with conn.transaction():
            conn.execute("DELETE FROM faculty_subjects WHERE faculty_id = %(id)s", {"id": faculty_id})
            cur = conn.execute("DELETE FROM faculty WHERE id = %(id)s", {"id": faculty_id})
    except psycopg.errors.ForeignKeyViolation:
        raise ApiError(409, "This faculty member still has classes in the schedule. Remove or reassign them first.")
    if cur.rowcount == 0:
        raise ApiError(404, "Faculty member not found.")
    return {"success": True, "message": "Faculty member deleted successfully."}


# =================================================================== faculty_subjects
FS_SELECT = """
    SELECT fs.faculty_id, f.first_name || ' ' || f.last_name AS faculty, fs.subject_id, sub.code AS subject_code,
           sub.name AS subject, fs.department_id, d.code AS department
    FROM faculty_subjects fs JOIN faculty f ON f.id = fs.faculty_id
    JOIN subjects sub ON sub.id = fs.subject_id JOIN departments d ON d.id = fs.department_id"""


def faculty_subjects_list(conn, data, query, token):
    require_login(token)
    where, params = ["TRUE"], {}
    for key in ("faculty_id", "subject_id", "department_id"):
        if _i(query, key):
            where.append(f"fs.{key} = %({key})s"); params[key] = _i(query, key)
    rows = all_rows(conn, f"{FS_SELECT} WHERE {' AND '.join(where)} ORDER BY f.last_name, sub.code, d.code", params)
    return {"success": True, "faculty_subjects": rows}


def faculty_subject_create(conn, data, query, token):
    require_role(token, ["admin"])
    keys = {k: _i(data, k) for k in ("faculty_id", "subject_id", "department_id")}
    errors = [{"field": k, "message": f"{k.replace('_id', '').title()} is required."} for k, v in keys.items() if v <= 0]
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    try:
        conn.execute("INSERT INTO faculty_subjects (faculty_id, subject_id, department_id) "
                     "VALUES (%(faculty_id)s, %(subject_id)s, %(department_id)s)", keys)
    except psycopg.errors.UniqueViolation:
        raise ApiError(409, "This faculty member already handles this subject for that program.")
    except psycopg.errors.ForeignKeyViolation:
        raise ApiError(404, "The faculty member, subject or department does not exist.")
    row = one(conn, f"{FS_SELECT} WHERE fs.faculty_id=%(faculty_id)s AND fs.subject_id=%(subject_id)s "
                    f"AND fs.department_id=%(department_id)s", keys)
    return {"success": True, "message": "Subject assigned to faculty member.", "faculty_subject": row}


def faculty_subject_delete(conn, data, query, token, faculty_id, subject_id, department_id):
    require_role(token, ["admin"])
    cur = conn.execute("DELETE FROM faculty_subjects WHERE faculty_id=%(f)s AND subject_id=%(s)s AND department_id=%(d)s",
                       {"f": faculty_id, "s": subject_id, "d": department_id})
    if cur.rowcount == 0:
        raise ApiError(404, "Assignment not found.")
    return {"success": True, "message": "Subject removed from faculty member."}


# =================================================================== section_rooms (get one)
def section_room_get(conn, data, query, token, section_id):
    require_login(token)
    row = one(conn, """SELECT s.id AS section_id, s.name AS section, r.id AS room_id, r.room_name AS room, r.building, r.floor
                       FROM section_rooms sr JOIN sections s ON s.id = sr.section_id JOIN rooms r ON r.id = sr.room_id
                       WHERE sr.section_id = %(s)s""", {"s": section_id})
    if not row:
        raise ApiError(404, "This section has no home room.")
    return {"success": True, "section_room": row}


# =================================================================== notifications (get one / update)
NOTE_SELECT = """
    SELECT n.id, n.kind, n.title, n.body, n.sender_id, n.sender_name, n.department_id, d.code AS program,
           n.section_id, COALESCE(sec.name, 'All sections') AS section,
           to_char(n.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS created_at,
           (SELECT COUNT(*) FROM notification_reads r WHERE r.notification_id = n.id) AS read_count
    FROM notifications n JOIN departments d ON d.id = n.department_id LEFT JOIN sections sec ON sec.id = n.section_id"""


def notification_get(conn, data, query, token, notification_id):
    tok = require_role(token, ["faculty", "admin", "student"])
    row = one(conn, f"{NOTE_SELECT} WHERE n.id = %(id)s", {"id": notification_id})
    if not row:
        raise ApiError(404, "Notification not found.")
    if tok["role"] == "student":
        me = one(conn, """SELECT u.section_id, COALESCE(u.department_id, s.department_id) AS department_id
                          FROM users u LEFT JOIN sections s ON s.id = u.section_id WHERE u.id = %(id)s""", {"id": tok["uid"]})
        if not me or me["department_id"] != row["department_id"] or (row["section_id"] and row["section_id"] != me["section_id"]):
            raise ApiError(404, "Notification not found.")
    return {"success": True, "notification": row}


def notification_update(conn, data, query, token, notification_id):
    tok = require_role(token, ["faculty", "admin"])
    title, body = _s(data, "title"), _s(data, "body")
    kind = data.get("kind")
    errors = [{"field": f, "message": m} for f, v, m in (("title", title, "Title is required."),
                                                         ("body", body, "Message is required.")) if not v]
    if errors:
        raise ApiError(422, REQUIRED_MSG, errors=errors)
    cur = conn.execute("""UPDATE notifications SET title=%(t)s, body=%(b)s, kind=COALESCE(%(k)s, kind)
                          WHERE id=%(id)s AND (sender_id=%(me)s OR %(admin)s)""",
                       {"t": title, "b": body, "k": kind if kind in ("transfer", "announcement") else None,
                        "id": notification_id, "me": tok["uid"], "admin": tok["role"] == "admin"})
    if cur.rowcount == 0:
        raise ApiError(404, "Notification not found.")
    return {"success": True, "message": "Notification updated.",
            "notification": one(conn, f"{NOTE_SELECT} WHERE n.id = %(id)s", {"id": notification_id})}


def notifications_all(conn, data, query, token):
    """Admin/faculty view of the notifications table (filter by program or section)."""
    require_role(token, ["faculty", "admin"])
    where, params = ["TRUE"], {}
    if _i(query, "department_id"):
        where.append("n.department_id = %(d)s"); params["d"] = _i(query, "department_id")
    if _i(query, "section_id"):
        where.append("n.section_id = %(s)s"); params["s"] = _i(query, "section_id")
    return {"success": True, "notifications": all_rows(conn, f"{NOTE_SELECT} WHERE {' AND '.join(where)} "
                                                              f"ORDER BY n.created_at DESC LIMIT 200", params)}


# =================================================================== notification_reads
def notification_reads_list(conn, data, query, token):
    require_role(token, ["faculty", "admin"])
    where, params = ["TRUE"], {}
    if _i(query, "notification_id"):
        where.append("r.notification_id = %(n)s"); params["n"] = _i(query, "notification_id")
    if _i(query, "user_id"):
        where.append("r.user_id = %(u)s"); params["u"] = _i(query, "user_id")
    rows = all_rows(conn, f"""
        SELECT r.notification_id, n.title, r.user_id, u.last_name || ', ' || u.first_name AS student, s.name AS section,
               to_char(r.read_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS read_at
        FROM notification_reads r JOIN notifications n ON n.id = r.notification_id
        JOIN users u ON u.id = r.user_id LEFT JOIN sections s ON s.id = u.section_id
        WHERE {' AND '.join(where)} ORDER BY r.read_at DESC LIMIT 500""", params)
    return {"success": True, "notification_reads": rows}


def notification_read_delete(conn, data, query, token, notification_id, user_id):
    """Marks a notification as unread again (removes the read record)."""
    tok = require_role(token, ["student", "faculty", "admin"])
    if tok["role"] == "student" and int(tok["uid"]) != user_id:
        raise ApiError(403, "You do not have permission to do this.")
    cur = conn.execute("DELETE FROM notification_reads WHERE notification_id=%(n)s AND user_id=%(u)s",
                       {"n": notification_id, "u": user_id})
    if cur.rowcount == 0:
        raise ApiError(404, "Read record not found.")
    return {"success": True, "message": "Notification marked as unread."}


# =================================================================== password_tokens (admin only, no secrets)
def password_tokens_list(conn, data, query, token):
    require_role(token, ["admin"])
    rows = all_rows(conn, """
        SELECT t.id, t.user_id, u.email, t.created_at, t.expires_at, t.used_at,
               CASE WHEN t.used_at IS NOT NULL THEN 'Used' WHEN t.expires_at < now() THEN 'Expired' ELSE 'Active' END AS status
        FROM password_tokens t JOIN users u ON u.id = t.user_id ORDER BY t.created_at DESC LIMIT 200""")
    return {"success": True, "password_tokens": rows}


def password_token_delete(conn, data, query, token, token_id):
    require_role(token, ["admin"])
    cur = conn.execute("DELETE FROM password_tokens WHERE id = %(id)s", {"id": token_id})
    if cur.rowcount == 0:
        raise ApiError(404, "Setup link not found.")
    return {"success": True, "message": "Setup link deleted."}


# =================================================================== Android app: signed-in user
def me(conn, data, query, token):
    """
    The signed-in student's or faculty member's details. For faculty it also
    lists the subjects they handle and the sections of each program, so the
    app needs no built-in faculty list.
    """
    tok = require_role(token, ["student", "faculty"])
    u = one(conn, """
        SELECT u.id, u.username, u.email, u.first_name, u.last_name, u.student_id, u.employee_id, u.contact_number,
               u.role, u.section_id, s.name AS section_name, d.code AS program
        FROM users u LEFT JOIN sections s ON s.id = u.section_id
        LEFT JOIN departments d ON d.id = COALESCE(u.department_id, s.department_id)
        WHERE u.id = %(id)s""", {"id": tok["uid"]})
    if not u:
        raise ApiError(401, "Your session has expired. Please log in again.")
    out = {"success": True, "user": u}
    if u["role"] == "faculty":
        f = one(conn, "SELECT id FROM faculty WHERE user_id = %(u)s ORDER BY id LIMIT 1", {"u": u["id"]})
        out["faculty_id"] = f["id"] if f else None
        # subjects assigned to them, plus the subjects of the classes they teach
        out["subjects"] = all_rows(conn, """
            SELECT code, name, program FROM (
                SELECT sub.code, sub.name, d.code AS program
                FROM faculty_subjects fs JOIN subjects sub ON sub.id = fs.subject_id
                JOIN departments d ON d.id = fs.department_id WHERE fs.faculty_id = %(f)s
                UNION
                SELECT sub.code, sub.name, d.code
                FROM schedules sc JOIN subjects sub ON sub.id = sc.subject_id
                JOIN sections sec ON sec.id = sc.section_id JOIN departments d ON d.id = sec.department_id
                WHERE sc.faculty_id = %(f)s) x
            ORDER BY code, program""", {"f": f["id"] if f else 0})
    out["sections"] = all_rows(conn, """
        SELECT s.name, d.code AS program FROM sections s JOIN departments d ON d.id = s.department_id
        ORDER BY d.code, s.name""")
    return out


def my_schedule(conn, data, query, token):
    """
    Weekly classes of the signed-in user: a student's section, or every class a
    faculty member teaches (in all programs).
    """
    tok = require_role(token, ["student", "faculty"])
    if tok["role"] == "student":
        where = "sc.section_id = (SELECT section_id FROM users WHERE id = %(u)s)"
    else:
        where = "sc.faculty_id IN (SELECT id FROM faculty WHERE user_id = %(u)s)"
    rows = all_rows(conn, f"""
        SELECT sc.id, sub.code AS subject_code, sub.name AS subject_name, sc.day_of_week,
               TO_CHAR(sc.start_time, 'HH24:MI') AS start_time, TO_CHAR(sc.end_time, 'HH24:MI') AS end_time,
               r.room_name, r.building, r.floor, sc.faculty_id,
               COALESCE(f.first_name || ' ' || f.last_name, 'TBA') AS faculty_name,
               sec.name AS section_name, d.code AS program
        FROM schedules sc JOIN subjects sub ON sub.id = sc.subject_id JOIN rooms r ON r.id = sc.room_id
        LEFT JOIN faculty f ON f.id = sc.faculty_id JOIN sections sec ON sec.id = sc.section_id
        JOIN departments d ON d.id = sec.department_id
        WHERE {where}
        ORDER BY {DAY_ORDER}, sc.start_time""", {"u": tok["uid"]})
    return {"success": True, "data": rows}


def rooms_today(conn, data, query, token):
    """
    Every room with the classes held in it on one day (default: today in the
    Philippines), so the app can show live availability.
    """
    require_login(token)
    day = _s(query, "day").lower().capitalize()
    if day not in DAYS:
        day = scalar(conn, "SELECT trim(to_char(now() AT TIME ZONE 'Asia/Manila', 'FMDay'))")
    rooms = all_rows(conn, f"SELECT {ROOM_COLS} FROM rooms ORDER BY floor DESC, room_name")
    classes = all_rows(conn, """
        SELECT sc.id, sc.room_id, r.room_name, sub.code AS subject_code, sub.name AS subject_name,
               TO_CHAR(sc.start_time, 'HH24:MI') AS start_time, TO_CHAR(sc.end_time, 'HH24:MI') AS end_time,
               sec.name AS section_name, d.code AS program,
               COALESCE(f.first_name || ' ' || f.last_name, 'TBA') AS faculty_name
        FROM schedules sc JOIN rooms r ON r.id = sc.room_id JOIN subjects sub ON sub.id = sc.subject_id
        JOIN sections sec ON sec.id = sc.section_id JOIN departments d ON d.id = sec.department_id
        LEFT JOIN faculty f ON f.id = sc.faculty_id
        WHERE sc.day_of_week = %(day)s ORDER BY sc.start_time""", {"day": day})
    return {"success": True, "day": day, "rooms": rooms, "classes": classes}
