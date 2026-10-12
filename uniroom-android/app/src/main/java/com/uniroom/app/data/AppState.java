package com.uniroom.app.data;

import com.uniroom.app.model.Account;
import com.uniroom.app.model.AppAlert;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Program;
import com.uniroom.app.model.Room;
import com.uniroom.app.model.Student;
import com.uniroom.app.model.Teacher;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the app remembers while it is running: who is signed in (with
 * their login token) and what was loaded from the database for them – their
 * classes, the rooms with today's classes, the sections of each program and
 * (students) their notifications. DataSync fills it in.
 */
public final class AppState {

    private static AppState instance;

    public static AppState get() {
        if (instance == null) instance = new AppState();
        return instance;
    }

    private Account account;
    /** Login token from the API (sent with every request). */
    private String token;
    /** Database id of the signed-in user. */
    private int userId;

    /** Weekly classes: a student's section, or everything a faculty member teaches. */
    private List<ClassSession> sessions = new ArrayList<>();
    /** The student's notifications (newest first). */
    private List<AppAlert> alerts = new ArrayList<>();
    /** All rooms (live status) and the classes held in them today. */
    private List<Room> rooms = new ArrayList<>();
    private List<ClassSession> todayInRooms = new ArrayList<>();
    /** Sections of each program, from the database. */
    private Map<Program, List<String>> sections = emptySections();

    /** True while the app shows the offline copy because the server cannot be reached. */
    private boolean offline = false;

    private Program facultyProgram = Program.BSIT;

    private AppState() {}

    // ---------- Sign in / out ----------
    public Account getAccount() { return account; }

    public boolean isSignedIn() { return account != null; }

    public String getToken() { return token; }

    public int getUserId() { return userId; }

    public void signInStudent(Student s, int databaseId, String loginToken) {
        reset();
        account = Account.of(s);
        userId = databaseId;
        token = loginToken;
    }

    public void signInFaculty(Teacher t, int databaseId, String loginToken) {
        reset();
        account = Account.of(t);
        userId = databaseId;
        token = loginToken;
    }

    public void logout() {
        reset();
        account = null;
        token = null;
        userId = 0;
    }

    private void reset() {
        sessions = new ArrayList<>();
        alerts = new ArrayList<>();
        rooms = new ArrayList<>();
        todayInRooms = new ArrayList<>();
        sections = emptySections();
        offline = false;
        facultyProgram = Program.BSIT;
    }

    private static Map<Program, List<String>> emptySections() {
        Map<Program, List<String>> m = new EnumMap<>(Program.class);
        for (Program p : Program.values()) m.put(p, new ArrayList<String>());
        return m;
    }

    // ---------- Data from the server ----------
    public void setSessions(List<ClassSession> fresh) {
        sessions = fresh == null ? new ArrayList<ClassSession>() : new ArrayList<>(fresh);
    }

    public void setRooms(List<Room> fresh, List<ClassSession> today) {
        rooms = fresh == null ? new ArrayList<Room>() : new ArrayList<>(fresh);
        todayInRooms = today == null ? new ArrayList<ClassSession>() : new ArrayList<>(today);
    }

    public void setSections(Map<Program, List<String>> fresh) {
        if (fresh != null) sections = fresh;
    }

    public boolean isOffline() { return offline; }

    public void setOffline(boolean value) { offline = value; }

    // ---------- Faculty program switcher ----------
    public Program getFacultyProgram() { return facultyProgram; }

    public void setFacultyProgram(Program p) { facultyProgram = p; }

    /** Sections of a program, e.g. BSMT → [BSMT-A1, BSMT-A2]. */
    public List<String> sectionsOf(Program p) {
        return new ArrayList<>(sections.get(p));
    }

    // ---------- Timetable ----------
    /** Classes of the signed-in student's section, or of the teacher for the current program. */
    public List<ClassSession> mySessions() {
        List<ClassSession> out = new ArrayList<>();
        if (account == null) return out;
        if (account.isStudent()) return new ArrayList<>(sessions);
        for (ClassSession s : sessions) if (s.program == facultyProgram) out.add(s);
        return out;
    }

    /** Programs a faculty member teaches in (from their schedule). */
    public List<Program> programsTaught() {
        Set<Program> out = new LinkedHashSet<>();
        for (ClassSession s : sessions) out.add(s.program);
        return new ArrayList<>(out);
    }

    // ---------- Rooms ----------
    public List<Room> rooms() { return new ArrayList<>(rooms); }

    /** Classes held in any room today (for "Today in this room"). */
    public List<ClassSession> todayInRooms() { return new ArrayList<>(todayInRooms); }

    public Room findRoom(String code) {
        for (Room r : rooms) if (r.code.equals(code)) return r;
        return null;
    }

    /**
     * Faculty: the rooms they teach in for the current program. These come from
     * the schedule the admin set up, so they are the same on every phone.
     */
    public List<String> assignedRooms() {
        Set<String> codes = new LinkedHashSet<>();
        for (ClassSession s : mySessions()) codes.add(s.room);
        return new ArrayList<>(codes);
    }

    // ---------- Notifications ----------
    public List<AppAlert> myAlerts() {
        if (account == null || !account.isStudent()) return new ArrayList<>();
        return new ArrayList<>(alerts);
    }

    /** Replaces the list with what the server sent. */
    public void setAlerts(List<AppAlert> fresh) {
        alerts = fresh == null ? new ArrayList<AppAlert>() : new ArrayList<>(fresh);
    }

    public int unreadCount() {
        int n = 0;
        for (AppAlert a : alerts) if (a.unread) n++;
        return n;
    }

    /** Marks one alert read on the phone (the server is told separately). */
    public void markRead(String id) {
        for (AppAlert a : alerts) if (a.id.equals(id)) a.unread = false;
    }

    public void markAllRead() {
        for (AppAlert a : alerts) a.unread = false;
    }
}
