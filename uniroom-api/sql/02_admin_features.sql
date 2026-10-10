-- =====================================================================
-- UNIROOM – admin features
--   * required user information (gender, age, department, employee ID)
--   * email notification + password setup link / temporary password
--   * first-login password change
--   * faster schedule conflict checks
-- =====================================================================
-- Safe to run more than once. Supabase: SQL Editor → paste → Run.
-- =====================================================================

-- ---------- 1. Extra user information ----------
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender                TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS age                   SMALLINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS department_id         INTEGER REFERENCES departments(id);
ALTER TABLE users ADD COLUMN IF NOT EXISTS employee_id           TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password  BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at            TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by            INTEGER;

DO $$
BEGIN
    ALTER TABLE users ADD CONSTRAINT users_gender_check
        CHECK (gender IS NULL OR gender IN ('Male', 'Female', 'Prefer not to say'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE users ADD CONSTRAINT users_age_check
        CHECK (age IS NULL OR age BETWEEN 15 AND 100);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Existing students/faculty: copy the department from their section.
UPDATE users u
SET    department_id = s.department_id
FROM   sections s
WHERE  s.id = u.section_id
  AND  u.department_id IS NULL;

-- No two accounts may share an email (ignoring upper/lower case).
-- If this fails, you already have duplicate emails – fix them first.
DO $$
BEGIN
    CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_key ON users (lower(email));
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'Duplicate emails exist in users – unique email index was not created.';
END $$;

-- ---------- 2. Link faculty accounts to the faculty list used by schedules ----------
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS user_id INTEGER REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS email   TEXT;

-- ---------- 2b. Which subjects each faculty member handles, per department ----------
CREATE TABLE IF NOT EXISTS faculty_subjects (
    faculty_id     INTEGER NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    subject_id     INTEGER NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    department_id  INTEGER NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    PRIMARY KEY (faculty_id, subject_id, department_id)
);

-- ---------- 3. Password setup links ----------
CREATE TABLE IF NOT EXISTS password_tokens (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,      -- SHA-256 of the token in the email link
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS password_tokens_user_idx ON password_tokens (user_id);

-- ---------- 4. Faster schedule conflict checks ----------
CREATE INDEX IF NOT EXISTS schedules_room_day_idx    ON schedules (room_id, day_of_week);
CREATE INDEX IF NOT EXISTS schedules_faculty_day_idx ON schedules (faculty_id, day_of_week);
CREATE INDEX IF NOT EXISTS schedules_section_day_idx ON schedules (section_id, day_of_week);

-- ---------- 5. Keep the tables private ----------
-- The PHP API connects as the "postgres" user, which is not affected by
-- Row Level Security. Turning RLS on (with no policies) stops anyone who
-- has your public "anon" key from reading tables such as users
-- (which contains password hashes) through Supabase's REST API.
ALTER TABLE departments     ENABLE ROW LEVEL SECURITY;
ALTER TABLE sections        ENABLE ROW LEVEL SECURITY;
ALTER TABLE users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE subjects        ENABLE ROW LEVEL SECURITY;
ALTER TABLE faculty         ENABLE ROW LEVEL SECURITY;
ALTER TABLE rooms           ENABLE ROW LEVEL SECURITY;
ALTER TABLE schedules       ENABLE ROW LEVEL SECURITY;
ALTER TABLE section_rooms   ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE faculty_subjects ENABLE ROW LEVEL SECURITY;
