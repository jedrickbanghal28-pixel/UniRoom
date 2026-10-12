package com.uniroom.app.data;

import com.uniroom.app.R;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.RoomLocation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed facts about the Tech Hub building that are not stored in the
 * database: what kind of room each room is, which college uses it, and the
 * photos + walking directions. Everything that changes (rooms, capacity,
 * status, classes, teachers, students) comes from the database.
 */
public final class RoomCatalog {

    private RoomCatalog() {}

    private static final String OURS = "BSIT & BS MedTech";

    /** Room code → { type, department }. */
    private static final Map<String, String[]> INFO = new HashMap<>();

    static {
        put(Room.LECTURE_ROOM, OURS, "TH 305", "TH 306", "TH 307", "TH 308");
        put(Room.COMPUTER_LAB, OURS, "TH 309", "TH 310", "TH 311");
        put(Room.DESIGN_STUDIO, "SD+C", "TH 201", "TH 202");
        put(Room.LECTURE_ROOM, "SD+C", "TH 203");
        put(Room.ENGINEERING_LAB, "CITE · Engineering", "TH 204", "TH 206");
        put(Room.LECTURE_ROOM, "CITE · Engineering", "TH 205");
    }

    private static void put(String type, String department, String... codes) {
        for (String c : codes) INFO.put(c, new String[]{type, department});
    }

    /** Kind of room, e.g. "Computer lab". Rooms the admin adds later are guessed from the name. */
    public static String typeOf(String code, int floor) {
        String[] i = INFO.get(code);
        if (i != null) return i[0];
        String c = code.toLowerCase();
        if (c.contains("lab")) return Room.COMPUTER_LAB;
        return Room.LECTURE_ROOM;
    }

    /** Who uses the room. 3rd floor rooms belong to BSIT and BS MedTech. */
    public static String departmentOf(String code, int floor) {
        String[] i = INFO.get(code);
        if (i != null) return i[1];
        return floor == 3 ? OURS : "Other colleges";
    }

    public static String departmentName(String department) {
        if ("SD+C".equals(department)) return "School of Design + Communication (SD+C)";
        if ("CITE · Engineering".equals(department)) return "College of IT & Engineering (CITE) · Engineering";
        return department;
    }

    // ---------- Room photos + directions ----------
    public static final List<RoomLocation> ROOM_LOCATIONS = Arrays.asList(
            new RoomLocation("th-lecture", "Lecture rooms", "TH 305, 306, 308",
                    new String[]{"TH 305", "TH 306", "TH 308"}, R.drawable.room_lecture,
                    "3rd floor, near the Faculty Office",
                    new String[]{
                            "Enter Tech Hub through the central courtyard.",
                            "Take the main stairs up to the 3rd floor (top level).",
                            "TH 305, 306 and 308 line the corridor facing the courtyard, near the Faculty Office."}),
            new RoomLocation("th-307", "Balcony lecture room", "TH 307",
                    new String[]{"TH 307"}, R.drawable.room_balcony,
                    "3rd floor, end of the balcony beside the study tables",
                    new String[]{
                            "Go up to the 3rd floor of Tech Hub.",
                            "Walk to the end of the balcony overlooking the trees.",
                            "TH 307 is the grey door beside the study tables."}),
            new RoomLocation("th-labs", "Computer labs", "TH 309–311",
                    new String[]{"TH 309", "TH 310", "TH 311"}, R.drawable.room_labs,
                    "3rd floor, near the stairwell",
                    new String[]{
                            "Take the stairwell up to the 3rd floor.",
                            "Follow the open corridor along the railing.",
                            "TH 309 is the first door, next to the yellow bin. TH 310 and TH 311 continue down the hall."})
    );

    public static RoomLocation locationFor(String roomCode) {
        for (RoomLocation l : ROOM_LOCATIONS) if (l.rooms.contains(roomCode)) return l;
        return null;
    }
}
