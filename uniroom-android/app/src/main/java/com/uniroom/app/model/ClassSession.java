package com.uniroom.app.model;

/** One class meeting in the weekly timetable. */
public class ClassSession {
    public final String id;
    public final String code;
    public final String title;
    /** 0 = Monday ... 6 = Sunday */
    public final int day;
    /** 24-hour time, e.g. "13:00" */
    public final String start;
    public final String end;
    public final String room;
    public final String building;
    public final String teacherId;
    public final String instructor;
    public final Program program;
    public final String section;

    public ClassSession(String id, String code, String title, int day, String start, String end,
                        String room, String building, String teacherId, String instructor,
                        Program program, String section) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.day = day;
        this.start = start;
        this.end = end;
        this.room = room;
        this.building = building;
        this.teacherId = teacherId;
        this.instructor = instructor;
        this.program = program;
        this.section = section;
    }
}
