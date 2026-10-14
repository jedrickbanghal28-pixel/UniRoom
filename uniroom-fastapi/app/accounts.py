"""
Account creation used by the admin website: validation, duplicate checks,
username, temporary password / setup link, and the account email.
"""
import hashlib
import html
import re
import secrets
import smtplib
import ssl
from email.message import EmailMessage
from email.utils import formataddr

from .core import ApiError, all_rows, hash_password, one, random_password, scalar, settings

GENDERS = ["Male", "Female", "Prefer not to say"]
ROLES = ["student", "faculty"]
MIN_AGE, MAX_AGE = 15, 100
EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


# ---------------------------------------------------------------- mail
class Mailer:
    def __init__(self):
        self.last_error = None
        self._smtp = None

    @staticmethod
    def is_configured():
        return bool(settings.smtp_host and settings.smtp_user and settings.smtp_pass)

    def _connect(self):
        if self._smtp:
            return self._smtp
        ctx = ssl.create_default_context()
        if settings.smtp_secure == "ssl":
            smtp = smtplib.SMTP_SSL(settings.smtp_host, settings.smtp_port, timeout=20, context=ctx)
        else:
            smtp = smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=20)
            if settings.smtp_secure == "tls":
                smtp.starttls(context=ctx)
        smtp.login(settings.smtp_user, settings.smtp_pass)
        self._smtp = smtp
        return smtp

    def send(self, to_email, to_name, subject, html_body, text_body):
        if not self.is_configured():
            self.last_error = "Email is not set up yet (SMTP_USER / SMTP_PASS are empty)."
            return False
        msg = EmailMessage()
        msg["From"] = formataddr((settings.mail_from_name, settings.mail_from or settings.smtp_user))
        msg["To"] = formataddr((to_name, to_email))
        msg["Subject"] = subject
        msg.set_content(text_body)
        msg.add_alternative(html_body, subtype="html")
        try:
            self._connect().send_message(msg)
            return True
        except smtplib.SMTPAuthenticationError:
            self.last_error = "Gmail rejected the login. Use a Google App Password in SMTP_PASS."
        except Exception as e:  # network, TLS, ...
            self.last_error = f"Could not send the email ({e.__class__.__name__})."
        self.close()
        return False

    def close(self):
        if self._smtp:
            try:
                self._smtp.quit()
            except Exception:
                pass
            self._smtp = None


# ---------------------------------------------------------------- lookups
def load_lookup(conn):
    return {
        "departments": all_rows(conn, "SELECT * FROM departments ORDER BY code"),
        "sections": all_rows(conn, "SELECT id, name, department_id FROM sections ORDER BY name"),
    }


def department_label(d):
    return f"{d['code']} – {d['name']}" if d.get("name") else d["code"]


def match_department(lookup, value):
    v = str(value or "").strip().lower()
    if not v:
        return None
    for d in lookup["departments"]:
        if (str(d["id"]) == v or d["code"].lower() == v or (d.get("name") and d["name"].lower() == v)
                or department_label(d).lower() == v):
            return d
    return None


def match_section(lookup, value):
    v = str(value or "").strip().lower()
    if not v:
        return None
    for s in lookup["sections"]:
        if str(s["id"]) == v or s["name"].lower() == v:
            return s
    return None


def normalize_gender(value):
    return {"m": "Male", "male": "Male", "f": "Female", "female": "Female",
            "prefer not to say": "Prefer not to say", "n/a": "Prefer not to say"}.get(str(value or "").strip().lower())


