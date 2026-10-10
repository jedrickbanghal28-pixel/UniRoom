package com.uniroom.app.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.uniroom.app.model.ClassSession;

/**
 * Time helpers.
 *
 * The prototype uses a fixed "demo clock" (Wednesday 23 September 2026, 7:40 AM) so that the
 * home screens always have classes to show. Set USE_DEMO_CLOCK to false to use the phone's real time.
 */
public final class Clock {

    public static final boolean USE_DEMO_CLOCK = true;

    private Clock() {}

    /** Day of the week, 0 = Monday ... 6 = Sunday. */
    public static int todayIndex() {
        if (USE_DEMO_CLOCK) return 2;
        int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // Sunday = 1
        return (d + 5) % 7;
    }

    /** Minutes since midnight. */
    public static int nowMinutes() {
        if (USE_DEMO_CLOCK) return 7 * 60 + 40;
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    /** Monday of the current week. */
    public static Calendar weekStart() {
        Calendar c = Calendar.getInstance();
        if (USE_DEMO_CLOCK) {
            c.set(2026, Calendar.SEPTEMBER, 21, 0, 0, 0);
        } else {
            c.add(Calendar.DAY_OF_MONTH, -todayIndex());
        }
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** Date of a given day: weekOffset weeks from this week, dayIndex 0 = Monday. */
    public static Calendar dateOf(int weekOffset, int dayIndex) {
        Calendar c = weekStart();
        c.add(Calendar.DAY_OF_MONTH, weekOffset * 7 + dayIndex);
        return c;
    }

    public static Calendar today() { return dateOf(0, todayIndex()); }

    // ---------- Formatting ----------
    public static int toMinutes(String time) {
        String[] p = time.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    /** "13:00" -> "1:00 PM" */
    public static String formatTime(String time) {
        int m = toMinutes(time);
        int h = m / 60, min = m % 60;
        String period = h >= 12 ? "PM" : "AM";
        int hour = h % 12 == 0 ? 12 : h % 12;
        return String.format(Locale.US, "%d:%02d %s", hour, min, period);
    }

    public static String formatRange(String start, String end) {
        return formatTime(start) + " – " + formatTime(end);
    }

    public static String formatRelative(int minutes) {
        if (minutes <= 0) return "Happening now";
        if (minutes < 60) return "Starts in " + minutes + " min";
        int h = minutes / 60, m = minutes % 60;
        return m != 0 ? "Starts in " + h + "h " + m + "m" : "Starts in " + h + "h";
    }

    private static final String[] DAY_LONG = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final String[] DAY_SHORT = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};

    public static String dayShort(int dayIndex) { return DAY_SHORT[dayIndex]; }

    public static String dayLong(int dayIndex) { return DAY_LONG[dayIndex]; }

    private static int dayIndexOf(Calendar c) { return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; }

    /** "Wednesday, 23 September" */
    public static String longDate(Calendar c) {
        return DAY_LONG[dayIndexOf(c)] + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)];
    }

    /** "September 2026" */
    public static String monthYear(Calendar c) {
        return MONTHS[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
    }

    // ---------- Schedule helpers ----------
    public static List<ClassSession> sessionsForDay(List<ClassSession> sessions, int day) {
        List<ClassSession> out = new ArrayList<>();
        for (ClassSession s : sessions) if (s.day == day) out.add(s);
        Collections.sort(out, (a, b) -> toMinutes(a.start) - toMinutes(b.start));
        return out;
    }

    /** The next class today that hasn't ended yet, or null. */
    public static ClassSession nextSession(List<ClassSession> sessions) {
        for (ClassSession s : sessionsForDay(sessions, todayIndex())) {
            if (toMinutes(s.end) > nowMinutes()) return s;
        }
        return null;
    }

    public static boolean isOngoing(ClassSession s) {
        int now = nowMinutes();
        return s.day == todayIndex() && toMinutes(s.start) <= now && toMinutes(s.end) > now;
    }

    public static String greeting() {
        int h = nowMinutes() / 60;
        if (h < 12) return "Good morning";
        if (h < 18) return "Good afternoon";
        return "Good evening";
    }
}
