package com.pso.testphone.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pso.testphone.App;
import com.pso.testphone.AppLogger;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;
import com.pso.testphone.gui.DialogActivity;
import com.pso.testphone.gui.MainActivity;
import com.pso.testphone.gui.MainActivityPresenter;
import com.pso.testphone.PermissionHelper;
import com.pso.testphone.data.PreferenceManager;
import com.pso.testphone.network.RemoteServerHelper;
import com.pso.testphone.R;
import com.pso.testphone.db.Provider;
import com.pso.testphone.db.ProviderDao;
import com.pso.testphone.db.TimePoint;
import com.pso.testphone.db.TimePointDao;
import com.pso.testphone.gui.GuiHelper;
import com.pso.testphone.interfaces.ServerTaskListener;
import com.pso.testphone.location.LocationManager;
import com.pso.testphone.recervers.SystemBroadcastReceiver;


import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.pso.testphone.App.CHANNEL_ID;
import static com.pso.testphone.interfaces.ServerTaskListener.NEED_ENABLED_ALL_LOCATION;
import static com.pso.testphone.recervers.SystemBroadcastReceiver.sendBroadCastToAssistant;

public class MainService extends Service {
    public static AtomicBoolean isRunning = new AtomicBoolean(false);
    private HandlerThread serviceThread;
    private Handler serviceHandler;
    private LocationManager mLocationManager;
    private static IntentFilter intentFilter;
    private static final String TAG = MainService.class.getSimpleName();
    public final int CHECK_INSTALL_APPS_INTERVAL = 60 * 1000 * 5; //5 min
    private long lastCheckInstallAppTime = 0;
    private static final Object lock = new Object();
    private final int SEND_BROADCAST_TO_ASSISTANT_DELAY = 1500;
    private static boolean notificationIsNormal = false;

    static {
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_DATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        ;
    }

    Runnable b = new Runnable() {
        @Override
        public void run() {
            serviceHandler.removeCallbacksAndMessages(b);
            sendBroadCastToAssistant(App.getContext());
            serviceHandler.postDelayed(b, SEND_BROADCAST_TO_ASSISTANT_DELAY);
        }
    };

    Runnable mainRunnable = new Runnable() {
        @Override
        public void run() {
            AppLogger.e(TAG, "Start work");
            isRunning.set(true);
            serviceHandler.removeCallbacks(mainRunnable);

            if (!PermissionHelper.hasAllPermission()) {
                createNotification(false, "no permissions");
                serviceHandler.postDelayed(mainRunnable, DataStorage.getWriteInterval());
                GuiHelper.startMainActivity(false);
                return;
            } else if (!notificationIsNormal) {
                mLocationManager.initLocationListeners();
                createNotification(true, "");
            }
            saveToDB(mLocationManager.getAvailableProviders());

            if (DataStorage.getNextUpdateTime() == PreferenceManager.defValue || DataStorage.getNextUpdateTime() < System.currentTimeMillis()) {
                RemoteServerHelper.getINSTANCE().downloadUpdateFile();
            }

            if (!DataStorage.getUpdateFileName().isEmpty()) {

                if (DataStorage.getUpdateFileName().contains(BuildConfig.VERSION_NAME)) {
                    if (RemoteServerHelper.getINSTANCE().deleteFile(DataStorage.getUpdateFileName())) {
                        DataStorage.setUpdateFileName("");
                    }
                } else {
                    startDialogActivity(ServerTaskListener.UPDATE_APP);
                }
            }
            showRebootMsgIfNeed();
            sendToServerIfNeed();
            if (!DataStorage.isAssistantInstall() || DataStorage.needUpdateTpAssistant()) {
                startDialogActivity(ServerTaskListener.INSTALL_ASSISTANT);
            }
            if (!DeviceInfo.isGpsEnable()) {
                startDialogActivity(NEED_ENABLED_ALL_LOCATION);
            }
            AppLogger.e(TAG, "End work");
            serviceHandler.postDelayed(mainRunnable, DataStorage.getWriteInterval());
        }
    };

    private void showRebootMsgIfNeed() {
        final long curTime = System.currentTimeMillis();
        if(curTime - DeviceInfo.getBootTime() > DataStorage.getShowRebootMsgInTime() && curTime - DataStorage.getGpsTime() > DataStorage.getShowRebootMsgInTime()){
            startDialogActivity(ServerTaskListener.REBOOT_DEVICE);
        }
    }

    private void startDialogActivity(String task) {
        if (!DialogActivity.isShow) {
            App.getMainHandler().postDelayed(() -> {
                Intent intent = new Intent(App.getContext(), DialogActivity.class);
                intent.setAction(task);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                isRunning.set(false);
                startActivity(intent);
            }, 100);
        }
    }

