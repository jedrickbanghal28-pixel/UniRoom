package com.uniroom.app.model;

public enum RoomStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    OCCUPIED("Occupied"),
    MAINTENANCE("Maintenance");

    public final String label;

    RoomStatus(String label) { this.label = label; }

    /** "Available" / "Reserved" / ... from the database (unknown → Available). */
    public static RoomStatus of(String text) {
        for (RoomStatus s : values()) if (s.label.equalsIgnoreCase(text == null ? "" : text.trim())) return s;
        return AVAILABLE;
    }
}
