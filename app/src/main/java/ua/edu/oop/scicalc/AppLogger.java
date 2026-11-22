package ua.edu.oop.scicalc;

import android.util.Log;

/** Centralized, level-aware Android Logcat logger. Never logs user expressions. */
public final class AppLogger {
    private static final String TAG = "SciCalc";
    private AppLogger() {}

    public static void info(String message) { Log.i(TAG, message); }
    public static void warn(String message) { Log.w(TAG, message); }
    public static void error(String message, Throwable cause) { Log.e(TAG, message, cause); }
}
