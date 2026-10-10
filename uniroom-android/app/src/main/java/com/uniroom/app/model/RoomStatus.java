package com.uniroom.app.model;

public enum RoomStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    OCCUPIED("Occupied");

    public final String label;

    RoomStatus(String label) { this.label = label; }
}
