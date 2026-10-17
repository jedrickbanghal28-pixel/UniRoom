-- =====================================================================
-- UNIROOM – faculty, subjects and class schedules
-- (BSIT and BSMEDTECH, Tech Hub 3rd floor)
-- =====================================================================
-- Run AFTER 01_base_schema.sql and 02_admin_features.sql.
-- Safe to run more than once: nothing is added twice.
--
-- Faculty and the subject each one handles (in BOTH programs):
--   Farah Diva Alvarado               ITE 031 – IT Laboratory
--   Almie Joy Batiancila Ilustrisimo  ITE 031 – Data Structure and Algorithms
--   Marvy P Segarra                   ITE 298 – Information Management
--
-- BEFORE RUNNING: change the three faculty emails in section 6 to their
-- real Gmail / school addresses (they are used to log in).
-- =====================================================================


-- ---------- 1. Departments (programs) ----------
INSERT INTO departments (code, name) VALUES
    ('BSIT', 'Information Technology'),
    ('BSMT', 'Medical Technology (BSMEDTECH)')
ON CONFLICT (code) DO NOTHING;


-- ---------- 2. Sections ----------
INSERT INTO sections (name, department_id)
SELECT v.name, d.id
FROM (VALUES ('BSIT-A1', 'BSIT'), ('BSMT-A1', 'BSMT'), ('BSMT-A2', 'BSMT')) AS v(name, dept)
JOIN departments d ON d.code = v.dept
ON CONFLICT (name) DO NOTHING;


-- ---------- 3. Rooms (Tech Hub, 3rd floor) ----------
INSERT INTO rooms (room_name, building, floor, capacity, status) VALUES
    ('TH 305', 'Tech Hub', 3, 50, 'Available'),
    ('TH 306', 'Tech Hub', 3, 50, 'Available'),
    ('TH 307', 'Tech Hub', 3, 35, 'Available'),
    ('TH 308', 'Tech Hub', 3, 50, 'Available'),
    ('TH 309', 'Tech Hub', 3, 40, 'Reserved'),
    ('TH 310', 'Tech Hub', 3, 40, 'Reserved'),
    ('TH 311', 'Tech Hub', 3, 40, 'Reserved')
ON CONFLICT (room_name) DO NOTHING;


-- ---------- 4. Subjects ----------
INSERT INTO subjects (code, name)
SELECT v.code, v.name
FROM (VALUES
    ('ITE 031', 'IT Laboratory'),
    ('ITE 031', 'Data Structures and Algorithms'),
    ('ITE 298', 'Information Management')
) AS v(code, name)
WHERE NOT EXISTS (SELECT 1 FROM subjects s WHERE s.code = v.code AND s.name = v.name);


-- ---------- 5. Faculty list (used by the schedule form) ----------
INSERT INTO faculty (first_name, last_name, email)
SELECT v.first_name, v.last_name, v.email
FROM (VALUES
    ('Farah Diva',            'Alvarado',    'f.alvarado@university.edu'),
    ('Almie Joy Batiancila',  'Ilustrisimo', 'a.ilustrisimo@university.edu'),
    ('Marvy P',               'Segarra',     'm.segarra@university.edu')
) AS v(first_name, last_name, email)
WHERE NOT EXISTS (
    SELECT 1 FROM faculty f WHERE f.first_name = v.first_name AND f.last_name = v.last_name
);


-- ---------- 6. Faculty login accounts ----------
-- Each account gets a random password that nobody knows. Open the account on
-- the website and use "Resend email" to send the faculty member their login.
-- ⚠ Change these emails to the real ones before running.
INSERT INTO users (first_name, last_name, username, email, password_hash, role,
                   department_id, gender, must_change_password)
SELECT v.first_name, v.last_name, v.username, v.email,
       extensions.crypt(encode(extensions.gen_random_bytes(12), 'hex'), extensions.gen_salt('bf')),
       'faculty', d.id, v.gender, TRUE
FROM (VALUES
    ('Farah Diva',           'Alvarado',    'falvarado',    'f.alvarado@university.edu',    'Female'),
    ('Almie Joy Batiancila', 'Ilustrisimo', 'ailustrisimo', 'a.ilustrisimo@university.edu', 'Female'),
    ('Marvy P',              'Segarra',     'msegarra',     'm.segarra@university.edu',     'Female')
) AS v(first_name, last_name, username, email, gender)
JOIN departments d ON d.code = 'BSIT'          -- home department; they also teach BSMEDTECH (below)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE lower(u.email) = lower(v.email))
  AND NOT EXISTS (SELECT 1 FROM users u WHERE lower(u.username) = v.username);

