package com.uniroom.app.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Talks to the UNIROOM API on Vercel (the same API the admin website uses).
 *
 * Accounts are created by the admin on the website. Students and faculty
 * sign in here with their username (or email) and password; the API answers
 * with their details and a login token. The token is sent with every
 * request after sign-in ("Authorization: Bearer ..."), so the server only
 * returns the signed-in user's own schedule and notifications.
 *
 * Uses only the standard Android classes (HttpURLConnection + org.json), so
 * no extra library is needed. Every call runs in the background and the
 * answer is delivered on the main (UI) thread.
 */
public final class ApiClient {

    /** Address of the API. Must end with "/". */
    public static final String BASE_URL = "https://uniroom-api.vercel.app/api/";

    /** The first request after the API has been idle can take a few seconds. */
    private static final int TIMEOUT_MS = 30000;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ApiClient() {}

    /** Result of a call: the JSON the server sent back, or an error message. */
    public interface Callback {
        void done(JSONObject json, String error);
    }

    /** Raw result of a request (used by the background notification check). */
    public static final class Result {
        public final int status;
        public final JSONObject json;
        public final String error;

        /** True when the server could not be reached at all (no internet, timeout). */
        public boolean offline() { return json == null && status == 0; }

        Result(int status, JSONObject json, String error) {
            this.status = status;
            this.json = json;
            this.error = error;
        }
    }

    // ---------- Sign in ----------

    /** POST login.php { username, password } – username can also be the email. */
    public static void login(String username, String password, Callback cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (Exception ignored) { }
        async("POST", "login.php", body, null, cb);
    }

    /** POST change_password.php – first sign-in with a temporary password. */
    public static void changePassword(String login, String current, String next, Callback cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("login", login);
            body.put("current_password", current);
            body.put("new_password", next);
        } catch (Exception ignored) { }
        async("POST", "change_password.php", body, null, cb);
    }

    // ---------- Signed-in user (student or faculty) ----------

    /** GET me.php – the signed-in user; for faculty also their subjects. Includes every section. */
    public static void me(String token, Callback cb) {
        async("GET", "me.php", null, token, cb);
    }

    /** GET my_schedule.php – a student's section classes, or every class a faculty member teaches. */
    public static void mySchedule(String token, Callback cb) {
        async("GET", "my_schedule.php", null, token, cb);
    }

    /** GET rooms_today.php – every room and the classes held in it today (live availability). */
    public static void roomsToday(String token, Callback cb) {
        String day = Clock.dayLong(Clock.todayIndex());
        async("GET", "rooms_today.php?day=" + day, null, token, cb);
    }

    // ---------- Notifications ----------

    /** GET notifications.php – the signed-in student's notifications (newest first). */
    public static void notifications(String token, Callback cb) {
        async("GET", "notifications.php", null, token, cb);
    }

    /** Same as notifications(), but waits for the answer (call it from a background thread). */
    public static Result notificationsNow(String token) {
        return request("GET", "notifications.php", null, token);
    }

    /** POST notifications_read.php { id } */
    public static void markRead(String token, int id, Callback cb) {
        JSONObject body = new JSONObject();
        try { body.put("id", id); } catch (Exception ignored) { }
        async("POST", "notifications_read.php", body, token, cb);
    }

    /** POST notifications_read.php { all: true } */
    public static void markAllRead(String token, Callback cb) {
        JSONObject body = new JSONObject();
        try { body.put("all", true); } catch (Exception ignored) { }
        async("POST", "notifications_read.php", body, token, cb);
    }

    /**
     * Faculty: POST notify_send.php – send a room change or an announcement to
     * the students of one course. section = "" sends it to every section.
     */
    public static void sendNotification(String token, String program, String section, String kind,
                                        String title, String text, Callback cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("program", program);
            body.put("section", section);
            body.put("kind", kind);
            body.put("title", title);
            body.put("body", text);
        } catch (Exception ignored) { }
        async("POST", "notify_send.php", body, token, cb);
    }

    // ---------- Plumbing ----------

    private static void async(String method, String path, JSONObject body, String token, Callback cb) {
        new Thread(() -> {
            Result r = request(method, path, body, token);
            MAIN.post(() -> cb.done(r.json, r.error));
        }).start();
    }

    /** GET request that waits for the answer (call it from a background thread). */
    public static Result getNow(String path, String token) {
        return request("GET", path, null, token);
    }

    /** Sends one request and waits for the answer. Never call this on the main thread. */
    private static Result request(String method, String path, JSONObject body, String token) {
        HttpURLConnection c = null;
        int code = 0;
        try {
            c = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                c.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

            code = c.getResponseCode();
            // Error answers (401 wrong password, 422 ...) still carry a JSON message.
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            String text = in == null ? "" : readAll(in);

            try {
                return new Result(code, new JSONObject(text), null);
            } catch (Exception notJson) {
                return new Result(code, null, "Server error (" + code + "). Please try again.");
            }
        } catch (java.net.SocketTimeoutException e) {
            return new Result(code, null, "The server took too long to answer. Please try again.");
        } catch (Exception e) {
            return new Result(code, null, "Unable to connect to the server. Check your internet connection.");
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /** "success": true in the server's answer. */
    public static boolean ok(JSONObject json) {
        return json != null && json.optBoolean("success", false);
    }

    /** The server's message, or a fallback. */
    public static String message(JSONObject json, String error, String fallback) {
        if (error != null) return error;
        String m = json == null ? "" : json.optString("message", "");
        return m.isEmpty() ? fallback : m;
    }

    /** True when the server says the login token is missing or expired. */
    public static boolean sessionExpired(JSONObject json) {
        if (json == null || json.optBoolean("success", true)) return false;
        String m = json.optString("message", "").toLowerCase();
        return m.contains("session has expired") || m.contains("log in again");
    }

    /** "data": [...] from a list answer (never null). */
    public static JSONArray data(JSONObject json) {
        JSONArray a = json == null ? null : json.optJSONArray("data");
        return a == null ? new JSONArray() : a;
    }
}
