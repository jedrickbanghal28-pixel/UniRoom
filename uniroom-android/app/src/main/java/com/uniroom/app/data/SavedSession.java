package com.uniroom.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Remembers who is signed in, on the phone (SharedPreferences), so that:
 *  - the app opens straight to the dashboard after it was closed,
 *  - the background check can fetch new notifications with the login token, and
 *  - the last schedule, rooms and notifications can still be shown when the
 *    phone has no internet (offline copy).
 * Everything is cleared on Log out.
 */
public final class SavedSession {

    private static final String FILE = "uniroom_session";

    /** Offline copies of the last answers from the server. */
    public static final String CACHE_ME = "cache_me";
    public static final String CACHE_SCHEDULE = "cache_schedule";
    public static final String CACHE_ROOMS = "cache_rooms";
    public static final String CACHE_ALERTS = "cache_alerts";

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
        return read(prefs(c).getString("user", null));
    }

    public static String token(Context c) {
        return prefs(c).getString("token", null);
    }

    public static boolean isStudent(Context c) {
        JSONObject u = user(c);
        return u != null && "student".equalsIgnoreCase(u.optString("role"));
    }

    // ---------- Offline copy ----------

    public static void putCache(Context c, String key, JSONObject json) {
        if (json != null) prefs(c).edit().putString(key, json.toString()).putLong("cache_time", System.currentTimeMillis()).apply();
    }

    /** The saved answer, or null if there is none. */
    public static JSONObject cache(Context c, String key) {
        return read(prefs(c).getString(key, null));
    }

    /** When the offline copy was last updated (0 = never). */
    public static long cacheTime(Context c) {
        return prefs(c).getLong("cache_time", 0);
    }

    // ---------- Phone notifications ----------

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

    private static JSONObject read(String s) {
        if (s == null) return null;
        try { return new JSONObject(s); } catch (Exception e) { return null; }
    }
}
