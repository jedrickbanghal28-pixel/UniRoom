-- =====================================================================
-- 10_review_fixes.sql
-- Database rules added after the documentation review.
--
-- Before this file, some rules were only checked by the API. This file
-- adds them to the database too, so wrong data cannot be saved even if
-- someone writes to the tables directly.
--
-- Safe to run more than once. Run it in the Supabase SQL Editor after
-- files 01 to 09. If old rows break a rule, that rule is skipped and a
-- NOTICE says which one, so you can fix the rows and run it again.
-- =====================================================================

-- 1. New accounts must change their password on the first sign-in.
--    The API already saves TRUE; now the database default says the same.
ALTER TABLE users ALTER COLUMN must_change_password SET DEFAULT TRUE;

-- 1b. Middle initial (optional) for accounts and faculty records.
ALTER TABLE users   ADD COLUMN IF NOT EXISTS middle_initial TEXT;
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS middle_initial TEXT;

-- 2. created_by points to the admin account that created the user.
DO $$
BEGIN
    ALTER TABLE users ADD CONSTRAINT users_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN foreign_key_violation THEN
        RAISE NOTICE 'users.created_by has ids that do not exist – foreign key not added.';
END $$;

-- 3. Classes run Monday to Saturday.
DO $$
BEGIN
    ALTER TABLE schedules ADD CONSTRAINT schedules_day_check
        CHECK (day_of_week IN ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN check_violation THEN
        RAISE NOTICE 'Some schedules are not Monday to Saturday – day check not added.';
END $$;

-- 4. A class must end after it starts.
DO $$
BEGIN
    ALTER TABLE schedules ADD CONSTRAINT schedules_time_check CHECK (end_time > start_time);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN check_violation THEN
        RAISE NOTICE 'Some schedules end before they start – time check not added.';
END $$;

-- 5. Room status values (same list as the website and the API).
DO $$
BEGIN
    ALTER TABLE rooms ADD CONSTRAINT rooms_status_check
        CHECK (status IN ('Available', 'Occupied', 'Reserved', 'Maintenance'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN check_violation THEN
        RAISE NOTICE 'Some rooms have another status – status check not added.';
END $$;

-- 6. Notification title up to 150 characters, message up to 500.
DO $$
BEGIN
    ALTER TABLE notifications ADD CONSTRAINT notifications_length_check
        CHECK (char_length(title) <= 150 AND char_length(body) <= 500);
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN check_violation THEN
        RAISE NOTICE 'Some notifications are too long – length check not added.';
END $$;

-- 7. Subject codes. Fix the subject title, then make the code unique.
--    In the sample data ITE 031 was used for two subjects. Give
--    Data Structures and Algorithms its real code first, for example:
--      UPDATE subjects SET code = '<real code>' WHERE name = 'Data Structures and Algorithms';
UPDATE subjects SET name = 'Data Structures and Algorithms' WHERE name = 'Data Structure and Algorithms';

DO $$
BEGIN
    ALTER TABLE subjects ADD CONSTRAINT subjects_code_key UNIQUE (code);
EXCEPTION
    WHEN duplicate_object OR duplicate_table THEN NULL;
    WHEN unique_violation THEN
        RAISE NOTICE 'Two subjects share the same code – give each subject its own code, then run this file again.';
END $$;
