package com.uniroom.app.model;

import java.util.Arrays;
import java.util.List;

/** A photo of an area of the building plus walking directions. */
public class RoomLocation {
    public final String id;
    public final String label;
    public final String range;
    public final List<String> rooms;
    /** Drawable resource of the photo. */
    public final int imageRes;
    public final String landmark;
    public final String[] directions;

    public RoomLocation(String id, String label, String range, String[] rooms, int imageRes,
                        String landmark, String[] directions) {
        this.id = id;
        this.label = label;
        this.range = range;
        this.rooms = Arrays.asList(rooms);
        this.imageRes = imageRes;
        this.landmark = landmark;
        this.directions = directions;
    }
}
