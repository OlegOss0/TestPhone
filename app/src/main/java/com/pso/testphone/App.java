package com.pso.testphone;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.room.Room;

import com.pso.testphone.data.Codes;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;
import com.pso.testphone.db.AppDataBase;
import com.pso.testphone.gui.MainActivityPresenter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class App extends Application {
    private static final String TAG = App.class.getSimpleName();
    public static final String CHANNEL_ID = "MainServiceChannel";

    private static Context mContext;
    private HandlerThread bgThread;
    private static Handler bgHandler;
    private static Handler mainHandler;
    private static AppDataBase appDataBase;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        DeviceInfo.Init(mContext);
        bgThread = new HandlerThread("App backGroundThread");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
        mainHandler = new Handler();
        appDataBase = Room.databaseBuilder(getApplicationContext(), AppDataBase.class, "database")
                .addMigrations(AppDataBase.MIGRATION_1_2, AppDataBase.MIGRATION_2_3, AppDataBase.MIGRATION_3_4, AppDataBase.MIGRATION_4_5)
                .allowMainThreadQueries()
                .build();
        createNotificationChannel();
    }

    public static boolean unpackAssistant() {
        AssetManager assetManager = getContext().getAssets();
        File unpackFile = new File(getContext().getFilesDir() + "/" + DataStorage.APP_ASSISTANT_FILE_NAME);
        if (!unpackFile.exists()) {
            InputStream in;
            OutputStream out;
            byte[] buffer = new byte[1024];
            try {
                in = new BufferedInputStream(assetManager.open(DataStorage.APP_ASSISTANT_FILE_NAME));
                out = new BufferedOutputStream(new FileOutputStream(unpackFile));
                int len = 0;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.close();
                unpackFile.setReadable(true, false);
                MainActivityPresenter.addMsg(true, "Done.");
                return true;
            } catch (IOException e) {
                AppLogger.writeLog(Codes.IOEXCEPTION_CODE, "UnpackAssistant exception");
                AppLogger.writeLogEx(e);
            }
        }
        return true;
    }
    public static void deleteAssistantIntallFile(){
        File unpackFile = new File(getContext().getFilesDir() + "/" + DataStorage.APP_ASSISTANT_FILE_NAME);
        unpackFile.delete();
    }

    public static Context getContext(){
        return mContext;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mainNotification = new NotificationChannel(CHANNEL_ID,
                    "TestPhone notification", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mainNotification);
            }
        }
    }

    public static AppDataBase getDataBase(){
        return appDataBase;
    }

    public static Handler getBgHandler(){
        return bgHandler;
    }

    public static Handler getMainHandler(){
        return mainHandler;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }




}
