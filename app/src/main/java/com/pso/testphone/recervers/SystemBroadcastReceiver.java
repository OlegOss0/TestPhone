package com.pso.testphone.recervers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.pso.testphone.App;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.services.MainService;

import java.util.Calendar;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;
import static com.pso.testphone.data.DataStorage.TP_ASSISTANT_PACKAGE_SERVICE_CLASS;


public class SystemBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = SystemBroadcastReceiver.class.getSimpleName();
    public static final String ALARM_EXTRA = "alarmIntent";
    public static final int MINUTE_ALARM_CODE = 1;
    public static final int MINUTE2_ALARM_CODE = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(ACTION_BOOT_COMPLETED)) {
                DataStorage.lastTimeShowRebootMsg.set(-1);
            }
            int alarmCode = intent.getIntExtra(ALARM_EXTRA, 0);
            if (alarmCode != 0) {
                sendBroadCastToAssistant(context);
                createAlarm(context, alarmCode, false);
                if (!isMainServiceRun()) {
                    startDataCollectorService(context);
                }
            }
            if (action.equals(ACTION_POWER_DISCONNECTED) || action.equals(ACTION_POWER_CONNECTED)) {
                if (!isMainServiceRun()) {
                    startDataCollectorService(context);
                }
            } else if (action.equals(ACTION_BOOT_COMPLETED)) {
                startDataCollectorService(context);
                createAlarm(context, MINUTE_ALARM_CODE, true);
            }
        }
    }

    public static void sendBroadCastToAssistant(Context context) {
        Log.e(TAG, "sendBroadCast alarmIntent");
        Intent i = new Intent("com.pso.cht_phandroidservice.MainReceiver.ACTION");
        i.setComponent(new ComponentName(DataStorage.TP_ASSISTANT_PACKAGE, TP_ASSISTANT_PACKAGE_SERVICE_CLASS));
        context.sendBroadcast(i);
    }

    public static void startDataCollectorService(Context context) {
        Intent startServiceIntent = new Intent(context, MainService.class);
        ContextCompat.startForegroundService(context, startServiceIntent);

    }

    public static void createAlarm(Context context, int code, boolean forceAll) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (forceAll) {
            Calendar c1 = calendar;
            c1.add(Calendar.MINUTE, MINUTE_ALARM_CODE);
            sendAlarm(MINUTE_ALARM_CODE, c1.getTimeInMillis());
            return;
        }
        switch (code) {
            case MINUTE_ALARM_CODE: {
                calendar.add(Calendar.MINUTE, code);
                break;
            }
            case MINUTE2_ALARM_CODE: {
                calendar.add(Calendar.MINUTE, code);
                calendar.add(Calendar.SECOND, 23);
                break;
            }
        }
        sendAlarm(code, calendar.getTimeInMillis());
    }

    private static void sendAlarm(int code, long time) {
        AlarmManager alarmManager = (AlarmManager) App.getContext().getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(App.getContext(), SystemBroadcastReceiver.class);
        alarmIntent.putExtra(ALARM_EXTRA, code);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getContext(), code, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC, time, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC, time, pendingIntent);
        }
    }

    private boolean isMainServiceRun() {
        return MainService.isRunning.get();
    }
}
