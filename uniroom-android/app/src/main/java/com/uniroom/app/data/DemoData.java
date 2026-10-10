package com.uniroom.app.data;

import com.uniroom.app.R;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Program;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.RoomLocation;
import com.uniroom.app.model.RoomStatus;
import com.uniroom.app.model.Teacher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in data: rooms (with photos and directions), the three faculty and
 * their timetable. Student accounts, student schedules and notifications
 * come from the database through the API.
 */
public final class DemoData {

    private DemoData() {}

    // ---------- Rooms ----------
    private static final String TH = "Tech Hub";
    private static final String OURS = "BSIT & BS MedTech";

    public static final List<Room> ROOMS = Arrays.asList(
            // 3rd floor — BSIT & BS MedTech
            new Room("TH 305", TH, 3, 50, RoomStatus.AVAILABLE, Room.LECTURE_ROOM, OURS),
            new Room("TH 306", TH, 3, 50, RoomStatus.AVAILABLE, Room.LECTURE_ROOM, OURS),
            new Room("TH 307", TH, 3, 35, RoomStatus.AVAILABLE, Room.LECTURE_ROOM, OURS),
            new Room("TH 308", TH, 3, 50, RoomStatus.AVAILABLE, Room.LECTURE_ROOM, OURS),
            new Room("TH 309", TH, 3, 40, RoomStatus.RESERVED, Room.COMPUTER_LAB, OURS),
            new Room("TH 310", TH, 3, 40, RoomStatus.RESERVED, Room.COMPUTER_LAB, OURS),
            new Room("TH 311", TH, 3, 40, RoomStatus.RESERVED, Room.COMPUTER_LAB, OURS),
            // 2nd floor — other colleges (for reference only)
            new Room("TH 201", TH, 2, 30, RoomStatus.OCCUPIED, Room.DESIGN_STUDIO, "SD+C"),
            new Room("TH 202", TH, 2, 30, RoomStatus.AVAILABLE, Room.DESIGN_STUDIO, "SD+C"),
            new Room("TH 203", TH, 2, 40, RoomStatus.RESERVED, Room.LECTURE_ROOM, "SD+C"),
            new Room("TH 204", TH, 2, 35, RoomStatus.OCCUPIED, Room.ENGINEERING_LAB, "CITE · Engineering"),
            new Room("TH 205", TH, 2, 45, RoomStatus.AVAILABLE, Room.LECTURE_ROOM, "CITE · Engineering"),
            new Room("TH 206", TH, 2, 35, RoomStatus.AVAILABLE, Room.ENGINEERING_LAB, "CITE · Engineering")
    );

    public static String departmentName(String department) {
        if ("SD+C".equals(department)) return "School of Design + Communication (SD+C)";
        if ("CITE · Engineering".equals(department)) return "College of IT & Engineering (CITE) · Engineering";
        return department;
    }

    public static Room findRoom(String code) {
        for (Room r : ROOMS) if (r.code.equals(code)) return r;
        return null;
    }

    public static List<Room> roomsOnFloor(int floor) {
        List<Room> out = new ArrayList<>();
        for (Room r : ROOMS) if (r.floor == floor) out.add(r);
        return out;
    }

    // ---------- Teachers ----------
    private static final String ALVARADO = "Farah Diva Alvarado";
    private static final String ILUSTRISIMO = "Almie Joy Batiancila Ilustrisimo";
    private static final String SEGARRA = "Marvy P Segarra";

    public static final List<Teacher> TEACHERS = Arrays.asList(
            new Teacher("alvarado", ALVARADO, "Alvarado", "FA", "falvarado",
                    "f.alvarado@university.edu", "ITE 031 – IT Laboratory"),
            new Teacher("ilustrisimo", ILUSTRISIMO, "Ilustrisimo", "AI", "ailustrisimo",
                    "a.ilustrisimo@university.edu", "ITE 031 – Data Structure and Algorithms"),
            new Teacher("segarra", SEGARRA, "Segarra", "MS", "msegarra",
                    "m.segarra@university.edu", "ITE 298 – Information Management")
    );

