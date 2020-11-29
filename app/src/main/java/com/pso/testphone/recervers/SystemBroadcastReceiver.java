package com.pso.testphone.recervers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.pso.testphone.App;
import com.pso.testphone.AppLogger;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.services.MainService;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;
import static com.pso.testphone.data.DataStorage.TP_ASSISTANT_PACKAGE_SERVICE_CLASS;


public class SystemBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = SystemBroadcastReceiver.class.getSimpleName();
    public static final String ALARM_EXTRA = "alarmIntent";
    public static final int MINUTE_ALARM_CODE = 17752174;
    public static final String TIME_EXTRA = "time_extra";
    private static final String ACTION_FROM_ASSISTANT = "alarmIntentFromAssistant";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //Log.e(" MainReceiver", "TestPhone : onReceive = action -" + action );
        if (action != null) {
            if (action.equals(ACTION_BOOT_COMPLETED)) {
                startDataCollectorService(context);
                createAlarm();
                return;
            }
            int alarmCode = intent.getIntExtra(ALARM_EXTRA, -1);
            if(alarmCode != -1){
                sendBroadCastToAssistant(context);
                if(!isMainServiceRun()){
                    startDataCollectorService(context);
                }
                createAlarm();
                return;
            }

            if (action.equals(ACTION_POWER_DISCONNECTED) || action.equals(ACTION_POWER_CONNECTED)) {
                if (!isMainServiceRun()) {
                    startDataCollectorService(context);
                }
            }
        }
        if (!isMainServiceRun()) {
            startDataCollectorService(context);
        }
        /*String extraStr = intent.getStringExtra(CHECK_EXTRA);
        if(extraStr != null && extraStr.equals(CHECK_EXTRA)){
            startDataCollectorService(context);
        }*/
    }

    public static void sendBroadCastToAssistant(Context context) {
        //Log.e(TAG, "sendBroadCast alarmIntent");
        Intent i = new Intent("com.pso.cht_phandroidservice.MainReceiver.ACTION");
        i.putExtra(TIME_EXTRA, String.valueOf(DataStorage.getExchangeTime()));
        i.setComponent(new ComponentName(DataStorage.TP_ASSISTANT_PACKAGE, TP_ASSISTANT_PACKAGE_SERVICE_CLASS));
        context.sendBroadcast(i);
    }

    public static void startDataCollectorService(Context context) {
        Intent startServiceIntent = new Intent(context, MainService.class);
        ContextCompat.startForegroundService(context, startServiceIntent);

    }

    public static void createAlarm() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, 1);
        sendAlarm(MINUTE_ALARM_CODE, calendar.getTimeInMillis());
    }

    private static void sendAlarm(int code, long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH_mm_ss");
        AppLogger.e("Alarm", "Created to " + formatter.format(time));
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
