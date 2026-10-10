package com.uniroom.app.model;

public class Room {
    public static final String COMPUTER_LAB = "Computer lab";
    public static final String LECTURE_ROOM = "Lecture room";
    public static final String DESIGN_STUDIO = "Design studio";
    public static final String ENGINEERING_LAB = "Engineering lab";

    public final String code;
    public final String building;
    public final int floor;
    public final int capacity;
    public final RoomStatus status;
    public final String type;
    public final String department;

    public Room(String code, String building, int floor, int capacity,
                RoomStatus status, String type, String department) {
        this.code = code;
        this.building = building;
        this.floor = floor;
        this.capacity = capacity;
        this.status = status;
        this.type = type;
        this.department = department;
    }

    public boolean isLab() {
        return COMPUTER_LAB.equals(type) || ENGINEERING_LAB.equals(type);
    }
}
