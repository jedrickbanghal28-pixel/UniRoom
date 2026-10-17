-- =====================================================================
-- UNIROOM – clear all accounts and activity before the live demo
--
-- DELETES (cannot be undone):
--   * every student and faculty account (admin accounts are kept)
--   * faculty records, their subjects and every class schedule
--   * all notifications, read records and password setup links
--
-- KEEPS: admin account(s), departments, sections, rooms, subjects,
--        section home rooms.
--
-- After running it, upload uniroom_live_demo_accounts.csv on the admin
-- website (Account Creation → Bulk upload) and add the classes again in
-- Schedule. Run in Supabase → SQL Editor.
-- =====================================================================

BEGIN;

DELETE FROM notification_reads;
DELETE FROM notifications;
DELETE FROM password_tokens
 WHERE user_id IN (SELECT id FROM users WHERE role <> 'admin');
DELETE FROM schedules;
DELETE FROM faculty_subjects;
DELETE FROM faculty;
DELETE FROM users WHERE role <> 'admin';

COMMIT;

-- Check: only the admin should be left
SELECT role, COUNT(*) AS accounts FROM users GROUP BY role ORDER BY role;
SELECT (SELECT COUNT(*) FROM departments) AS departments, (SELECT COUNT(*) FROM sections) AS sections,
       (SELECT COUNT(*) FROM rooms) AS rooms, (SELECT COUNT(*) FROM subjects) AS subjects,
       (SELECT COUNT(*) FROM schedules) AS schedules;