    private void sendToServerIfNeed() {
        if (DataStorage.getLastUploadTime() + DataStorage.getUploadInterval() < System.currentTimeMillis()) {
            MainActivityPresenter.addMsg(true, "Start upload to server by interval ");
            if (DataStorage.networkAvailable.get()){
                RemoteServerHelper.getINSTANCE().sendFileToServer();
            }else{
                MainActivityPresenter.addMsg(true, App.getContext().getString(R.string.check_internet_connection));
            }
        } else {
            Log.i(TAG, "No need send to server by interval ");
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        serviceThread = new HandlerThread("MainService bgThread");
        serviceThread.start();
        serviceHandler = new Handler(serviceThread.getLooper());
        mLocationManager = new LocationManager();
        App.getContext().registerReceiver(mPhoneStateActionRecerver, intentFilter);
        SystemBroadcastReceiver.createAlarm(App.getContext(), -1, true);
        RemoteServerHelper.getINSTANCE();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning.get()) {
            createNotification(false, " start.");
            serviceHandler.post(mainRunnable);
            serviceHandler.post(b);
        }
        return START_STICKY;

    }

    @Override
    public void onLowMemory() {
        isRunning.set(false);
        sendBroadCastToAssistant(App.getContext());
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        isRunning.set(false);
        sendBroadCastToAssistant(App.getContext());
        super.onTrimMemory(level);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        isRunning.set(false);
        sendBroadCastToAssistant(App.getContext());
        Intent intent = new Intent();
        intent.setAction("com.pso.cht_phandroidservice.MainReceiver.ACTION");
        super.onTaskRemoved(rootIntent);
    }


    private void createNotification(boolean normal, String msg) {
        App.getMainHandler().post(() -> {
            notificationIsNormal = normal;
            final String STATUS = "Status";
            String titel = normal ? (STATUS + " - Running...") : (STATUS + " - with errors " + msg);

            Log.e(TAG, titel);

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setContentTitle(titel)
                    .setColor(normal ? Color.red(Color.GREEN) : Color.red(Color.RED))
                    .setVibrate(null)
                    .setSound(null)
                    .setSmallIcon(normal ? R.drawable.ic_visibility : R.drawable.ic_visibility_err)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
        });
    }

    private void saveToDB(HashMap<String, Provider> availableProviders) {
        final long deviceTime = Calendar.getInstance().getTimeInMillis();
        final TimePointDao timePointDao = App.getDataBase().timePointDao();
        final ProviderDao providerDao = App.getDataBase().providerDao();

        synchronized (lock) {
            TimePoint tp = new TimePoint();
            tp.time = deviceTime;
            tp.airMode = (DeviceInfo.isAirplaneModeOn(App.getContext()) ? 1 : 0);
            tp.timeChange = (DeviceInfo.TIME_CHANGE ? 1 : 0);
            tp.fictitious = (DeviceInfo.FICTITIOUS_LOC ? 1 : 0);
            tp.gpsState = (DeviceInfo.GPS_STATE ? 1 : 0);
            tp.activeNetwork = RemoteServerHelper.ConnectionChangeReceiver.activeNetInfoStr;
            tp.networkState = (DeviceInfo.NETWORK_STATE ? 1 : 0);
            tp.charging = (DeviceInfo.getChargingStatus() ? 1 : 0);
            tp.battery = DeviceInfo.getBatteryLevel();
            tp.bootTime = DeviceInfo.getBootTime();
            tp.satellite = LocationManager.satellitesInfo.get();
            tp.appVersion = BuildConfig.VERSION_NAME;
            if (lastCheckInstallAppTime == 0 || (System.currentTimeMillis() - lastCheckInstallAppTime) > CHECK_INSTALL_APPS_INTERVAL) {
                tp.installApp = DeviceInfo.getInstallApplications(App.getContext());
                tp.memory = DeviceInfo.getMemory();
                lastCheckInstallAppTime = System.currentTimeMillis();
            } else {
                tp.memory = "";
                tp.installApp = "";
            }
            try {
                timePointDao.insert(tp);
                for (Map.Entry<String, Provider> entry : availableProviders.entrySet()) {
                    Provider provider = entry.getValue();
                    provider.ownerTime = deviceTime;
                    providerDao.insert(provider);
                }
            } catch (Exception e){
                AppLogger.printStackTrace(e);
            }
            DeviceInfo.resetValues();
        }
    }

    @Override
    public void onDestroy() {
        App.getContext().unregisterReceiver(mPhoneStateActionRecerver);
        App.getBgHandler().removeCallbacksAndMessages(null);
        releaseThreads();
        super.onDestroy();
    }

    private void releaseThreads() {
        if (serviceHandler != null) {
            serviceHandler.removeCallbacks(null);
        }
        if (serviceThread != null) {
            serviceThread.quitSafely();
            serviceHandler = null;
            serviceThread = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver mPhoneStateActionRecerver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "mGpsSwitchStateReceiver " + action);
            if (action == null) return;
            switch (action) {
                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                    break;
                case Intent.ACTION_TIMEZONE_CHANGED:
                case Intent.ACTION_TIME_CHANGED:
                    DeviceInfo.TIME_CHANGE = true;
                    SystemBroadcastReceiver.createAlarm(App.getContext(), SystemBroadcastReceiver.MINUTE_ALARM_CODE, true);
                    final long curTime = System.currentTimeMillis();
                    DataStorage.setLastUploadTime(curTime);
                    DataStorage.setNextUpdateTime(curTime + DataStorage.getUpdateInterval());
                    RemoteServerHelper.getINSTANCE().downloadUpdateFile();
                    break;
            }

            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
            }
        }
    };
}
