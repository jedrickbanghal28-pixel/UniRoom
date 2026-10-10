package com.uniroom.app.model;

public class Student {
    public final String id;
    public final String fullName;
    public final String firstName;
    public final String initials;
    public final String username;
    public final String email;
    public final String schoolId;
    public final String contact;
    public final Program program;
    public final String section;

    public Student(String id, String fullName, String firstName, String initials, String username,
                   String email, String schoolId, String contact, Program program, String section) {
        this.id = id;
        this.fullName = fullName;
        this.firstName = firstName;
        this.initials = initials;
        this.username = username;
        this.email = email;
        this.schoolId = schoolId;
        this.contact = contact;
        this.program = program;
        this.section = section;
    }
}
