-- =====================================================================
-- UNIROOM – base tables
-- =====================================================================
-- Run this ONLY on a brand-new, empty Supabase database.
-- If you already copied your tables to Supabase, skip this file and run
-- 02_admin_features.sql instead.
--
-- Supabase: Dashboard → SQL Editor → New query → paste → Run.
-- =====================================================================

CREATE TABLE IF NOT EXISTS departments (
    id      SERIAL PRIMARY KEY,
    code    TEXT NOT NULL UNIQUE,          -- e.g. BSIT, BSMT
    name    TEXT                           -- e.g. Information Technology
);

CREATE TABLE IF NOT EXISTS sections (
    id             SERIAL PRIMARY KEY,
    name           TEXT NOT NULL UNIQUE,   -- e.g. BSIT-A1
    department_id  INTEGER NOT NULL REFERENCES departments(id)
);

CREATE TABLE IF NOT EXISTS users (
    id              SERIAL PRIMARY KEY,
    first_name      TEXT NOT NULL,
    last_name       TEXT NOT NULL,
    student_id      TEXT UNIQUE,
    username        TEXT NOT NULL UNIQUE,
    email           TEXT NOT NULL UNIQUE,
    contact_number  TEXT,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL CHECK (role IN ('admin', 'faculty', 'student')),
    section_id      INTEGER REFERENCES sections(id)
);

CREATE TABLE IF NOT EXISTS subjects (
    id    SERIAL PRIMARY KEY,
    code  TEXT NOT NULL,
    name  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS faculty (
    id          SERIAL PRIMARY KEY,
    first_name  TEXT NOT NULL,
    last_name   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS rooms (
    id         SERIAL PRIMARY KEY,
    room_name  TEXT NOT NULL UNIQUE,
    building   TEXT NOT NULL,
    floor      INTEGER NOT NULL,
    capacity   INTEGER NOT NULL,
    status     TEXT NOT NULL DEFAULT 'Available'
);

CREATE TABLE IF NOT EXISTS schedules (
    id           SERIAL PRIMARY KEY,
    section_id   INTEGER NOT NULL REFERENCES sections(id),
    subject_id   INTEGER NOT NULL REFERENCES subjects(id),
    room_id      INTEGER NOT NULL REFERENCES rooms(id),
    faculty_id   INTEGER REFERENCES faculty(id),
    day_of_week  TEXT NOT NULL,            -- Monday … Friday
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL
);

CREATE TABLE IF NOT EXISTS section_rooms (
    section_id  INTEGER PRIMARY KEY REFERENCES sections(id),
    room_id     INTEGER NOT NULL REFERENCES rooms(id)
);
