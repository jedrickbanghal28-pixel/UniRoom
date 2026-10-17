-- =====================================================================
-- UNIROOM – demo faculty accounts (one for BSIT, one for BS MedTech)
-- Run once in Supabase → SQL Editor (after 06_demo_accounts.sql).
-- Running it again resets the demo classes.
--
--   Username        Name                 Teaches
--   demo.fac.bsit   Prof. Daniel Cruz    BSIT-A1  · ITE 298 Information Management
--   demo.fac.bsmt   Prof. Grace Lim      BSMT-A1/A2 · ITE 031 IT Laboratory
--
-- Passwords are random and are not written here. To sign in, change the
-- email to a real inbox and use "Resend email" on the website.
-- Each one gets a faculty record, a subject and two classes on Tuesday /
-- Thursday. A class is only added when the room, the teacher and the
-- section are free at that time (same conflict rule as the website).
-- The demo students (06) are in these sections, so a notification sent
-- from a demo faculty account reaches them.
--
-- To remove everything later:
--   DELETE FROM schedules WHERE faculty_id IN (SELECT f.id FROM faculty f JOIN users u ON u.id = f.user_id WHERE u.username LIKE 'demo.fac.%');
--   DELETE FROM faculty WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo.fac.%');
--   DELETE FROM users WHERE username LIKE 'demo.fac.%';
-- =====================================================================

-- 1) Accounts (department = the program they teach)
INSERT INTO users (first_name, last_name, employee_id, username, email, password_hash, role,
                   department_id, gender, age, must_change_password)
SELECT d.first_name, d.last_name, d.employee_id, d.username, d.email,
       extensions.crypt(encode(extensions.gen_random_bytes(12), 'hex'), extensions.gen_salt('bf')), 'faculty',
       dep.id, d.gender, d.age, TRUE
FROM (VALUES
        ('Daniel', 'Cruz', 'DEMO-FAC-IT', 'demo.fac.bsit', 'demo.fac.bsit@uniroom.demo', 'BSIT', 'Male',   35),
        ('Grace',  'Lim',  'DEMO-FAC-MT', 'demo.fac.bsmt', 'demo.fac.bsmt@uniroom.demo', 'BSMT', 'Female', 32)
     ) AS d(first_name, last_name, employee_id, username, email, program, gender, age)
JOIN departments dep ON upper(dep.code) = d.program
ON CONFLICT (username) DO UPDATE
   SET password_hash = EXCLUDED.password_hash,
       first_name = EXCLUDED.first_name,
       last_name = EXCLUDED.last_name,
       department_id = EXCLUDED.department_id,
       must_change_password = TRUE;

-- 2) Faculty records (linked to the accounts)
INSERT INTO faculty (first_name, last_name, email, user_id)
SELECT u.first_name, u.last_name, u.email, u.id
FROM users u
WHERE u.username IN ('demo.fac.bsit', 'demo.fac.bsmt')
  AND NOT EXISTS (SELECT 1 FROM faculty f WHERE f.user_id = u.id);

-- 3) Subjects they handle (added if the subject is missing)
INSERT INTO subjects (code, name)
SELECT v.code, v.name FROM (VALUES ('ITE 298', 'Information Management'), ('ITE 031', 'IT Laboratory')) v(code, name)
WHERE NOT EXISTS (SELECT 1 FROM subjects s WHERE upper(s.code) = v.code AND lower(s.name) = lower(v.name));

INSERT INTO faculty_subjects (faculty_id, subject_id, department_id)
SELECT f.id, s.id, dep.id
FROM (VALUES ('demo.fac.bsit', 'ITE 298', 'Information Management', 'BSIT'),
             ('demo.fac.bsmt', 'ITE 031', 'IT Laboratory', 'BSMT')) v(username, code, name, program)
JOIN users u ON u.username = v.username
JOIN faculty f ON f.user_id = u.id
JOIN subjects s ON upper(s.code) = v.code AND lower(s.name) = lower(v.name)
JOIN departments dep ON upper(dep.code) = v.program
ON CONFLICT DO NOTHING;

-- 4) Demo classes (old demo classes are replaced; skipped if anything clashes)
DELETE FROM schedules
WHERE faculty_id IN (SELECT f.id FROM faculty f JOIN users u ON u.id = f.user_id WHERE u.username LIKE 'demo.fac.%');

INSERT INTO schedules (section_id, subject_id, room_id, faculty_id, day_of_week, start_time, end_time)
SELECT sec.id, s.id, r.id, f.id, v.day, v.start_t::time, v.end_t::time
FROM (VALUES
        ('demo.fac.bsit', 'BSIT-A1', 'ITE 298', 'Information Management', 'TH 305', 'Tuesday',  '08:00', '10:00'),
        ('demo.fac.bsit', 'BSIT-A1', 'ITE 298', 'Information Management', 'TH 306', 'Thursday', '08:00', '10:00'),
        ('demo.fac.bsmt', 'BSMT-A1', 'ITE 031', 'IT Laboratory',          'TH 309', 'Tuesday',  '10:30', '12:30'),
        ('demo.fac.bsmt', 'BSMT-A2', 'ITE 031', 'IT Laboratory',          'TH 310', 'Tuesday',  '13:00', '15:00')
     ) AS v(username, section, code, name, room, day, start_t, end_t)
JOIN users u ON u.username = v.username
JOIN faculty f ON f.user_id = u.id
JOIN sections sec ON upper(sec.name) = v.section
JOIN subjects s ON upper(s.code) = v.code AND lower(s.name) = lower(v.name)
JOIN rooms r ON upper(r.room_name) = v.room
WHERE NOT EXISTS (
    SELECT 1 FROM schedules x
    WHERE x.day_of_week = v.day
      AND x.start_time < v.end_t::time AND v.start_t::time < x.end_time
      AND (x.room_id = r.id OR x.faculty_id = f.id OR x.section_id = sec.id));

-- Check: the two accounts and their classes
SELECT u.username, u.first_name || ' ' || u.last_name AS name, d.code AS program,
       sec.name AS section, sub.code AS subject, sc.day_of_week, sc.start_time, sc.end_time, r.room_name
FROM users u
JOIN departments d ON d.id = u.department_id
JOIN faculty f ON f.user_id = u.id
LEFT JOIN schedules sc ON sc.faculty_id = f.id
LEFT JOIN sections sec ON sec.id = sc.section_id
LEFT JOIN subjects sub ON sub.id = sc.subject_id
LEFT JOIN rooms r ON r.id = sc.room_id
WHERE u.username LIKE 'demo.fac.%'
ORDER BY u.username, sc.day_of_week, sc.start_time;
