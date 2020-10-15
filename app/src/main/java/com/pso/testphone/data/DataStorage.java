package com.pso.testphone.data;

import android.os.Build;

import com.pso.testphone.BuildConfig;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DataStorage {
    public static final String APP_NAME = "test_phone";
    public static final String APP_ASSISTANT_FILE_NAME = "tp_assistant.apk";
    public static final String APP_ASSISTANT_NAME = "TPAssistant";
    public static final String TP_ASSISTANT_PACKAGE = "com.pso.tpassistant";
    public static final String TP_ASSISTANT_PACKAGE_SERVICE_CLASS = "com.pso.tpassistant.MainReceiver";

    public static final int APP_UPDATE_REQUEST = 0x0000112;
    public static final int APP_INSTALL_ASSISTANT_REQUEST = 0x0000113;
    public static final int REGUEST_CODE_GPS_ENABLE = 0x0000114;
    private static AtomicLong updateInterval = new AtomicLong(PreferenceManager.getINSTANCE().getUpdateInterval());
    private static AtomicLong nextUpdateTime = new AtomicLong(PreferenceManager.getINSTANCE().getNextUpdateTime());
    private static AtomicLong uploadInterval = new AtomicLong(PreferenceManager.getINSTANCE().getUploadInterval());
    private static AtomicLong writeInterval = new AtomicLong(PreferenceManager.getINSTANCE().getWriteInterval());
    private static AtomicLong lastUploadTime = new AtomicLong(PreferenceManager.getINSTANCE().getLastUploadTime());
    private static AtomicReference<String> updateFileName = new AtomicReference<String>(PreferenceManager.getINSTANCE().getUpdateFileName());
    private static AtomicBoolean tpAssistantInstall = new AtomicBoolean(true);
    private static AtomicLong gpsTime = new AtomicLong(PreferenceManager.getINSTANCE().getGpsTimePref());
    private static AtomicLong showRebootMsgInTime = new AtomicLong(PreferenceManager.getINSTANCE().showRebootMsgInTimePref());
    public static AtomicLong lastTimeShowRebootMsg = new AtomicLong(-1);
    public static AtomicBoolean additionalLogginEnabled = new AtomicBoolean(PreferenceManager.getINSTANCE().getAdditionalLoggingPref());
    public static AtomicBoolean networkAvailable = new AtomicBoolean(true);

    public static boolean isAdditionalLoggingEnabled(){
        return additionalLogginEnabled.get();
    }

    public static void switchAdditionalLogging(boolean enable){
        additionalLogginEnabled.set(enable);
        PreferenceManager.getINSTANCE().setAdditionalLoggingPref(enable);
    }

    public static boolean needUpdateTpAssistant(){
        return PreferenceManager.getINSTANCE().needUpdateTpAssistant();
    }

    public static void noNeedUpdateTpAssistant(){
        PreferenceManager.getINSTANCE().setNoNeedUpdateTpAssistant();
    }

    public static void setGpsTime(long time){
        gpsTime.set(time);
        PreferenceManager.getINSTANCE().setGpsTimePref(time);
    }

    public static long getGpsTime(){
        return gpsTime.get();
    }

    public static void setShowRebootMsgInTime(long time){
        showRebootMsgInTime.set(time);
        PreferenceManager.getINSTANCE().setShowRebootMsgInTimePref(time);
    }

    public static long getShowRebootMsgInTime(){
        return showRebootMsgInTime.get();
    }

    public static String getLogin(){
        return BuildConfig.SERVER_LOGIN;
    }

    public static String getPass(){
        return BuildConfig.SERVER_PASS;
    }

    public static void setLastUploadTime(long time){
        lastUploadTime.set(time);
        PreferenceManager.getINSTANCE().setLastUploadTimePref(time);
    }

    public static long getLastUploadTime(){
        return lastUploadTime.get();
    }

    public static void setUpdateInterval(long interval){
        updateInterval.set(interval);
        PreferenceManager.getINSTANCE().setUpdateInterval(interval);
    }

    public static long getUpdateInterval(){
        return updateInterval.get();
    }

    public static void setUpdateFileName(String s) {
        updateFileName.set(s);
        PreferenceManager.getINSTANCE().setUpdateFileName(s);
    }

    public static String getUpdateFileName(){
        return updateFileName.get();
    }

    public static void setNextUpdateTime(long time){
        nextUpdateTime.set(time);
        PreferenceManager.getINSTANCE().setNextUpdateTime(time);
    }

    public static long getNextUpdateTime(){
        return nextUpdateTime.get();
    }

    public static void setUploadInterval(long interval){
        uploadInterval.set(interval);
        PreferenceManager.getINSTANCE().setUpLoadInterval(interval);
    }

    public static long getUploadInterval(){
        return uploadInterval.get();
    }

    public static void setWriteInterval(long interval){
        writeInterval.set(interval);
        PreferenceManager.getINSTANCE().setWriteInterval(interval);
    }

    public static long getWriteInterval(){
        return writeInterval.get();
    }

    public static boolean isFirstTime() {
        return PreferenceManager.getINSTANCE().isFirstTime();
    }

    public static void setAssistantInstall(boolean isInst) {
        tpAssistantInstall.set(isInst);
    }

    public static boolean isAssistantInstall(){
        return tpAssistantInstall.get();
    }

    public static String getUploadServerAddress() {
        return PreferenceManager.getINSTANCE().getUploadServerAdressPref();
    }

    public static void setUploadServerAddress(String uploadServerAddress) {
        PreferenceManager.getINSTANCE().setUploadServerAdressPref(uploadServerAddress);
    }

    public static String getAdminPassword() {
        return PreferenceManager.getINSTANCE().getAdminPasswordPref();
    }

    public static void setAdminPassword(String pass) {
        PreferenceManager.getINSTANCE().setAdminPasswordPref(pass);
    }
}
