package com.uniroom.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Remembers who is signed in, on the phone (SharedPreferences), so that:
 *  - the app opens straight to the dashboard after it was closed, and
 *  - the background check can fetch new notifications with the login token.
 * Cleared on Log out.
 */
public final class SavedSession {

    private static final String FILE = "uniroom_session";

    private SavedSession() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Saves the "user" object from login.php and the login token. */
    public static void save(Context c, JSONObject user, String token) {
        prefs(c).edit()
                .putString("user", user.toString())
                .putString("token", token)
                .apply();
    }

    /** The saved "user" object, or null when nobody is signed in. */
    public static JSONObject user(Context c) {
        String s = prefs(c).getString("user", null);
        if (s == null) return null;
        try { return new JSONObject(s); } catch (Exception e) { return null; }
    }

    public static String token(Context c) {
        return prefs(c).getString("token", null);
    }

    public static boolean isStudent(Context c) {
        JSONObject u = user(c);
        return u != null && "student".equalsIgnoreCase(u.optString("role"));
    }

    /** Highest notification id already shown as a phone notification. */
    public static int lastNotifiedId(Context c) {
        return prefs(c).getInt("last_notified_id", 0);
    }

    public static void setLastNotifiedId(Context c, int id) {
        prefs(c).edit().putInt("last_notified_id", id).apply();
    }

    public static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }
}
