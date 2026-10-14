package com.uniroom.app.notify;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.uniroom.app.R;
import com.uniroom.app.data.SavedSession;
import com.uniroom.app.screens.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Shows new faculty notifications as phone notifications (status bar).
 * A notification is shown once: the highest id already shown is remembered.
 */
public final class Notifier {

    public static final String CHANNEL_ID = "uniroom_alerts";
    private static final int PERMISSION_REQUEST = 41;

    private Notifier() {}

    /** Android 13+ asks the user before an app may show notifications. */
    public static void askPermission(Activity a) {
        if (Build.VERSION.SDK_INT >= 33
                && a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST);
        }
    }

    private static void createChannel(Context c) {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Room changes and announcements",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Notifications your instructors send to your section.");
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    /**
     * Looks at the "data" list from notifications.php and shows every unread
     * notification newer than the last one shown.
     *
     * @param firstRun true right after sign-in: remember what is there without
     *                 showing it (the student sees it in the app anyway).
     */
    public static void showNew(Context c, JSONArray rows, boolean firstRun) {
        int last = SavedSession.lastNotifiedId(c);
        int highest = last;

        for (int i = rows.length() - 1; i >= 0; i--) {   // oldest first
            JSONObject r = rows.optJSONObject(i);
            if (r == null) continue;
            int id = r.optInt("id");
            if (id <= last) continue;
            highest = Math.max(highest, id);
            if (!firstRun && r.optBoolean("unread", false)) {
                show(c, id, r.optString("title"), r.optString("body"), r.optString("from"));
            }
        }
        if (highest != last) SavedSession.setLastNotifiedId(c, highest);
    }

    private static void show(Context c, int id, String title, String body, String from) {
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createChannel(c);

        Intent open = new Intent(c, LoginActivity.class);
        open.putExtra(LoginActivity.EXTRA_OPEN_ALERTS, true);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = body + (from.isEmpty() ? "" : "\n— " + from);
        Notification n = new Notification.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(id, n);
        } catch (RuntimeException e) {
            android.util.Log.e("UniRoom", "Could not show the phone notification", e);
        }
    }
}
