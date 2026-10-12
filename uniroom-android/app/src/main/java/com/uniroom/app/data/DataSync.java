package com.uniroom.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.uniroom.app.model.ClassSession;
import com.uniroom.app.model.Program;
import com.uniroom.app.model.Student;

import org.json.JSONObject;

import java.util.List;

/**
 * Loads the signed-in user's data from the server into AppState:
 *  me.php          – who they are (+ faculty subjects, sections of each program)
 *  my_schedule.php – their weekly classes
 *  rooms_today.php – every room and today's classes in it (live availability)
 *  notifications   – students only
 *
 * Each answer is also saved on the phone. If the server cannot be reached
 * (no internet), the saved copy is shown instead and AppState.isOffline()
 * is true until the next successful refresh.
 */
public final class DataSync {

    private DataSync() {}

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static final int OK = 0, OFFLINE = 1, EXPIRED = 2, FAILED = 3;

    public interface Done {
        /** @param result OK, OFFLINE (showing the saved copy), EXPIRED (sign in again) or FAILED */
        void done(int result, String message);
    }

    /**
     * Sign-in: loads everything and fills in AppState. If the phone is offline
     * and this user's data was saved before, the saved copy is used.
     */
    public static void signIn(Context context, JSONObject loginUser, String token, Done done) {
        final Context c = context.getApplicationContext();
        new Thread(() -> {
            ApiClient.Result me = ApiClient.getNow("me.php", token);
            if (me.offline()) {
                JSONObject cachedMe = SavedSession.cache(c, SavedSession.CACHE_ME);
                boolean sameUser = cachedMe != null && cachedMe.optJSONObject("user") != null
                        && cachedMe.optJSONObject("user").optInt("id") == loginUser.optInt("id");
                if (sameUser) {
                    post(() -> {
                        applyMe(cachedMe, token);
                        applyCache(c);
                        pickFacultyProgram();
                        AppState.get().setOffline(true);
                        done.done(OFFLINE, "You're offline. Showing your saved schedule.");
                    });
                } else {
                    post(() -> done.done(FAILED, me.error));
                }
                return;
            }
            if (ApiClient.sessionExpired(me.json) || me.status == 401) {
                post(() -> done.done(EXPIRED, "Please sign in again."));
                return;
            }
            if (!ApiClient.ok(me.json)) {
                post(() -> done.done(FAILED, ApiClient.message(me.json, me.error, "Unable to load your account.")));
                return;
            }
            Loaded data = loadRest(token, isStudent(me.json));
            if (data.error != null) {
                post(() -> done.done(FAILED, data.error));
                return;
            }
            post(() -> {
                SavedSession.putCache(c, SavedSession.CACHE_ME, me.json);
                applyMe(me.json, token);
                apply(c, data);
                pickFacultyProgram();
                done.done(OK, null);
            });
        }).start();
    }

    /** Refresh while the app is open (schedule, rooms and notifications). Quietly keeps the old data if offline. */
    public static void refresh(Context context, Done done) {
        final Context c = context.getApplicationContext();
        final AppState state = AppState.get();
        final String token = state.getToken();
        if (token == null || state.getAccount() == null) return;
        final boolean student = state.getAccount().isStudent();
        new Thread(() -> {
            Loaded data = loadRest(token, student);
            post(() -> {
                if (AppState.get().getToken() == null || !token.equals(AppState.get().getToken())) return; // signed out meanwhile
                if (data.expired) { done.done(EXPIRED, "Please sign in again."); return; }
                if (data.offline) { state.setOffline(true); done.done(OFFLINE, null); return; }
                if (data.error != null) { done.done(FAILED, data.error); return; }
                apply(c, data);
                done.done(OK, null);
            });
        }).start();
    }

    // ---------- internals ----------

    /** Answers of my_schedule, rooms_today and notifications. */
    private static final class Loaded {
        JSONObject schedule, rooms, alerts;
        boolean offline, expired;
        String error;
    }

    private static Loaded loadRest(String token, boolean student) {
        Loaded d = new Loaded();
        ApiClient.Result s = ApiClient.getNow("my_schedule.php", token);
        ApiClient.Result r = check(d, s) ? ApiClient.getNow("rooms_today.php?day="
                + Clock.dayLong(Clock.todayIndex()), token) : null;
        ApiClient.Result a = r != null && check(d, r) && student ? ApiClient.notificationsNow(token) : null;
        if (a != null) check(d, a);
        if (d.offline || d.expired || d.error != null) return d;
        d.schedule = s.json;
        d.rooms = r.json;
        d.alerts = a == null ? null : a.json;
        return d;
    }

    /** True if the answer is usable; otherwise records why not. */
    private static boolean check(Loaded d, ApiClient.Result x) {
        if (x.offline()) { d.offline = true; return false; }
        if (x.status == 401 || ApiClient.sessionExpired(x.json)) { d.expired = true; return false; }
        if (!ApiClient.ok(x.json)) { d.error = ApiClient.message(x.json, x.error, "Unable to load your data."); return false; }
        return true;
    }

    private static boolean isStudent(JSONObject me) {
        JSONObject u = me.optJSONObject("user");
        return u != null && "student".equalsIgnoreCase(u.optString("role"));
    }

    /** Signs the account in from a me.php answer. */
    private static void applyMe(JSONObject me, String token) {
        JSONObject u = me.optJSONObject("user");
        AppState state = AppState.get();
        if (isStudent(me)) state.signInStudent(ApiMapper.student(u), u.optInt("id"), token);
        else state.signInFaculty(ApiMapper.teacher(me), u.optInt("id"), token);
        state.setSections(ApiMapper.sections(me.optJSONArray("sections")));
    }

    private static void apply(Context c, Loaded d) {
        SavedSession.putCache(c, SavedSession.CACHE_SCHEDULE, d.schedule);
        SavedSession.putCache(c, SavedSession.CACHE_ROOMS, d.rooms);
        if (d.alerts != null) SavedSession.putCache(c, SavedSession.CACHE_ALERTS, d.alerts);
        applyData(d.schedule, d.rooms, d.alerts);
        AppState.get().setOffline(false);
    }

    private static void applyCache(Context c) {
        applyData(SavedSession.cache(c, SavedSession.CACHE_SCHEDULE),
                SavedSession.cache(c, SavedSession.CACHE_ROOMS),
                SavedSession.cache(c, SavedSession.CACHE_ALERTS));
    }

    private static void applyData(JSONObject schedule, JSONObject rooms, JSONObject alerts) {
        AppState state = AppState.get();
        if (schedule != null) state.setSessions(ApiMapper.sessions(ApiClient.data(schedule)));
        if (rooms != null) {
            List<ClassSession> today = ApiMapper.todaysClasses(rooms.optJSONArray("classes"));
            state.setRooms(ApiMapper.rooms(rooms.optJSONArray("rooms"), today), today);
        }
        if (alerts != null && state.getAccount() != null && state.getAccount().isStudent()) {
            Student s = state.getAccount().student;
            Program p = s.program;
            state.setAlerts(ApiMapper.alerts(ApiClient.data(alerts), p));
        }
    }

    /** Faculty: start on a program they actually teach in. */
    private static void pickFacultyProgram() {
        AppState state = AppState.get();
        if (state.getAccount() == null || !state.getAccount().isFaculty()) return;
        List<Program> taught = state.programsTaught();
        if (!taught.isEmpty() && !taught.contains(state.getFacultyProgram())) state.setFacultyProgram(taught.get(0));
    }

    private static void post(Runnable r) {
        MAIN.post(r);
    }
}
