"""
Shared pieces of the UNIROOM API: settings, database connection, errors,
login tokens and passwords.
"""
import base64
import hashlib
import hmac
import json
import os
import secrets
import time
from contextlib import contextmanager
from pathlib import Path

import psycopg
from psycopg.rows import dict_row


# ---------------------------------------------------------------- settings
def _load_dotenv():
    """Reads uniroom-fastapi/.env for local testing (Vercel uses its own variables)."""
    env_file = Path(__file__).resolve().parent.parent / ".env"
    if not env_file.is_file():
        return
    for line in env_file.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = (part.strip() for part in line.split("=", 1))
        os.environ.setdefault(key, value.strip("\"'"))


_load_dotenv()


def env(key, default=None):
    value = os.environ.get(key)
    return default if value in (None, "") else value


class Settings:
    db_host = env("DB_HOST", "localhost")
    db_port = env("DB_PORT", "5432")
    db_name = env("DB_NAME", "postgres")
    db_user = env("DB_USER", "postgres")
    db_password = env("DB_PASSWORD", "")
    db_sslmode = env("DB_SSLMODE", "prefer")          # "require" for Supabase
    pgcrypto_schema = env("PGCRYPTO_SCHEMA", "extensions")
    app_secret = env("APP_SECRET", "change-this-local-dev-secret")
    admin_web_url = env("ADMIN_WEB_URL", "http://localhost:5500").rstrip("/")
    allowed_origins = env("ALLOWED_ORIGINS", "*")
    smtp_host = env("SMTP_HOST", "smtp.gmail.com")
    smtp_port = int(env("SMTP_PORT", "465"))
    smtp_secure = env("SMTP_SECURE", "ssl")
    smtp_user = env("SMTP_USER", "")
    smtp_pass = env("SMTP_PASS", "")
    mail_from = env("MAIL_FROM", env("SMTP_USER", ""))
    mail_from_name = env("MAIL_FROM_NAME", "UNIROOM")
    setup_link_hours = int(env("SETUP_LINK_HOURS", "48"))


settings = Settings()


# ---------------------------------------------------------------- errors
class ApiError(Exception):
    """Stops the request and sends {success: false, ...} with an HTTP status."""

    def __init__(self, status, message, **extra):
        super().__init__(message)
        self.status = status
        self.payload = {"success": False, "message": message, **extra}


# ---------------------------------------------------------------- database
def _db_hint(raw):
    low = raw.lower()
    if "password authentication failed" in low:
        return "Wrong DB_PASSWORD. Use the Supabase database password (or reset it in Supabase → Project Settings → Database)."
    if "tenant or user not found" in low:
        return "Wrong DB_USER or DB_HOST. Copy both again from Supabase → Connect → Transaction pooler."
    if "could not translate host name" in low or "name or service not known" in low:
        return "Wrong DB_HOST. Copy it again from Supabase → Connect → Transaction pooler."
    if "timeout" in low or "timed out" in low:
        return "The database did not answer. Check DB_HOST and DB_PORT (6543), and that the Supabase project is not paused."
    if not settings.db_password:
        return "DB_PASSWORD is empty. Add your Supabase database password in Vercel and redeploy."
    return "Check DB_HOST, DB_PORT, DB_USER and DB_PASSWORD in Vercel, then redeploy."


@contextmanager
def connect():
    """One connection per request (serverless). Autocommit; use conn.transaction() for multi-step writes."""
    try:
        conn = psycopg.connect(
            host=settings.db_host, port=settings.db_port, dbname=settings.db_name,
            user=settings.db_user, password=settings.db_password, sslmode=settings.db_sslmode,
            row_factory=dict_row, autocommit=True,
            prepare_threshold=None,          # Supabase transaction pooler: no prepared statements
            connect_timeout=15,
        )
    except psycopg.OperationalError as e:
        raw = str(e)
        if settings.db_password:
            raw = raw.replace(settings.db_password, "***")
        raise ApiError(500, "Database connection failed.", reason=_db_hint(raw), details=raw,
                       using={"host": settings.db_host, "port": settings.db_port,
                              "user": settings.db_user, "dbname": settings.db_name})
    try:
        yield conn
    finally:
        conn.close()


def one(conn, sql, params=None):
    return conn.execute(sql, params or {}).fetchone()


def all_rows(conn, sql, params=None):
    return conn.execute(sql, params or {}).fetchall()


def scalar(conn, sql, params=None):
    row = conn.execute(sql, params or {}).fetchone()
    return None if row is None else next(iter(row.values()))


# ---------------------------------------------------------------- tokens
def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _b64url_decode(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def make_token(user, hours=8):
    """Same format as the old PHP API, so existing logins keep working."""
    payload = _b64url(json.dumps({"uid": int(user["id"]), "role": user["role"],
                                  "exp": int(time.time()) + hours * 3600},
                                 separators=(",", ":")).encode())
    sig = _b64url(hmac.new(settings.app_secret.encode(), payload.encode(), hashlib.sha256).digest())
    return f"{payload}.{sig}"


def read_token(authorization):
    if not authorization or not authorization.lower().startswith("bearer "):
        return None
    parts = authorization.split(None, 1)[1].strip().split(".")
    if len(parts) != 2:
        return None
    payload, sig = parts
    expected = _b64url(hmac.new(settings.app_secret.encode(), payload.encode(), hashlib.sha256).digest())
    if not hmac.compare_digest(expected, sig):
        return None
    try:
        data = json.loads(_b64url_decode(payload))
    except Exception:
        return None
    if not isinstance(data, dict) or data.get("exp", 0) < time.time():
        return None
    return data


def require_role(token, roles):
    if not token:
        raise ApiError(401, "Your session has expired. Please log in again.")
    if token.get("role") not in roles:
        raise ApiError(403, "You do not have permission to do this.")
    return token


# ---------------------------------------------------------------- passwords
# bcrypt through PostgreSQL's pgcrypto (Supabase has it in the "extensions"
# schema). Works with the $2y$ hashes the PHP version saved.
def _crypt():
    s = settings.pgcrypto_schema
    return (f"{s}.crypt", f"{s}.gen_salt") if s else ("crypt", "gen_salt")


def hash_password(conn, password):
    crypt, gen_salt = _crypt()
    return scalar(conn, f"SELECT {crypt}(%(p)s, {gen_salt}('bf', 10)) AS h", {"p": password})


def verify_password(conn, password, stored):
    if not stored or not stored.startswith("$2"):
        return False
    normalized = "$2a$" + stored[4:]           # $2y$ / $2b$ use the same algorithm
    crypt, _ = _crypt()
    return bool(scalar(conn, f"SELECT {crypt}(%(p)s, %(h)s) = %(h)s AS ok", {"p": password, "h": normalized}))


def random_password(length=10):
    chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
    return "".join(secrets.choice(chars) for _ in range(length))


def password_problem(pw):
    if len(pw) < 8:
        return "Password must be at least 8 characters."
    if not any(c.isalpha() for c in pw) or not any(c.isdigit() for c in pw):
        return "Password must contain both letters and numbers."
    return None
