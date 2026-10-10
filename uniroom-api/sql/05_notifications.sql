-- =====================================================
-- 05 – Notifications from faculty to students
-- =====================================================
-- A faculty member sends a room change or an announcement
-- from the Android app to one section (or the whole course).
-- Students of that course/section see it in their alerts.
-- Safe to run more than once.

CREATE TABLE IF NOT EXISTS notifications (
    id             SERIAL PRIMARY KEY,
    sender_id      INTEGER REFERENCES users(id) ON DELETE SET NULL,
    sender_name    TEXT NOT NULL,
    department_id  INTEGER NOT NULL REFERENCES departments(id),
    section_id     INTEGER REFERENCES sections(id),      -- NULL = every section of the course
    kind           TEXT NOT NULL DEFAULT 'announcement'
                   CHECK (kind IN ('transfer', 'announcement')),
    title          TEXT NOT NULL,
    body           TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Which student has read which notification
CREATE TABLE IF NOT EXISTS notification_reads (
    notification_id  INTEGER NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id          INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (notification_id, user_id)
);

CREATE INDEX IF NOT EXISTS notifications_target_idx
    ON notifications (department_id, section_id, created_at DESC);

-- Same protection as the other tables: only the API (database owner) can read them.
ALTER TABLE notifications       ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_reads  ENABLE ROW LEVEL SECURITY;
