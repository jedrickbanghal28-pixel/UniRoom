package com.uniroom.app.model;

/** A notification a student receives from a teacher. */
public class AppAlert {
    public static final String TRANSFER = "transfer";
    public static final String ANNOUNCEMENT = "announcement";

    public final String id;
    public final Program program;
    public final String kind;
    public final String title;
    public final String body;
    public final String from;
    public final String time;
    public boolean unread;

    public AppAlert(String id, Program program, String kind, String title, String body,
                    String from, String time, boolean unread) {
        this.id = id;
        this.program = program;
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.from = from;
        this.time = time;
        this.unread = unread;
    }
}