# ---------------------------------------------------------------- validation
def validate_account(raw, lookup):
    """Checks one account (form or CSV row). Returns (clean_account, errors)."""
    errors = []
    get = lambda k: str(raw.get(k) if raw.get(k) is not None else "").strip()
    err = lambda field, msg: errors.append({"field": field, "message": msg})

    a = {"first_name": get("first_name"), "last_name": get("last_name"), "email": get("email").lower(),
         "gender": None, "age": None, "role": get("role").lower(), "department_id": None, "section_id": None,
         "student_id": get("student_id"), "employee_id": get("employee_id"), "contact_number": get("contact_number")}

    if not a["first_name"]:
        err("first_name", "First name is required.")
    if not a["last_name"]:
        err("last_name", "Last name is required.")
    if not a["email"]:
        err("email", "Email address is required.")
    elif not EMAIL_RE.match(a["email"]):
        err("email", "Email address is not valid.")

    gender = get("gender")
    if not gender:
        err("gender", "Gender is required.")
    elif not (a.__setitem__("gender", normalize_gender(gender)) or a["gender"]):
        err("gender", "Gender must be Male, Female or Prefer not to say.")

    age = get("age")
    if not age:
        err("age", "Age is required.")
    elif not age.isdigit() or not MIN_AGE <= int(age) <= MAX_AGE:
        err("age", f"Age must be a whole number from {MIN_AGE} to {MAX_AGE}.")
    else:
        a["age"] = int(age)

    if not a["role"]:
        err("role", "Role is required.")
    elif a["role"] not in ROLES:
        err("role", "Role must be Student or Faculty.")

    dept_value = get("department_id") or get("department")
    dept = None
    if not dept_value:
        err("department", "Department is required.")
    elif not (dept := match_department(lookup, dept_value)):
        err("department", f'Department "{dept_value}" does not exist.')
    else:
        a["department_id"] = int(dept["id"])

    if a["role"] == "student":
        sec_value = get("section_id") or get("section")
        if not sec_value:
            err("section", "Section is required for students.")
        elif not (sec := match_section(lookup, sec_value)):
            err("section", f'Section "{sec_value}" does not exist.')
        elif dept and int(sec["department_id"]) != int(dept["id"]):
            err("section", f"Section {sec['name']} is not part of {dept['code']}.")
        else:
            a["section_id"] = int(sec["id"])
        if not a["student_id"]:
            err("student_id", "Student ID is required for students.")
        a["employee_id"] = ""
    else:
        a["student_id"] = ""
    return a, errors


def find_duplicates(conn, a, exclude_id=None):
    errors = []
    ex = {"x": exclude_id or 0}
    if a.get("email") and one(conn, "SELECT 1 FROM users WHERE lower(email) = lower(%(e)s) AND id <> %(x)s LIMIT 1",
                              {"e": a["email"], **ex}):
        errors.append({"field": "email", "message": "Email address is already registered."})
    if a.get("student_id") and one(conn, "SELECT 1 FROM users WHERE student_id = %(s)s AND id <> %(x)s LIMIT 1",
                                   {"s": a["student_id"], **ex}):
        errors.append({"field": "student_id", "message": "Student ID is already registered."})
    return errors


def unique_username(conn, email):
    local = email.split("@")[0]
    # "school.inbox+juan.cruz@gmail.com" (Gmail plus address) → username "juan.cruz"
    if "+" in local and local.split("+", 1)[1]:
        local = local.split("+", 1)[1]
    base = re.sub(r"[^a-z0-9._]", "", local.lower())[:40] or "user"
    for i in range(1, 1000):
        candidate = base if i == 1 else f"{base}{i}"
        if not one(conn, "SELECT 1 FROM users WHERE lower(username) = %(u)s LIMIT 1", {"u": candidate}):
            return candidate
    return base + secrets.token_hex(3)


def new_setup_link(conn, user_id):
    conn.execute("UPDATE password_tokens SET used_at = now() WHERE user_id = %(u)s AND used_at IS NULL", {"u": user_id})
    token = secrets.token_hex(32)
    conn.execute("INSERT INTO password_tokens (user_id, token_hash, expires_at) "
                 "VALUES (%(u)s, %(h)s, now() + make_interval(hours => %(hours)s))",
                 {"u": user_id, "h": hashlib.sha256(token.encode()).hexdigest(), "hours": settings.setup_link_hours})
    return f"{settings.admin_web_url}/set-password.html?token={token}"


def create_account(conn, a, mode, admin_id):
    """Saves the account. mode: 'temporary' (temporary password) or 'link' (setup link)."""
    with conn.transaction():
        username = unique_username(conn, a["email"])
        temporary = random_password(10) if mode == "temporary" else None
        pw_hash = hash_password(conn, temporary or secrets.token_hex(24))
        user_id = scalar(conn, """
            INSERT INTO users (first_name, last_name, student_id, username, email, contact_number,
                               password_hash, role, section_id, gender, age, department_id,
                               employee_id, must_change_password, created_by)
            VALUES (%(first_name)s, %(last_name)s, %(student_id)s, %(username)s, %(email)s, %(contact_number)s,
                    %(password_hash)s, %(role)s, %(section_id)s, %(gender)s, %(age)s, %(department_id)s,
                    %(employee_id)s, TRUE, %(created_by)s)
            RETURNING id""", {
            **a, "username": username, "password_hash": pw_hash,
            "student_id": a["student_id"] or None, "contact_number": a["contact_number"] or None,
            "employee_id": a["employee_id"] or None, "created_by": admin_id or None})
        if a["role"] == "faculty":
            try:
                with conn.transaction():          # savepoint: never block the account itself
                    conn.execute("INSERT INTO faculty (first_name, last_name, user_id, email) VALUES (%(f)s, %(l)s, %(u)s, %(e)s)",
                                 {"f": a["first_name"], "l": a["last_name"], "u": user_id, "e": a["email"]})
            except Exception:
                pass
        setup_link = new_setup_link(conn, user_id) if mode != "temporary" else None
    user = {**a, "id": user_id, "username": username}
    return {"user": user, "temporary_password": temporary, "setup_link": setup_link}


