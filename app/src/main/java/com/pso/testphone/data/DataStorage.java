package com.pso.testphone.data;

import com.pso.testphone.BuildConfig;

import java.util.ArrayList;
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
    private static AtomicLong writeInterval = new AtomicLong(PreferenceManager.getINSTANCE().getWriteIntervalPref());

    private static AtomicLong unloadDataFileInt = new AtomicLong(PreferenceManager.getINSTANCE().getUnloadDataFileIntPref());
    private static AtomicLong lastUnloadDataFileTime = new AtomicLong(PreferenceManager.getINSTANCE().getLastUnloadDataFileTimePref());

    private static AtomicLong updateInterval = new AtomicLong(PreferenceManager.getINSTANCE().getUpdateIntervalPref());
    private static AtomicLong lastUpdateTime = new AtomicLong(PreferenceManager.getINSTANCE().getLastUpdateTimePref());

    private static AtomicLong unloadLogInterval = new AtomicLong(PreferenceManager.getINSTANCE().getUnloadLogIntervalPref());
    private static AtomicLong lastUnloadLogTime = new AtomicLong(PreferenceManager.getINSTANCE().getLastUnloadLogsTimePref());

    private static AtomicReference<String> updateFileName = new AtomicReference<String>(PreferenceManager.getINSTANCE().getUpdateFileName());
    private static AtomicBoolean tpAssistantInstall = new AtomicBoolean(true);
    private static AtomicLong gpsTime = new AtomicLong(PreferenceManager.getINSTANCE().getGpsTimePref());
    private static AtomicLong showRebootMsgInTime = new AtomicLong(PreferenceManager.getINSTANCE().showRebootMsgInTimePref());
    private static AtomicLong lastTimeShowRebootMsg = new AtomicLong(PreferenceManager.getINSTANCE().getLastTimeShowRebootMsgPref());
    public static AtomicBoolean additionalLogginEnabled = new AtomicBoolean(PreferenceManager.getINSTANCE().getAdditionalLoggingPref());
    public static AtomicBoolean networkAvailable = new AtomicBoolean(true);
    public static AtomicReference<String> ip = new AtomicReference<>("");
    public static AtomicReference<String> activeNetInfoStr = new AtomicReference<>("");
    private static ArrayList<String> deniedPermsList = new ArrayList<>();
    private static AtomicLong exchangeAssistantTime = new AtomicLong(1000 * 3);

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

    public static long getLastTimeShowRebootMsg(){
        return lastTimeShowRebootMsg.get();
    }

    public static void setLastTimeShowRebootMsg(long time){
        lastTimeShowRebootMsg.set(time);
        PreferenceManager.getINSTANCE().setLastTimeShowRebootMsgPref(time);
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

    public static String getIpInfoToken(){
        return BuildConfig.IP_INFO_TOKEN;
    }

    //Update interval
    public static void setUpdateInterval(long interval){
        updateInterval.set(interval);
        PreferenceManager.getINSTANCE().setUpdateIntervalPref(interval);
    }
    public static long getUpdateInterval(){
        return updateInterval.get();
    }
    public static void setLastUpdateTime(long time){
        lastUpdateTime.set(time);
        PreferenceManager.getINSTANCE().setLastUpdateTimePref(time);
    }
    public static long getLastUpdateTime(){
        return lastUpdateTime.get();
    }
    //

    public static void setUpdateFileName(String s) {
        updateFileName.set(s);
        PreferenceManager.getINSTANCE().setUpdateFileName(s);
    }

    public static String getUpdateFileName(){
        return updateFileName.get();
    }


    //to unload main data file to server
    public static void setUnloadDataFileInt(long interval){
        unloadDataFileInt.set(interval);
        PreferenceManager.getINSTANCE().setUnloadDataFileIntPref(interval);
    }
    public static long getUnloadDataFileInt(){
        return unloadDataFileInt.get();
    }
    public static void setLastUnloadDataFileTime(long time){
        lastUnloadDataFileTime.set(time);
        PreferenceManager.getINSTANCE().setLastUnloadDataFileTimePref(time);
    }
    public static long getLastUnloadDataFileTime(){
        return lastUnloadDataFileTime.get();
    }
    //

    //Main interval write all params in db
    public static void setWriteInterval(long interval){
        writeInterval.set(interval);
        PreferenceManager.getINSTANCE().setWriteIntervalPref(interval);
    }
    public static long getWriteInterval(){
        return writeInterval.get();
    }
    //

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

    public static boolean setUploadServerAddress(String uploadServerAddress) {
        if (uploadServerAddress.isEmpty()) return false;
        if (uploadServerAddress.equals(DataStorage.getUploadServerAddress())) return true;
        PreferenceManager.getINSTANCE().setUploadServerAdressPref(uploadServerAddress);
        return true;
    }

    public static String getAdminPassword() {
        return PreferenceManager.getINSTANCE().getAdminPasswordPref();
    }

    public static boolean setAdminPassword(String pass) {
        if (pass.isEmpty()) return false;
        PreferenceManager.getINSTANCE().setAdminPasswordPref(pass);
        return true;
    }

    ////to unload logs  file to server
    public static void setUnloadLogsInterval(long interval) {
        unloadLogInterval.set(interval);
        PreferenceManager.getINSTANCE().setUnloadLogIntervalPref(interval);
    }
    public static long getUnloadLogsInterval() {
        return unloadLogInterval.get();
    }
    public static void setLastUnloadLogsTime(long time){
        lastUnloadLogTime.set(time);
        PreferenceManager.getINSTANCE().setLastUnloadLogsTimePref(time);
    }
    public static long getLastUnloadLogFileTime(){
        return lastUnloadLogTime.get();
    }
    //

    public static void addDeniedPerm(String pStr) {
        deniedPermsList.add(pStr);
    }

    public static String getDeniedPermsString(){
        StringBuilder sb = new StringBuilder();
        for(String p : deniedPermsList){
            sb.append(p);
            sb.append(" ");
        }
        return sb.toString();
    }

    public static void clearDeniedPerm(){
        deniedPermsList.clear();
    }

    public static long getAssExchangeTime() {
        return exchangeAssistantTime.get();
    }
}
