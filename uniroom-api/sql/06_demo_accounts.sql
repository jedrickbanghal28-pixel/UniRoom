-- =====================================================================
-- UNIROOM – demo student accounts (for the class demo / defense)
-- Run once in Supabase → SQL Editor. Running it again just resets the
-- passwords. They are real accounts in the users table, so they work in
-- the app exactly like accounts the admin creates (schedule, rooms,
-- notifications), but they skip the "change temporary password" step.
--
--   Username     Password   Name              Program / section
--   demo.bsit1   Demo2026   Juan Dela Cruz    BSIT · BSIT-A1
--   demo.bsit2   Demo2026   Bea Santos        BSIT · BSIT-A1
--   demo.bsmt1   Demo2026   Andrea Reyes      BSMT · BSMT-A1
--   demo.bsmt2   Demo2026   Paolo Garcia      BSMT · BSMT-A2
--
-- Faculty accounts are not changed.
-- To remove them later:  DELETE FROM users WHERE username LIKE 'demo.%';
-- =====================================================================

INSERT INTO users (first_name, last_name, student_id, username, email, password_hash, role,
                   section_id, department_id, gender, age, must_change_password)
SELECT d.first_name, d.last_name, d.student_id, d.username, d.email,
       extensions.crypt('Demo2026', extensions.gen_salt('bf')), 'student',
       s.id, s.department_id, d.gender, d.age, FALSE
FROM (VALUES
        ('Juan',    'Dela Cruz', 'DEMO-IT-01', 'demo.bsit1', 'demo.bsit1@uniroom.demo', 'BSIT', 'BSIT-A1', 'Male',   19),
        ('Bea',     'Santos',    'DEMO-IT-02', 'demo.bsit2', 'demo.bsit2@uniroom.demo', 'BSIT', 'BSIT-A1', 'Female', 20),
        ('Andrea',  'Reyes',     'DEMO-MT-01', 'demo.bsmt1', 'demo.bsmt1@uniroom.demo', 'BSMT', 'BSMT-A1', 'Female', 19),
        ('Paolo',   'Garcia',    'DEMO-MT-02', 'demo.bsmt2', 'demo.bsmt2@uniroom.demo', 'BSMT', 'BSMT-A2', 'Male',   20)
     ) AS d(first_name, last_name, student_id, username, email, program, section, gender, age)
-- the named section, or else the first section of that program
CROSS JOIN LATERAL (
    SELECT sec.id, sec.department_id
    FROM sections sec JOIN departments dep ON dep.id = sec.department_id
    WHERE upper(dep.code) = d.program
    ORDER BY (upper(sec.name) = d.section) DESC, sec.name
    LIMIT 1) s
ON CONFLICT (username) DO UPDATE
   SET password_hash = EXCLUDED.password_hash,
       first_name = EXCLUDED.first_name,
       last_name = EXCLUDED.last_name,
       must_change_password = FALSE,
       section_id = EXCLUDED.section_id,
       department_id = EXCLUDED.department_id;

-- Check: should list 4 accounts with their section
SELECT u.username, u.first_name || ' ' || u.last_name AS name, s.name AS section, d.code AS program
FROM users u JOIN sections s ON s.id = u.section_id JOIN departments d ON d.id = s.department_id
WHERE u.username LIKE 'demo.%' ORDER BY u.username;
