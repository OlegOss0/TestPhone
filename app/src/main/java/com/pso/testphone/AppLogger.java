package com.pso.testphone;

import android.os.Build;
import android.util.Log;

import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.PreferenceManager;
import com.pso.testphone.gui.MainActivityPresenter;

public class AppLogger {

    public static void e(String TAG, String msg) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, msg);
        }
    }

    public static void e(String TAG, String msg, Exception ex) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, msg, ex);
        }
    }

    public static void i(String TAG, String msg) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, msg);
        }
    }

    public static void witnAdditionalMsg(String msg) {
        if (BuildConfig.DEBUG || DataStorage.isAdditionalLoggingEnabled()) {
            MainActivityPresenter.addMsg(true, msg);
        }
    }

    public static void printStackTrace(Exception e) {
        if (BuildConfig.DEBUG || DataStorage.isAdditionalLoggingEnabled()) {
            StackTraceElement[] stackTraceElements = e.getStackTrace();
            MainActivityPresenter.addMsg(true, e.getMessage());
            for (int i = 0; i < stackTraceElements.length; i++) {
                MainActivityPresenter.addMsg(true, "Class " + stackTraceElements[i].getClassName() + ", Method " + stackTraceElements[i].getMethodName() + "," + stackTraceElements[i].getLineNumber());
            }
        }
        e.printStackTrace();
    }
}
