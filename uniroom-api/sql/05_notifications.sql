-- =====================================================
-- 05 – Notifications from faculty to students
-- =====================================================
-- A faculty member sends a room change or an announcement
-- from the Android app to one section (or the whole course).
-- Students of that course/section see it in their alerts.
--
-- Safe to run more than once. If your database already has an
-- older "notifications" table with different columns, it is kept
-- (renamed to notifications_old) and a new one is created.

-- 1. Move an older, different "notifications" table out of the way
DO $$
BEGIN
    IF to_regclass('public.notifications') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'notifications'
                         AND column_name = 'sender_name') THEN
        IF to_regclass('public.notifications_old') IS NOT NULL THEN
            EXECUTE 'ALTER TABLE public.notifications RENAME TO notifications_old_' || to_char(now(), 'YYYYMMDDHH24MISS');
        ELSE
            ALTER TABLE public.notifications RENAME TO notifications_old;
        END IF;
    END IF;

    IF to_regclass('public.notification_reads') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'notification_reads'
                         AND column_name = 'notification_id')
       OR to_regclass('public.notification_reads') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'notification_reads'
                         AND column_name = 'read_at') THEN
        EXECUTE 'ALTER TABLE public.notification_reads RENAME TO notification_reads_old_' || to_char(now(), 'YYYYMMDDHH24MISS');
    END IF;
END $$;

-- 2. The notifications (names chosen so they never clash with older objects)
CREATE SEQUENCE IF NOT EXISTS uniroom_notification_id_seq;

CREATE TABLE IF NOT EXISTS public.notifications (
    id             INTEGER NOT NULL DEFAULT nextval('uniroom_notification_id_seq'),
    sender_id      INTEGER,
    sender_name    TEXT NOT NULL,
    department_id  INTEGER NOT NULL,
    section_id     INTEGER,                    -- NULL = every section of the course
    kind           TEXT NOT NULL DEFAULT 'announcement',
    title          TEXT NOT NULL,
    body           TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uniroom_notifications_pk      PRIMARY KEY (id),
    CONSTRAINT uniroom_notifications_kind    CHECK (kind IN ('transfer', 'announcement')),
    CONSTRAINT uniroom_notifications_sender  FOREIGN KEY (sender_id)     REFERENCES public.users(id) ON DELETE SET NULL,
    CONSTRAINT uniroom_notifications_dept    FOREIGN KEY (department_id) REFERENCES public.departments(id),
    CONSTRAINT uniroom_notifications_section FOREIGN KEY (section_id)    REFERENCES public.sections(id)
);

ALTER SEQUENCE uniroom_notification_id_seq OWNED BY public.notifications.id;

-- 3. Which student has read which notification
CREATE TABLE IF NOT EXISTS public.notification_reads (
    notification_id  INTEGER NOT NULL,
    user_id          INTEGER NOT NULL,
    read_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uniroom_notification_reads_pk   PRIMARY KEY (notification_id, user_id),
    CONSTRAINT uniroom_notification_reads_note FOREIGN KEY (notification_id) REFERENCES public.notifications(id) ON DELETE CASCADE,
    CONSTRAINT uniroom_notification_reads_user FOREIGN KEY (user_id)         REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS uniroom_notifications_target_idx
    ON public.notifications (department_id, section_id, created_at DESC);

-- 4. Same protection as the other tables: only the API (database owner) can read them.
ALTER TABLE public.notifications       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_reads  ENABLE ROW LEVEL SECURITY;

-- Check: should list the columns id, sender_id, sender_name, department_id, section_id, kind, title, body, created_at
SELECT column_name FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'notifications'
ORDER BY ordinal_position;
