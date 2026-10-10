package com.uniroom.app.model;

/** The person who is signed in: either a student or a teacher. */
public class Account {
    public final Role role;
    public final Student student;   // set when role == STUDENT
    public final Teacher teacher;   // set when role == FACULTY

    private Account(Role role, Student student, Teacher teacher) {
        this.role = role;
        this.student = student;
        this.teacher = teacher;
    }

    public static Account of(Student s) { return new Account(Role.STUDENT, s, null); }

    public static Account of(Teacher t) { return new Account(Role.FACULTY, null, t); }

    public boolean isStudent() { return role == Role.STUDENT; }

    public boolean isFaculty() { return role == Role.FACULTY; }
}