-- Link each faculty-list entry to its login account
UPDATE faculty f
SET    user_id = u.id
FROM   users u
WHERE  u.role = 'faculty'
  AND  u.first_name = f.first_name
  AND  u.last_name  = f.last_name
  AND  f.user_id IS NULL;


-- ---------- 7. Subject each faculty member handles – BSIT and BSMEDTECH ----------
INSERT INTO faculty_subjects (faculty_id, subject_id, department_id)
SELECT f.id, s.id, d.id
FROM (VALUES
    ('Alvarado',    'ITE 031', 'IT Laboratory'),
    ('Ilustrisimo', 'ITE 031', 'Data Structure and Algorithms'),
    ('Segarra',     'ITE 298', 'Information Management')
) AS v(last_name, code, subject)
JOIN faculty f      ON f.last_name = v.last_name
JOIN subjects s     ON s.code = v.code AND s.name = v.subject
CROSS JOIN departments d
WHERE d.code IN ('BSIT', 'BSMT')
ON CONFLICT DO NOTHING;


-- ---------- 8. Weekly class schedule ----------
-- (No room, faculty or section has two classes at the same time.)
INSERT INTO schedules (section_id, subject_id, room_id, faculty_id, day_of_week, start_time, end_time)
SELECT sec.id, sub.id, r.id, f.id, v.day, v.start_time::time, v.end_time::time
FROM (VALUES
    -- BSIT-A1
    ('BSIT-A1', 'IT Laboratory',                 'TH 309', 'Alvarado',    'Wednesday', '13:00', '16:00'),
    ('BSIT-A1', 'Data Structure and Algorithms', 'TH 308', 'Ilustrisimo', 'Friday',    '08:00', '10:00'),
    ('BSIT-A1', 'Information Management',        'TH 308', 'Segarra',     'Friday',    '10:30', '12:30'),
    -- BSMT-A1
    ('BSMT-A1', 'Data Structure and Algorithms', 'TH 305', 'Ilustrisimo', 'Monday',    '13:00', '15:00'),
    ('BSMT-A1', 'Information Management',        'TH 308', 'Segarra',     'Monday',    '15:30', '17:30'),
    ('BSMT-A1', 'IT Laboratory',                 'TH 310', 'Alvarado',    'Wednesday', '08:00', '10:00'),
    ('BSMT-A1', 'Data Structure and Algorithms', 'TH 306', 'Ilustrisimo', 'Thursday',  '13:00', '15:00'),
    ('BSMT-A1', 'Information Management',        'TH 307', 'Segarra',     'Thursday',  '15:30', '17:30'),
    -- BSMT-A2
    ('BSMT-A2', 'Information Management',        'TH 307', 'Segarra',     'Monday',    '13:00', '15:00'),
    ('BSMT-A2', 'Data Structure and Algorithms', 'TH 306', 'Ilustrisimo', 'Monday',    '15:30', '17:30'),
    ('BSMT-A2', 'IT Laboratory',                 'TH 311', 'Alvarado',    'Wednesday', '10:00', '12:00'),
    ('BSMT-A2', 'Information Management',        'TH 308', 'Segarra',     'Thursday',  '13:00', '15:00'),
    ('BSMT-A2', 'Data Structure and Algorithms', 'TH 305', 'Ilustrisimo', 'Thursday',  '15:30', '17:30')
) AS v(section, subject, room, faculty, day, start_time, end_time)
JOIN sections sec ON sec.name = v.section
JOIN subjects sub ON sub.name = v.subject
JOIN rooms r      ON r.room_name = v.room
JOIN faculty f    ON f.last_name = v.faculty
WHERE NOT EXISTS (
    SELECT 1 FROM schedules x
    WHERE x.section_id = sec.id AND x.day_of_week = v.day AND x.start_time = v.start_time::time
);


-- ---------- Check the result ----------
SELECT f.first_name || ' ' || f.last_name AS faculty,
       string_agg(DISTINCT s.code || ' – ' || s.name, ', ') AS handles,
       string_agg(DISTINCT d.code, ', ') AS programs
FROM faculty f
JOIN faculty_subjects fs ON fs.faculty_id = f.id
JOIN subjects s ON s.id = fs.subject_id
JOIN departments d ON d.id = fs.department_id
GROUP BY f.id, f.first_name, f.last_name
ORDER BY f.last_name;
