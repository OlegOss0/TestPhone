package com.pso.testphone.data;

import android.content.SharedPreferences;

import com.pso.testphone.App;
import com.pso.testphone.BuildConfig;

import static android.content.Context.MODE_PRIVATE;

public class PreferenceManager {
    private static final String DEFAULT_ADMIN_PASSWORD = "1234";
    private static PreferenceManager INSTANCE;
    private static SharedPreferences sPref;
    private static SharedPreferences.Editor editor;
    private static final String PREF_NAME = "main pref";
    private static final long defChUpdateInt = 1000 * 60;
    private static final long defUploadInterval = 1000 * 60 * 15;
    private static final long defWriteInterval = 1000 * 15;
    public static final long defValue = -1;

    //preferences name
    private static final String LAST_UPLOAD_TIME_PREF = "last_upload_time";
    private static final String UPDATE_INTERVAL = "update_interval";
    private static final String NEXT_UPDATE_TIME = "next_update_time";
    private static final String UPLOAD_INTERVAL = "upload_interval";
    private static final String WRITE_INTERVAL = "write_interval";
    private static final String UPDATE_FILE_NAME = "update_file_name";
    private static final String FIRST_TIME_PREF = "first_time";
    private static final String SHOW_REBOOT_MSG_PREF = "show_reboot_msg";
    private static final String GPS_TIME = "gps_time";
    private static final String ADDITIONAL_LOGGING_PREF = "additional_logging_pref";
    private static final String UPLOAD_SERVER_ADDRESS_PREF = "upload_server_address_pref";
    private static final String ADMIN_PASSWORD_PREF = "password_for_main_menu_pref";
    private static final String NEED_UPDATE_ASSISTANT_PREF = "need_update_assisatnt_pref";

    protected static PreferenceManager getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new PreferenceManager();
            sPref = App.getContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            editor = sPref.edit();
        }
        return INSTANCE;
    }

    protected long getLastUploadTime() {
        return sPref.getLong(LAST_UPLOAD_TIME_PREF, -1);
    }

    protected void setLastUploadTimePref(long time) {
        editor.putLong(LAST_UPLOAD_TIME_PREF, time).apply();
    }

    protected long getUpdateInterval() {
        return sPref.getLong(UPDATE_INTERVAL, defChUpdateInt);
    }

    protected long getNextUpdateTime(){
        return sPref.getLong(NEXT_UPDATE_TIME, defValue);
    }

    protected void setNextUpdateTime(long time){
        editor.putLong(NEXT_UPDATE_TIME, time).apply();
    }

    protected void setUpdateInterval(long interval) {
        editor.putLong(UPDATE_INTERVAL, interval).apply();
    }

    protected long getUploadInterval() {
        return sPref.getLong(UPLOAD_INTERVAL, defUploadInterval);
    }

    protected void setUpLoadInterval(long interval) {
        editor.putLong(UPLOAD_INTERVAL, interval).apply();
    }

    protected long getWriteInterval(){
        return sPref.getLong(WRITE_INTERVAL, defWriteInterval);
    }

    protected void setWriteInterval(long interval){
        editor.putLong(WRITE_INTERVAL, interval).apply();
    }

    protected String getUpdateFileName() {
        return sPref.getString(UPDATE_FILE_NAME, "");
    }

    protected void setUpdateFileName(String fileName){
        editor.putString(UPDATE_FILE_NAME, fileName).apply();
    }

    protected boolean isFirstTime() {
        if(sPref.getBoolean(FIRST_TIME_PREF, true)){
            editor.putBoolean(FIRST_TIME_PREF, false);
            return true;
        }
        return sPref.getBoolean(FIRST_TIME_PREF, false);
    }

    protected long getGpsTimePref() {
        return sPref.getLong(GPS_TIME, -1);
    }

    protected void setGpsTimePref(long time){
        editor.putLong(GPS_TIME, time).apply();
    }

    protected long showRebootMsgInTimePref() {
        return sPref.getLong(SHOW_REBOOT_MSG_PREF, 720000); //millisec in 2 hour
    }

    protected void setShowRebootMsgInTimePref(long time) {
        editor.putLong(SHOW_REBOOT_MSG_PREF, time).apply();
    }

    protected boolean getAdditionalLoggingPref() {
        return sPref.getBoolean(ADDITIONAL_LOGGING_PREF, false);
    }

    protected void setAdditionalLoggingPref(boolean enabled){
        editor.putBoolean(ADDITIONAL_LOGGING_PREF, enabled).apply();
    }

    public String getUploadServerAdressPref() {
        return sPref.getString(UPLOAD_SERVER_ADDRESS_PREF, BuildConfig.DEFAULT_UPLOAD_SERVER_ADDRESS);
}

    public void setUploadServerAdressPref(String uploadServerAddress) {
        editor.putString(UPLOAD_SERVER_ADDRESS_PREF, uploadServerAddress).apply();
    }

    public String getAdminPasswordPref() {
        return sPref.getString(ADMIN_PASSWORD_PREF, DEFAULT_ADMIN_PASSWORD);
    }

    public void setAdminPasswordPref(String pass) {
        editor.putString(ADMIN_PASSWORD_PREF, pass).apply();
    }

    public boolean needUpdateTpAssistant() {
        return sPref.getBoolean(NEED_UPDATE_ASSISTANT_PREF, false);
    }

    public void setNoNeedUpdateTpAssistant() {
        editor.putBoolean(NEED_UPDATE_ASSISTANT_PREF, false).apply();
    }
}
