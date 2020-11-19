package com.pso.testphone.data;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.pso.testphone.App;
import com.pso.testphone.BuildConfig;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DeviceInfo {
    public static boolean TIME_CHANGE = false;
    public static boolean FICTITIOUS_LOC = false;
    public static boolean GPS_STATE = false;
    public static boolean NETWORK_STATE = false;
    private static long bootTime = -1;
    public static final String GPS_PROVIDER = "gps";
    public static final String NETWORK_PROVIDER = "network";
    private static String mIMEI;
    private static String deniedPermissionList = "";


    public static void Init(Context context) {
        isAirplaneModeOn(context);
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }


    public static boolean isMockLocationOn(Location location, Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return location.isFromMockProvider();
        } else {
            String mockLocation = "0";
            try {
                mockLocation = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return !mockLocation.equals("0");
        }
    }

    public static void resetValues() {
        TIME_CHANGE = false;
        FICTITIOUS_LOC = false;
        GPS_STATE = false;
        NETWORK_STATE = false;
    }

    public static void setProviderState(String providerName, boolean providerEnabled) {
        switch (providerName) {
            case GPS_PROVIDER:
                GPS_STATE = providerEnabled;
                break;
            case NETWORK_PROVIDER:
                NETWORK_STATE = providerEnabled;
                break;
        }
    }

    public static String getInstallApplications(Context context) {
        boolean tpAssistantInstall = false;
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        ArrayList<String> apps = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (ApplicationInfo applicationInfo : allApps) {
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = applicationInfo.loadLabel(pm).toString();
                if (appName.contains(DataStorage.APP_ASSISTANT_NAME)) {
                    tpAssistantInstall = true;
                }
                apps.add(applicationInfo.loadLabel(pm).toString());
            }
        }
        DataStorage.setAssistantInstall(tpAssistantInstall);
        sb.append("Total " + apps.size() + ".");
        for (String app : apps) {
            sb.append(app);
            sb.append(".");
        }
        return sb.toString();
    }


    public static String getFull() {
        StringBuilder sb = new StringBuilder();
        sb.append("Manufacturer - " + (Build.MANUFACTURER).toUpperCase() + " . ")
                .append("Model - " + (Build.MODEL).toUpperCase() + " . ")
                .append("OS Android - " + Build.VERSION.SDK_INT + " . ")
                .append("S/N - " + (Build.VERSION.SDK_INT >= 29 ? "not support" : Build.SERIAL) + " . ")
                .append("IMEI - " + getIMEI() + " . ")
                .append("Ap. version - " + BuildConfig.VERSION_NAME);
        return sb.toString();
    }

    public static String getIMEI() {
        if (mIMEI == null) {
            TelephonyManager tMgr = (TelephonyManager) App.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try{
                    mIMEI = tMgr.getImei();
                }catch (Exception e){

                }
                if(mIMEI == null || mIMEI.isEmpty()){
                    mIMEI = Settings.Secure.getString(
                            App.getContext().getContentResolver(),
                            Settings.Secure.ANDROID_ID);
                }
            }else{
                mIMEI = tMgr.getDeviceId();
            }
        }
        return mIMEI;
    }

    public static String getMemory() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(bytesToHuman(TotalMemory()).replace(",", ":"))
                .append("|");
        stringBuilder.append(bytesToHuman(BusyMemory()).replace(",", ":"))
                .append("|");
        stringBuilder.append(bytesToHuman(FreeMemory()).replace(",", ":"))
                .append("|");
        return stringBuilder.toString();

    }

    private static long TotalMemory() {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long Total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        return Total;
    }

    private static long FreeMemory() {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long Free = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        return Free;
    }

    private static long BusyMemory() {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long Total = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
        long Free = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
        long Busy = Total - Free;
        return Busy;
    }

    public static String floatForm(double d) {
        return new DecimalFormat("#,##").format(d);
    }


    public static String bytesToHuman(long size) {
        long Kb = 1 * 1024;
        long Mb = Kb * 1024;
        long Gb = Mb * 1024;
        long Tb = Gb * 1024;
        long Pb = Tb * 1024;
        long Eb = Pb * 1024;

        if (size < Kb) return floatForm(size) + " byte";
        if (size >= Kb && size < Mb) return floatForm((double) size / Kb) + " Kb";
        if (size >= Mb && size < Gb) return floatForm((double) size / Mb) + " Mb";
        if (size >= Gb && size < Tb) return floatForm((double) size / Gb) + " Gb";
        if (size >= Tb && size < Pb) return floatForm((double) size / Tb) + " Tb";
        if (size >= Pb && size < Eb) return floatForm((double) size / Pb) + " Pb";
        if (size >= Eb) return floatForm((double) size / Eb) + " Eb";

        return "???";
    }

    public static int getBatteryLevel() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = App.getContext().registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100.0f);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean getChargingStatus() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = App.getContext().registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (status == 2) {
            return true;
        }
        return false;
    }

    public static long getBootTime() {
        if (bootTime == -1) {
            bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        }
        return bootTime;
    }

    public static boolean isGpsEnable() {
        String gpsONstr = Settings.Secure.getString(App.getContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        return (gpsONstr.contains("network,gps")) | (gpsONstr.contains("gps,network"));
    }

    public static String getDeniedPermissions() {
        return deniedPermissionList;
    }


}
