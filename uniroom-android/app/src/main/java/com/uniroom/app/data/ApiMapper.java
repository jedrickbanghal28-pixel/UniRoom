package com.uniroom.app.data;

import com.uniroom.app.model.AppAlert;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Program;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.RoomStatus;
import com.uniroom.app.model.Student;
import com.uniroom.app.model.Teacher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns the API's answers into the app's own objects (Student, Teacher,
 * ClassSession, Room, AppAlert). Every screen reads these objects, and all of
 * them come from the database.
 */
public final class ApiMapper {

    private ApiMapper() {}

    private static final String[] DAYS =
            {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

    // ---------- People ----------

    /** "user" object from login.php / me.php → Student. */
    public static Student student(JSONObject u) {
        String first = text(u, "first_name");
        String last = text(u, "last_name");
        String full = (first + " " + last).trim();
        String section = text(u, "section_name");

        return new Student(
                String.valueOf(u.optInt("id")),
                full.isEmpty() ? text(u, "username") : full,
                first.isEmpty() ? full : first,
                initials(first, last),
                text(u, "username"),
                text(u, "email"),
                orDash(text(u, "student_id")),
                orDash(text(u, "contact_number")),
                Program.of(text(u, "program"), section),
                section.isEmpty() ? "No section yet" : section);
    }

    /** me.php answer of a faculty account → Teacher (any faculty the admin creates works). */
    public static Teacher teacher(JSONObject me) {
        JSONObject u = me.optJSONObject("user");
        if (u == null) u = new JSONObject();
        String first = text(u, "first_name");
        String last = text(u, "last_name");
        String full = (first + " " + last).trim();

        // "ITE 298 – Information Management, ITE 031 – IT Laboratory" (each subject once)
        Set<String> subjects = new LinkedHashSet<>();
        JSONArray rows = me.optJSONArray("subjects");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject s = rows.optJSONObject(i);
            if (s != null) subjects.add(text(s, "code") + " – " + text(s, "name"));
        }
        String subject = subjects.isEmpty() ? "No subjects assigned yet" : String.join(", ", subjects);

        String facultyId = me.isNull("faculty_id") ? "" : String.valueOf(me.optInt("faculty_id"));
        return new Teacher(facultyId,
                full.isEmpty() ? text(u, "username") : full,
                last.isEmpty() ? full : last,
                initials(first, last),
                text(u, "username"),
                text(u, "email"),
                subject);
    }

    /** "sections" from me.php → the sections of each program, e.g. BSMT → [BSMT-A1, BSMT-A2]. */
    public static Map<Program, List<String>> sections(JSONArray rows) {
        Map<Program, List<String>> out = new EnumMap<>(Program.class);
        for (Program p : Program.values()) out.put(p, new ArrayList<String>());
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r == null) continue;
            String program = text(r, "program").toUpperCase();
            for (Program p : Program.values()) {
                if (p.code.equals(program)) out.get(p).add(text(r, "name"));
            }
        }
        return out;
    }

    // ---------- Classes ----------

    /** "data" from my_schedule.php → weekly classes (students: their section; faculty: what they teach). */
    public static List<ClassSession> sessions(JSONArray rows) {
        List<ClassSession> out = new ArrayList<>();
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r == null) continue;
            int day = dayIndex(text(r, "day_of_week"));
            if (day < 0) continue;
            out.add(session(r, day));
        }
        return out;
    }

    /** "classes" from rooms_today.php → today's classes in every room. */
    public static List<ClassSession> todaysClasses(JSONArray rows) {
        List<ClassSession> out = new ArrayList<>();
        int today = Clock.todayIndex();
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r != null) out.add(session(r, today));
        }
        return out;
    }

    private static ClassSession session(JSONObject r, int day) {
        String instructor = text(r, "faculty_name");
        String section = text(r, "section_name");
        String building = text(r, "building");
        return new ClassSession(
                "db" + r.optInt("id"),
                text(r, "subject_code"),
                text(r, "subject_name"),
                day,
                hhmm(text(r, "start_time")),
                hhmm(text(r, "end_time")),
                text(r, "room_name"),
                building.isEmpty() ? "Tech Hub" : building,
                r.isNull("faculty_id") ? "" : String.valueOf(r.optInt("faculty_id")),
                instructor.isEmpty() ? "TBA" : instructor,
                Program.of(text(r, "program"), section),
                section);
    }

    // ---------- Rooms ----------

    /**
     * "rooms" + "classes" from rooms_today.php → rooms with live status: a room
     * with a class going on right now is shown as Occupied.
     */
    public static List<Room> rooms(JSONArray rows, List<ClassSession> today) {
        List<Room> out = new ArrayList<>();
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r == null) continue;
            String code = text(r, "room_name");
            int floor = r.optInt("floor");
            RoomStatus status = RoomStatus.of(text(r, "status"));
            if (status != RoomStatus.MAINTENANCE) {
                for (ClassSession s : today) {
                    if (s.room.equals(code) && Clock.isOngoing(s)) { status = RoomStatus.OCCUPIED; break; }
                }
            }
            out.add(new Room(code, text(r, "building"), floor, r.optInt("capacity"), status,
                    RoomCatalog.typeOf(code, floor), RoomCatalog.departmentOf(code, floor)));
        }
        return out;
    }

    // ---------- Notifications ----------

    /** "data" array from notifications.php → the student's alerts (newest first). */
    public static List<AppAlert> alerts(JSONArray rows, Program program) {
        List<AppAlert> out = new ArrayList<>();
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r == null) continue;
            out.add(new AppAlert(
                    String.valueOf(r.optInt("id")),
                    program,
                    "transfer".equals(text(r, "kind")) ? AppAlert.TRANSFER : AppAlert.ANNOUNCEMENT,
                    text(r, "title"),
                    text(r, "body"),
                    text(r, "from"),
                    timeLabel(text(r, "created_at")),
                    r.optBoolean("unread", false)));
        }
        return out;
    }

    /** "2026-10-10T09:14:20Z" → "Just now", "5 min ago", "7:15 AM", "Yesterday", "Oct 3". */
    public static String timeLabel(String iso) {
        try {
            ZonedDateTime t = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault());
            ZonedDateTime now = ZonedDateTime.now();
            long minutes = java.time.Duration.between(t, now).toMinutes();
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " min ago";
            LocalDate day = t.toLocalDate();
            if (day.equals(now.toLocalDate())) return t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US));
            if (day.equals(now.toLocalDate().minusDays(1))) return "Yesterday";
            return t.format(DateTimeFormatter.ofPattern("MMM d", Locale.US));
        } catch (Exception e) {
            return "";
        }
    }

    // ---------- helpers ----------

    private static int dayIndex(String day) {
        String d = day.trim().toLowerCase();
        for (int i = 0; i < DAYS.length; i++) {
            if (d.equals(DAYS[i]) || (d.length() >= 3 && DAYS[i].startsWith(d))) return i;
        }
        return -1;
    }

    /** "08:00:00" → "08:00" */
    private static String hhmm(String t) {
        return t.length() >= 5 ? t.substring(0, 5) : t;
    }

    private static String initials(String first, String last) {
        String a = first.isEmpty() ? "" : first.substring(0, 1);
        String b = last.isEmpty() ? "" : last.substring(0, 1);
        String s = (a + b).toUpperCase();
        return s.isEmpty() ? "U" : s;
    }

    private static String orDash(String s) {
        return s.isEmpty() ? "—" : s;
    }

    static String text(JSONObject o, String key) {
        if (o == null || o.isNull(key)) return "";
        return o.optString(key, "").trim();
    }
}