# ---------------------------------------------------------------- email
def send_account_email(mailer, user, lookup, temporary_password, setup_link):
    e = lambda s: html.escape(str(s if s is not None else ""))
    dept = next((d for d in lookup["departments"] if user.get("department_id") and int(d["id"]) == int(user["department_id"])), None)
    section = next((s for s in lookup["sections"] if user.get("section_id") and int(s["id"]) == int(user["section_id"])), None)
    details = {"Name": f"{user['first_name']} {user['last_name']}", "Email": user["email"], "Username": user["username"],
               "Role": user["role"].capitalize(), "Department": department_label(dept) if dept else ""}
    if user["role"] == "student":
        details["Section"] = section["name"] if section else ""
        details["Student ID"] = user.get("student_id") or ""
    elif user.get("employee_id"):
        details["Employee ID"] = user["employee_id"]

    rows_html = "".join(f'<tr><td style="padding:6px 12px 6px 0;color:#777">{e(k)}</td>'
                        f'<td style="padding:6px 0;font-weight:bold">{e(v)}</td></tr>' for k, v in details.items())
    rows_text = "".join(f"  {k}: {v}\n" for k, v in details.items())
    hours = settings.setup_link_hours
    if setup_link:
        action_html = (f"<p>To start using your account, set your own password using the button below. "
                       f"This link works once and expires in {hours} hours.</p>"
                       f'<p style="margin:24px 0"><a href="{e(setup_link)}" style="background:#800020;color:#fff;'
                       f'padding:12px 22px;border-radius:6px;text-decoration:none;font-weight:bold">Set my password</a></p>'
                       f'<p style="font-size:13px;color:#777">If the button does not work, copy this link into your browser:<br>{e(setup_link)}</p>')
        action_text = f"Set your password (link works once, expires in {hours} hours):\n{setup_link}\n"
    else:
        where = "the UNIROOM mobile app" if user["role"] == "student" else "UNIROOM"
        action_html = (f"<p>Sign in to {where} with your username <strong>{e(user['username'])}</strong> "
                       f"(or your email) and this temporary password:</p>"
                       f'<p style="font-size:20px;font-weight:bold;letter-spacing:1px;background:#f8e9ed;padding:10px 14px;'
                       f'border-radius:6px;display:inline-block">{e(temporary_password)}</p>'
                       f"<p><strong>You will be asked to change this password the first time you log in.</strong></p>")
        action_text = (f"Sign in to {where} with your username ({user['username']}) or email.\n"
                       f"Temporary password: {temporary_password}\nYou will be asked to change it the first time you log in.\n")
    body_html = ('<div style="font-family:Arial,Helvetica,sans-serif;color:#202124;max-width:560px">'
                 '<h2 style="color:#800020;margin-bottom:4px">UNIROOM</h2>'
                 '<p style="color:#777;margin-top:0">University Room Management System</p>'
                 f"<p>Hello {e(user['first_name'])},</p>"
                 "<p>An administrator has created your UNIROOM account. Here are your account details:</p>"
                 f'<table style="border-collapse:collapse;margin:12px 0">{rows_html}</table>{action_html}'
                 '<p style="font-size:13px;color:#777;margin-top:28px">Never share your password with anyone. '
                 "If you did not expect this email, please contact your school administrator.</p></div>")
    body_text = (f"Hello {user['first_name']},\n\nAn administrator has created your UNIROOM account.\n\n"
                 f"Account details:\n{rows_text}\n{action_text}\n"
                 "Never share your password with anyone. If you did not expect this email, contact your school administrator.\n")
    ok = mailer.send(user["email"], details["Name"], "Your UNIROOM account has been created", body_html, body_text)
    return ok, (None if ok else mailer.last_error)
