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
    private static final long DEF_UPDATE_INTERVAL = 1000 * 60;
    private static final long DEF_UNLOAD_DATA_FILE_INTERVAL = 1000 * 60 * 15;
    private static final long DEF_WRITE_INTERVAL = 1000 * 15;
    private static final long DEF_UNLOAD_LOG_INTERVAL = 1000 * 60 * 15;
    private static final long DEF_SHOW_REBOOT_MSG = (1000 * 60 * 60) * 2;
    public static final long DEF_VALUE = -1;


    //preferences name
    private static final String UNLOAD_DATA_FILE_INT_PREF = "upload_interval";
    private static final String LAST_UNLOAD_DATA_FILE_TIME_PREF = "last_upload_time";

    private static final String UPDATE_INTERVAL_PREF = "update_interval";
    private static final String LAST_UPDATE_TIME_PREF = "next_update_time";

    private static final String WRITE_INTERVAL = "write_interval";

    private static final String LAST_UNLOAD_LOG_TIME = "last_upload_log_interval";
    private static final String UNLOAD_LOG_INTERVAL = "upload_log_interval";

    private static final String SHOW_REBOOT_MSG_PREF = "show_reboot_msg";
    private static final String LAST_SHOW_REBOOT_MSG_TIME_PREF = "last_show_reboot_msg_time";

    private static final String UPDATE_FILE_NAME = "update_file_name";
    private static final String FIRST_TIME_PREF = "first_time";
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

    //to unload logs file to server
    protected long getLastUnloadLogsTimePref(){
        return sPref.getLong(LAST_UNLOAD_LOG_TIME, DEF_VALUE);
    }
    protected void setLastUnloadLogsTimePref(long time){
        editor.putLong(LAST_UNLOAD_LOG_TIME, time).apply();
    }
    protected long getUnloadLogIntervalPref(){
        return sPref.getLong(UNLOAD_LOG_INTERVAL, DEF_UNLOAD_LOG_INTERVAL);
    }
    protected void setUnloadLogIntervalPref(long interval) {
        editor.putLong(UNLOAD_LOG_INTERVAL, interval).apply();
    }
    //

    //Update interval
    protected long getUpdateIntervalPref() {
        return sPref.getLong(UPDATE_INTERVAL_PREF, DEF_UPDATE_INTERVAL);
    }
    protected void setUpdateIntervalPref(long interval) {
        editor.putLong(UPDATE_INTERVAL_PREF, interval).apply();
    }
    protected long getLastUpdateTimePref(){
        return sPref.getLong(LAST_UPDATE_TIME_PREF, DEF_VALUE);
    }
    protected void setLastUpdateTimePref(long time){
        editor.putLong(LAST_UPDATE_TIME_PREF, time).apply();
    }
    //

    //to unload main data file to server
    protected long getUnloadDataFileIntPref() {
        return sPref.getLong(UNLOAD_DATA_FILE_INT_PREF, DEF_UNLOAD_DATA_FILE_INTERVAL);
    }
    protected void setUnloadDataFileIntPref(long interval) {
        editor.putLong(UNLOAD_DATA_FILE_INT_PREF, interval).apply();
    }
    protected long getLastUnloadDataFileTimePref() {
        return sPref.getLong(LAST_UNLOAD_DATA_FILE_TIME_PREF, DEF_VALUE);
    }
    protected void setLastUnloadDataFileTimePref(long time) {
        editor.putLong(LAST_UNLOAD_DATA_FILE_TIME_PREF, time).apply();
    }
    //

    //Main interval write all params in db
    protected long getWriteIntervalPref(){
        return sPref.getLong(WRITE_INTERVAL, DEF_WRITE_INTERVAL);
    }
    protected void setWriteIntervalPref(long interval){
        editor.putLong(WRITE_INTERVAL, interval).apply();
    }
    //

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
        return sPref.getLong(SHOW_REBOOT_MSG_PREF, DEF_SHOW_REBOOT_MSG); //millisec in 2 hour
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

    //reboot msg
    public long getLastTimeShowRebootMsgPref() {
        return sPref.getLong(LAST_SHOW_REBOOT_MSG_TIME_PREF, DEF_VALUE);
    }
    public void setLastTimeShowRebootMsgPref(long time) {
        editor.putLong(LAST_SHOW_REBOOT_MSG_TIME_PREF, time).apply();
    }
}
