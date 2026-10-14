package com.uniroom.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Keeps the details of the last error on the phone, so that if the app ever
 * closes unexpectedly it can show what happened the next time it opens
 * (instead of crashing again with the same saved data).
 */
public final class CrashLog {

    private static final String FILE = "uniroom_crash";

    private CrashLog() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Saves an error. fatal = the app is about to close. */
    public static void record(Context c, Throwable t, boolean fatal) {
        try {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String when = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            String text = "UniRoom 2.4 · Android " + Build.VERSION.RELEASE + " · " + Build.MANUFACTURER + " " + Build.MODEL
                    + "\n" + when + (fatal ? " (app closed)" : "") + "\n\n" + sw;
            if (text.length() > 6000) text = text.substring(0, 6000);
            // commit(): the app may be closing, so write it now
            prefs(c).edit().putString("last", text).putBoolean("fatal", fatal).commit();
        } catch (Throwable ignored) { }
    }

    /** The saved error text, or null. */
    public static String pending(Context c) {
        return prefs(c).getString("last", null);
    }

    public static boolean wasFatal(Context c) {
        return prefs(c).getBoolean("fatal", false);
    }

    public static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }
}