    // ---------- Weekly timetable ----------
    public static final List<ClassSession> SESSIONS = Arrays.asList(
            // BSIT — Wednesday afternoon & Friday
            new ClassSession("it1", "ITE 031", "IT Laboratory", 2, "13:00", "16:00", "TH 309", TH, "alvarado", ALVARADO, Program.BSIT, "BSIT-A1"),
            new ClassSession("it2", "ITE 031", "Data Structure and Algorithms", 4, "08:00", "10:00", "TH 308", TH, "ilustrisimo", ILUSTRISIMO, Program.BSIT, "BSIT-A1"),
            new ClassSession("it3", "ITE 298", "Information Management", 4, "10:30", "12:30", "TH 308", TH, "segarra", SEGARRA, Program.BSIT, "BSIT-A1"),

            // BS MedTech — Monday (afternoon lecture rooms)
            new ClassSession("mt1", "ITE 031", "Data Structure and Algorithms", 0, "13:00", "15:00", "TH 305", TH, "ilustrisimo", ILUSTRISIMO, Program.BSMT, "BSMT-A1"),
            new ClassSession("mt2", "ITE 298", "Information Management", 0, "13:00", "15:00", "TH 307", TH, "segarra", SEGARRA, Program.BSMT, "BSMT-A2"),
            new ClassSession("mt3", "ITE 298", "Information Management", 0, "15:30", "17:30", "TH 308", TH, "segarra", SEGARRA, Program.BSMT, "BSMT-A1"),
            new ClassSession("mt4", "ITE 031", "Data Structure and Algorithms", 0, "15:30", "17:30", "TH 306", TH, "ilustrisimo", ILUSTRISIMO, Program.BSMT, "BSMT-A2"),

            // BS MedTech — Wednesday morning (labs)
            new ClassSession("mt5", "ITE 031", "IT Laboratory", 2, "08:00", "10:00", "TH 310", TH, "alvarado", ALVARADO, Program.BSMT, "BSMT-A1"),
            new ClassSession("mt6", "ITE 031", "IT Laboratory", 2, "10:00", "12:00", "TH 311", TH, "alvarado", ALVARADO, Program.BSMT, "BSMT-A2"),

            // BS MedTech — Thursday (afternoon lecture rooms)
            new ClassSession("mt7", "ITE 031", "Data Structure and Algorithms", 3, "13:00", "15:00", "TH 306", TH, "ilustrisimo", ILUSTRISIMO, Program.BSMT, "BSMT-A1"),
            new ClassSession("mt8", "ITE 298", "Information Management", 3, "13:00", "15:00", "TH 308", TH, "segarra", SEGARRA, Program.BSMT, "BSMT-A2"),
            new ClassSession("mt9", "ITE 298", "Information Management", 3, "15:30", "17:30", "TH 307", TH, "segarra", SEGARRA, Program.BSMT, "BSMT-A1"),
            new ClassSession("mt10", "ITE 031", "Data Structure and Algorithms", 3, "15:30", "17:30", "TH 305", TH, "ilustrisimo", ILUSTRISIMO, Program.BSMT, "BSMT-A2")
    );

    /** Starting room assignments per teacher, per program portal. */
    public static Map<Program, List<String>> startingRoomsFor(String teacherId) {
        Map<Program, List<String>> m = new HashMap<>();
        switch (teacherId) {
            case "alvarado":
                m.put(Program.BSIT, list("TH 309"));
                m.put(Program.BSMT, list("TH 310", "TH 311"));
                break;
            case "ilustrisimo":
                m.put(Program.BSIT, list("TH 308"));
                m.put(Program.BSMT, list("TH 305", "TH 306"));
                break;
            case "segarra":
                m.put(Program.BSIT, list("TH 308"));
                m.put(Program.BSMT, list("TH 307", "TH 308"));
                break;
            default:
                m.put(Program.BSIT, new ArrayList<String>());
                m.put(Program.BSMT, new ArrayList<String>());
        }
        return m;
    }

    private static List<String> list(String... items) {
        return new ArrayList<>(Arrays.asList(items));
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
