package com.uniroom.app.notify;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import com.uniroom.app.data.ApiClient;
import com.uniroom.app.data.SavedSession;

/**
 * Checks for new notifications in the background (about every 15 minutes,
 * the shortest interval Android allows), even when the app is closed.
 * Runs only while a student is signed in.
 */
public class NotificationCheckJob extends JobService {

    private static final int JOB_ID = 2026;

    /** Starts the background check (called after a student signs in). */
    public static void schedule(Context c) {
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(c, NotificationCheckJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)   // keeps running after the phone restarts
                .build();
        c.getSystemService(JobScheduler.class).schedule(job);
    }

    /** Stops the background check (called on Log out). */
    public static void cancel(Context c) {
        c.getSystemService(JobScheduler.class).cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        final Context c = getApplicationContext();
        final String token = SavedSession.token(c);
        if (token == null || !SavedSession.isStudent(c)) {
            cancel(c);
            return false;
        }
        new Thread(() -> {
            ApiClient.Result r = ApiClient.notificationsNow(token);
            if (ApiClient.ok(r.json)) {
                Notifier.showNew(c, ApiClient.data(r.json), false);
            } else if (r.status == 401) {
                // Login expired: stop checking until the student signs in again.
                cancel(c);
            }
            jobFinished(params, false);
        }).start();
        return true;   // still working in the thread
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;   // try again later
    }
}
