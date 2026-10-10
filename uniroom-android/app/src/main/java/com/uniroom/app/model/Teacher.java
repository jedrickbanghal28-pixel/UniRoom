package com.uniroom.app.model;

public class Teacher {
    public final String id;
    public final String fullName;
    public final String lastName;
    public final String initials;
    public final String username;
    public final String email;
    public final String subject;

    public Teacher(String id, String fullName, String lastName, String initials,
                   String username, String email, String subject) {
        this.id = id;
        this.fullName = fullName;
        this.lastName = lastName;
        this.initials = initials;
        this.username = username;
        this.email = email;
        this.subject = subject;
    }
}
