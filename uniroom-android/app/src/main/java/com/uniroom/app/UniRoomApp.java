package com.uniroom.app;

import android.app.Application;

import com.uniroom.app.data.CrashLog;
import com.uniroom.app.data.SavedSession;

/**
 * Runs when the app starts. If the app ever closes because of an error, the
 * details are saved and the saved sign-in is cleared, so the next start opens
 * the sign-in screen (with the error details) instead of crashing again.
 */
public class UniRoomApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler system = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            CrashLog.record(this, error, true);
            try { SavedSession.clear(this); } catch (Throwable ignored) { }
            if (system != null) system.uncaughtException(thread, error);
            else System.exit(2);
        });
    }
}
