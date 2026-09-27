DROP TABLE IF EXISTS schedules, subjects, faculty, rooms, users, sections, departments CASCADE;
CREATE TABLE departments(id SERIAL PRIMARY KEY,code VARCHAR(20) UNIQUE NOT NULL,name VARCHAR(120) NOT NULL,office VARCHAR(120));
CREATE TABLE sections(id SERIAL PRIMARY KEY,department_id INT NOT NULL REFERENCES departments(id),name VARCHAR(40) NOT NULL,year_level INT NOT NULL CHECK(year_level=1),UNIQUE(department_id,name));
CREATE TABLE users(id SERIAL PRIMARY KEY,first_name VARCHAR(60) NOT NULL,last_name VARCHAR(60) NOT NULL,student_id VARCHAR(40) UNIQUE NOT NULL,username VARCHAR(50) UNIQUE NOT NULL,email VARCHAR(120) UNIQUE NOT NULL,contact_number VARCHAR(30) NOT NULL,password_hash TEXT NOT NULL,role VARCHAR(20) NOT NULL DEFAULT 'student' CHECK(role IN('student','admin','department_head')),section_id INT NOT NULL REFERENCES sections(id),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE faculty(id SERIAL PRIMARY KEY,employee_id VARCHAR(40) UNIQUE NOT NULL,first_name VARCHAR(60) NOT NULL,last_name VARCHAR(60) NOT NULL,email VARCHAR(120) UNIQUE,department_id INT NOT NULL REFERENCES departments(id));
CREATE TABLE rooms(id SERIAL PRIMARY KEY,room_name VARCHAR(50) UNIQUE NOT NULL,building VARCHAR(80) NOT NULL,floor INT NOT NULL,capacity INT NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'Available' CHECK(status IN('Available','Occupied','Reserved','Maintenance')));
CREATE TABLE subjects(id SERIAL PRIMARY KEY,code VARCHAR(30) UNIQUE NOT NULL,name VARCHAR(150) NOT NULL);
CREATE TABLE schedules(id SERIAL PRIMARY KEY,section_id INT NOT NULL REFERENCES sections(id),subject_id INT NOT NULL REFERENCES subjects(id),room_id INT NOT NULL REFERENCES rooms(id),faculty_id INT REFERENCES faculty(id),day_of_week VARCHAR(12) NOT NULL,start_time TIME NOT NULL,end_time TIME NOT NULL,CHECK(end_time>start_time));
INSERT INTO departments(code,name,office) VALUES('BSIT','College of Information Technology','CIT Office'),('BSMEDTECH','College of Medical Technology','MedTech Office');
INSERT INTO sections(department_id,name,year_level) SELECT id,'BS-IT A1',1 FROM departments WHERE code='BSIT';
INSERT INTO sections(department_id,name,year_level) SELECT id,'BS-MedTech A1',1 FROM departments WHERE code='BSMEDTECH';
INSERT INTO rooms(room_name,building,floor,capacity,status) VALUES('TH 309','Tech Hub',3,40,'Available'),('TH 311','Tech Hub',3,40,'Available'),('TH 303','Tech Hub',3,40,'Available'),('M 303','Main Building',3,40,'Available'),('TH 305','Tech Hub',3,40,'Available'),('TH 310','Tech Hub',3,40,'Available');
INSERT INTO subjects(code,name) VALUES('ITE 300','Information Technology Fundamentals'),('ITE 031','IT Laboratory'),('ITE 292','Systems and Architecture'),('HIS 007','Life and Work of Rizal');
INSERT INTO faculty(employee_id,first_name,last_name,email,department_id) SELECT 'F001','Darleen Realin','Medrano','darleen.medrano@example.edu',id FROM departments WHERE code='BSIT';
INSERT INTO schedules(section_id,subject_id,room_id,faculty_id,day_of_week,start_time,end_time) SELECT sec.id,sub.id,r.id,f.id,'Tuesday','13:00','16:00' FROM sections sec,subjects sub,rooms r,faculty f WHERE sec.name='BS-IT A1' AND sub.code='ITE 300' AND r.room_name='TH 309' AND f.employee_id='F001';
INSERT INTO schedules(section_id,subject_id,room_id,faculty_id,day_of_week,start_time,end_time) SELECT sec.id,sub.id,r.id,f.id,'Wednesday','10:00','12:00' FROM sections sec,subjects sub,rooms r,faculty f WHERE sec.name='BS-IT A1' AND sub.code='ITE 031' AND r.room_name='TH 311' AND f.employee_id='F001';

-- UniRoom section-to-room bridge used by the faculty Room Tagging feature.
CREATE TABLE IF NOT EXISTS section_rooms (
    id SERIAL PRIMARY KEY,
    section_id INTEGER NOT NULL UNIQUE,
    room_id INTEGER NOT NULL,
    CONSTRAINT fk_section_rooms_section
        FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
    CONSTRAINT fk_section_rooms_room
        FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);
