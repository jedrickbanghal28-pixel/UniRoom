package com.uniroom.app.data;

import com.uniroom.app.model.AppAlert;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Program;
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
import java.util.List;
import java.util.Locale;

/**
 * Turns the API's answers into the app's own objects (Student, ClassSession),
 * so every screen keeps working the same way it did with the sample data.
 */
public final class StudentMapper {

    private StudentMapper() {}

    private static final String[] DAYS =
            {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

    /** "user" object from login.php → Student. */
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
                program(text(u, "program"), section),
                section.isEmpty() ? "No section yet" : section);
    }

    /** "BSIT" → BSIT, "BSMT" / "BSMEDTECH" → BSMT. The section name is used if the code is missing. */
    public static Program program(String code, String section) {
        String c = (code + " " + section).toUpperCase();
        if (c.contains("BSMT") || c.contains("MEDTECH") || c.contains("MED")) return Program.BSMT;
        return Program.BSIT;
    }

    /** "data" array from schedule.php → the student's weekly classes. */
    public static List<ClassSession> sessions(JSONArray rows, Student student) {
        List<ClassSession> out = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r == null) continue;
            int day = dayIndex(text(r, "day_of_week"));
            if (day < 0) continue;

            String instructor = text(r, "faculty_name");
            out.add(new ClassSession(
                    "db" + r.optInt("id"),
                    text(r, "subject_code"),
                    text(r, "subject_name"),
                    day,
                    hhmm(text(r, "start_time")),
                    hhmm(text(r, "end_time")),
                    text(r, "room_name"),
                    text(r, "building"),
                    teacherIdFor(instructor),
                    instructor.isEmpty() ? "TBA" : instructor,
                    student.program,
                    student.section));
        }
        return out;
    }

    /** "data" array from notifications.php → the student's alerts (newest first). */
    public static List<AppAlert> alerts(JSONArray rows, Program program) {
        List<AppAlert> out = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
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

    /**
     * Faculty account from login.php → one of the three faculty in the app,
     * matched by email, then username, then surname. Null if none match.
     */
    public static Teacher teacher(JSONObject u) {
        String email = text(u, "email");
        String username = text(u, "username");
        String last = text(u, "last_name");
        for (Teacher t : DemoData.TEACHERS) if (t.email.equalsIgnoreCase(email)) return t;
        for (Teacher t : DemoData.TEACHERS) if (t.username.equalsIgnoreCase(username)) return t;
        for (Teacher t : DemoData.TEACHERS) if (!last.isEmpty() && t.lastName.equalsIgnoreCase(last)) return t;
        return null;
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

    /** Links a class to one of the three faculty so their details show in the app. */
    private static String teacherIdFor(String name) {
        for (Teacher t : DemoData.TEACHERS) {
            if (t.fullName.equalsIgnoreCase(name.trim())) return t.id;
        }
        return "";
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

    private static String text(JSONObject o, String key) {
        if (o == null || o.isNull(key)) return "";
        return o.optString(key, "").trim();
    }
}
