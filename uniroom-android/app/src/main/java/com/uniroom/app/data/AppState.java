package com.uniroom.app.data;

import com.uniroom.app.model.Account;
import com.uniroom.app.model.AppAlert;
import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Program;
import com.uniroom.app.model.Student;
import com.uniroom.app.model.Teacher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the app remembers while it is running: who is signed in (with
 * their login token), the student's classes and notifications from the
 * database, the faculty program they are looking at and their rooms.
 */
public final class AppState {

    private static AppState instance;

    public static AppState get() {
        if (instance == null) instance = new AppState();
        return instance;
    }

    private Account account;
    /** Login token from the API (sent with notification requests). */
    private String token;
    /** Database id of the signed-in user. */
    private int userId;
    /** The student's classes, loaded from the database. */
    private List<ClassSession> studentSessions = new ArrayList<>();
    /** The student's notifications, loaded from the database (newest first). */
    private List<AppAlert> alerts = new ArrayList<>();

    private Program facultyProgram = Program.BSIT;
    private Map<Program, List<String>> assignedRooms = emptyAssignments();

    private AppState() {}

    // ---------- Sign in / out ----------
    public Account getAccount() { return account; }

    public boolean isSignedIn() { return account != null; }

    public String getToken() { return token; }

    public int getUserId() { return userId; }

    /** Student account created by the admin, signed in through the API. */
    public void signInStudent(Student s, int databaseId, String loginToken, List<ClassSession> sessions) {
        account = Account.of(s);
        userId = databaseId;
        token = loginToken;
        studentSessions = sessions == null ? new ArrayList<ClassSession>() : sessions;
        alerts = new ArrayList<>();
    }

    /** Faculty account created by the admin, signed in through the API. */
    public void signInFaculty(Teacher t, int databaseId, String loginToken) {
        account = Account.of(t);
        userId = databaseId;
        token = loginToken;
        studentSessions = new ArrayList<>();
        alerts = new ArrayList<>();
        facultyProgram = Program.BSIT;
        assignedRooms = DemoData.startingRoomsFor(t.id);
    }

    public void logout() {
        account = null;
        token = null;
        userId = 0;
        studentSessions = new ArrayList<>();
        alerts = new ArrayList<>();
    }

    private static Map<Program, List<String>> emptyAssignments() {
        Map<Program, List<String>> m = new HashMap<>();
        m.put(Program.BSIT, new ArrayList<String>());
        m.put(Program.BSMT, new ArrayList<String>());
        return m;
    }

    // ---------- Faculty program switcher ----------
    public Program getFacultyProgram() { return facultyProgram; }

    public void setFacultyProgram(Program p) { facultyProgram = p; }

    // ---------- Timetable ----------
    /** Classes of the signed-in student's section, or of the teacher for the current portal. */
    public List<ClassSession> mySessions() {
        List<ClassSession> out = new ArrayList<>();
        if (account == null) return out;
        if (account.isStudent()) return new ArrayList<>(studentSessions);
        for (ClassSession s : DemoData.SESSIONS) {
            if (s.teacherId.equals(account.teacher.id) && s.program == facultyProgram) out.add(s);
        }
        return out;
    }

    // ---------- Faculty rooms ----------
    public List<String> assignedRooms() { return assignedRooms.get(facultyProgram); }

    public void addRoom(String code) {
        List<String> list = assignedRooms.get(facultyProgram);
        if (!list.contains(code)) list.add(code);
    }

    public void removeRoom(String code) { assignedRooms.get(facultyProgram).remove(code); }

    // ---------- Notifications (from the database) ----------
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
