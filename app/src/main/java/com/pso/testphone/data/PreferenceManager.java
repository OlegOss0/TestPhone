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
    private static final long DEF_EXCHANGE_TIME = 1000 * 3;
    public static final long DEF_VALUE = -1;
    public static final boolean DEF_BOOL_VALUE_FALSE = false;
    public static final boolean DEF_BOOL_VALUE_TRUE = false;
    public static final boolean PROPERTIES_DEF_VALUE_=  DEF_BOOL_VALUE_TRUE;


    //preferences name
    private static final String UNLOAD_DATA_FILE_INT_PREF = "upload_interval";
    private static final String LAST_UNLOAD_DATA_FILE_TIME_PREF = "last_upload_time";

    private static final String EXCHANGE_TIME_PREF = "exchange_time_pref";

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
        if(!DataStorage.TP_ASSISTANT_VER.isEmpty()){
            if(Double.parseDouble(DataStorage.TP_ASSISTANT_VER) < Double.parseDouble("1.2")){
                return true;
            }
        }
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

    protected long getExchangeTimePref() {
        return sPref.getLong(ADDITIONAL_LOGGING_PREF, DEF_EXCHANGE_TIME);
    }
    protected void setExchangeTimePref(long time) {
        editor.putLong(LAST_SHOW_REBOOT_MSG_TIME_PREF, time).apply();
    }


    //Properties values
    private static final String AIR_MODE_CHECKING_PREF = "air_mode_pref";
    public boolean getAirModeCheckingPref() {
        return sPref.getBoolean(AIR_MODE_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setdAirModeCheckingPref(boolean enable) {
        editor.putBoolean(AIR_MODE_CHECKING_PREF, enable).apply();
    }

    private static final String TIME_CHANGE_CHECKING_PREF = "time_change_pref";
    public boolean getTimeChangeCheckingPref() {
        return sPref.getBoolean(TIME_CHANGE_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setTimeChangeCheckingPref(boolean enable) {
        editor.putBoolean(TIME_CHANGE_CHECKING_PREF, enable).apply();
    }

    private static final String FAKE_COORD_CHECKING_PREF = "used_fake_coordinates_pref";
    public boolean getFakeCoordCheckingPref() {
        return sPref.getBoolean(FAKE_COORD_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setFakeCoordCheckingPref(boolean enable) {
        editor.putBoolean(FAKE_COORD_CHECKING_PREF, enable).apply();
    }

    private static final String COORD_CHECKING_PREF = "coordinates_pref";
    public boolean getCoordCheckingPref() {
        return sPref.getBoolean(COORD_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setCoordCheckingPref(boolean enable) {
        editor.putBoolean(FAKE_COORD_CHECKING_PREF, enable).apply();
    }

    private static final String SATELLITES_CHECKING_PREF = "satellites_pref";
    public boolean getSatellitesCheckingPref() {
        return sPref.getBoolean(SATELLITES_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setSatellitesCheckingPref(boolean enable) {
        editor.putBoolean(SATELLITES_CHECKING_PREF, enable).apply();

    }

    private static final String GPS_ERR_CHECKING_PREF = "gps_error_pref";
    public boolean getGpsErrorCheckingPref() {
        return sPref.getBoolean(GPS_ERR_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setGpsErrorCheckingPref(boolean enable) {
        editor.putBoolean(GPS_ERR_CHECKING_PREF, enable).apply();
    }

    private static final String CHARGE_CHECKING_PREF = "charging_pref";
    public boolean getChargeCheckingPref() {
        return sPref.getBoolean(CHARGE_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setChargeCheckingPref(boolean enable) {
        editor.putBoolean(CHARGE_CHECKING_PREF, enable).apply();
    }

    private static final String BATTERY_CHECKING_PREF = "battery_pref";
    public boolean getBatteryCheckingPref() {
        return sPref.getBoolean(BATTERY_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setBatteryCheckingPref(boolean enable) {
        editor.putBoolean(BATTERY_CHECKING_PREF, enable).apply();
    }

    private static final String MEMORY_CHECKING_PREF = "memory_cpace_pref";
    public boolean getMemoryCheckingPref() {
        return sPref.getBoolean(MEMORY_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setMemoryCheckingPref(boolean enable) {
        editor.putBoolean(MEMORY_CHECKING_PREF, enable).apply();
    }

    private static final String APPS_CHECKING_PREF = "installed_apps_pref";
    public boolean getAppsCheckingPref() {
        return sPref.getBoolean(APPS_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setAppsCheckingPref(boolean enable) {
        editor.putBoolean(APPS_CHECKING_PREF, enable).apply();
    }

    private static final String PERMS_CHECKING_PREF = "denied_perms_pref";
    public boolean getPermsCheckingPref() {
        return sPref.getBoolean(PERMS_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setPermsCheckingPref(boolean enable) {
        editor.putBoolean(PERMS_CHECKING_PREF, enable).apply();
    }

    private static final String NETWORK_STATE_CHECKING_PREF = "network_states_pref";
    public boolean getNetworkCheckingPref() {
        return sPref.getBoolean(NETWORK_STATE_CHECKING_PREF, PROPERTIES_DEF_VALUE_);
    }
    public void setNetworkCheckingPref(boolean enable) {
        editor.putBoolean(NETWORK_STATE_CHECKING_PREF, enable).apply();
    }
}
