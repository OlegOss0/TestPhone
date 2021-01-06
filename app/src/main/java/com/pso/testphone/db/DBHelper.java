package com.pso.testphone.db;

import com.pso.testphone.App;
import com.pso.testphone.AppLogger;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;
import com.pso.testphone.gui.MainActivityPresenter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class DBHelper {

    private static DBHelper Instance;
    public static String TelemetryFileHeader;
    public static final String[] PROVIDER_NAME = {"network", "gps"};
    private static final Object lock = new Object();
    private final String ON = "on";
    private final String OFF = "off";
    private final String NULL = "null";
    private final String EMPTY = "";
    private final String CHANGE = "change";
    private final String TRUE = "true";
    private final String FALSE = "false";
    private final String FAKE = "fake";
    private final String ZERO = "0";
    private final String ONE = "1";
    private final String DISABLED = "-";

    public static DBHelper getInstance() {
        if (Instance == null) {
            synchronized (lock) {
                Instance = new DBHelper();
            }
        }
        return Instance;
    }

    private DBHelper() {
        TelemetryFileHeader = initTelemetryFileHeader();
    }

    public class Data {
        public final String dataStr;
        public ArrayList<Object> objs = new ArrayList<>();

        Data(String dataStr, Object obj) {
            this.dataStr = dataStr;
            this.objs.add(obj);
        }

        Data(String dataStr) {
            this.dataStr = dataStr;
        }
    }

    /*public class GeneretedData {
        public LinkedList<Data> data = new LinkedList<>();
        public String fileName;
        public boolean hasOtherDayData;
        public ArrayList<Object> objDeleteFromDb = new ArrayList<>();
    }*/

    public class GeneretedData {
        public String dataStr = "";
        public String fileName;
        public boolean hasOtherData;
        public ArrayList<Object> objDeleteFromDb = new ArrayList<>();
        public boolean dbHasNextDay;
    }


    public static void deleteSendingDataFromDb(ArrayList<Object> objDeleteFromDb) {
        if (objDeleteFromDb != null && !objDeleteFromDb.isEmpty()) {
            for (Object o : objDeleteFromDb) {
                if (o instanceof MyLog) {
                    App.getDataBase().myLogDao().delete((MyLog) o);
                } else if (o instanceof TimePoint) {
                    App.getDataBase().timePointDao().delete((TimePoint) o);
                } else if (o instanceof Provider) {
                    App.getDataBase().providerDao().delete((Provider) o);
                }
            }
        }
    }

    public GeneretedData getLogsData() {
        GeneretedData generatedData = new GeneretedData();
        Calendar tmpDay = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        List<MyLog> myLogs;
        int totalEntries = 0;
        try {
            myLogs = App.getDataBase().myLogDao().get(DataStorage.DB_LIMIT);
            totalEntries = App.getDataBase().myLogDao().getCounts();
        } catch (Exception e) {
            AppLogger.writeLogEx(e);
            AppLogger.printStackTrace(e);
            MainActivityPresenter.addMsg(true, "Exception when trying to read data from a database");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (myLogs != null && !myLogs.isEmpty()) {
            if (totalEntries > myLogs.size()) {
                generatedData.hasOtherData = true;
            }
            for (int i = 0; i < myLogs.size(); i++) {
                MyLog log = myLogs.get(i);
                final long time = log.time;
                if (tmpDay == null) {
                    tmpDay = Calendar.getInstance();
                    tmpDay.setTimeInMillis(time);
                    generatedData.fileName = getFileName(time, true);
                } else if (itNextDay(tmpDay, time)) {
                    if (i < myLogs.size()) generatedData.hasOtherData = true;
                    generatedData.dbHasNextDay = true;
                    generatedData.dataStr = sb.toString();
                    return generatedData;
                }
                final String code = log.code;
                final String msg = log.msg;

                sb.append(formatter.format(time))
                        .append(',');
                sb.append(code)
                        .append(',');
                sb.append(msg)
                        .append('\n');
                generatedData.objDeleteFromDb.add(log);
            }
            generatedData.dataStr = sb.toString();
        }
        return generatedData;
    }

    private static long lastGpsTime = -1;

    public GeneretedData generateData() {
        //if new date fileExist always false;
        GeneretedData generatedData = new GeneretedData();
        String gpsErrStr = "";
        Calendar tmpDay = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        List<TimePoint> timePointList;
        int allTpCounts = 0;

        try {
            timePointList = App.getDataBase().timePointDao().get(DataStorage.DB_LIMIT);
            allTpCounts = App.getDataBase().timePointDao().getCounts();
        } catch (Exception e) {
            AppLogger.writeLogEx(e);
            AppLogger.printStackTrace(e);
            MainActivityPresenter.addMsg(true, "Exception when trying to read data from a database");
            return null;
        }
        if (timePointList != null) {
            StringBuilder sb = new StringBuilder();
            generatedData.hasOtherData = allTpCounts > timePointList.size();
            for (int j = 0; j < timePointList.size(); j++) {
                TimePoint tp = timePointList.get(j);
                final long time = tp.time;
                if (tmpDay == null) {
                    tmpDay = Calendar.getInstance();
                    tmpDay.setTimeInMillis(time);
                    generatedData.fileName = getFileName(time, false);
                } else if (itNextDay(tmpDay, time)) {
                    if(j < timePointList.size())generatedData.hasOtherData = true;
                    generatedData.dataStr = sb.toString();
                    generatedData.dbHasNextDay = true;
                    return generatedData;
                }
                final String airMode = DataStorage.isAirModeCheckingEnabled() ? (tp.airMode == 1 ? ON : OFF) : DISABLED;
                final String timeCh = DataStorage.isTimeChangeCheckingEnabled() ? (tp.timeChange == 1 ? CHANGE : EMPTY) : DISABLED;
                final String locationFake = DataStorage.isFakeCoordCheckingEnabled() ? (tp.fictitious == 1 ? FAKE : EMPTY) : DISABLED;
                final String gpsState = DataStorage.isNetworkCheckingEnabled() ? (tp.gpsState == 1 ? ON : OFF) : DISABLED;
                final String activeNetwork = DataStorage.isNetworkCheckingEnabled() ? tp.activeNetwork : DISABLED;
                final String networkState = DataStorage.isNetworkCheckingEnabled() ? (tp.networkState == 1 ? ON : OFF) : DISABLED;
                final String charging = DataStorage.isChargeCheckingEnabled() ? (tp.charging == 1 ? ON : OFF) : DISABLED;
                final String battery = DataStorage.isBatteryCheckingEnabled() ? ((tp.battery) + "%") : DISABLED;
                final long bootTime = tp.bootTime;
                final String installApp = DataStorage.isAppsCheckingEnabled() ? tp.installApp : DISABLED;
                final String memory = DataStorage.isMemoryCheckingEnabled() ? tp.memory : DISABLED;
                final String satInfo = DataStorage.isSatellitesCheckingEnabled() ? tp.satellite : DISABLED;
                final String appVersion = tp.appVersion;
                final String deniedPerms = DataStorage.isPermissionsCheckingEnabled() ? tp.noPermissions : DISABLED;

                List<Provider> providersList;
                {
                    try {
                        providersList = App.getDataBase().providerDao().getProvidersByTime(time);
                    } catch (Exception e) {
                        AppLogger.printStackTrace(e);
                        AppLogger.writeLogEx(e);
                        MainActivityPresenter.addMsg(true, "Exception when trying to read time point data from a database, time point was deleted");
                        App.getDataBase().timePointDao().delete(tp);
                        continue;
                    }
                    sb.append(formatter.format(time))
                            .append(',');
                    sb.append(formatter.format(bootTime))
                            .append(',');
                    sb.append(airMode)
                            .append(',');
                    sb.append(timeCh)
                            .append(',');
                    sb.append(locationFake)
                            .append(',');
                    sb.append(activeNetwork)
                            .append(',');
                    HashMap<String, Provider> providerHashMap = new HashMap<>();
                    for (Provider provider : providersList) {
                        providerHashMap.put(provider.name, provider);
                    }

                    for (int i = 0; i < PROVIDER_NAME.length; i++) {
                        Provider provider = providerHashMap.get(PROVIDER_NAME[i]);
                        if (PROVIDER_NAME[i].equals("gps")) {
                            if (DataStorage.isGpsErrorCheckingEnabled()) {
                                if (provider == null) {
                                    gpsErrStr = EMPTY;
                                } else {
                                    if (lastGpsTime == -1) {
                                        gpsErrStr = ZERO;
                                    } else if (provider.time - lastGpsTime < 1000) {
                                        gpsErrStr = ONE;
                                    } else {
                                        gpsErrStr = ZERO;
                                    }
                                    lastGpsTime = provider.time;
                                }
                            } else {
                                gpsErrStr = DISABLED;
                            }
                        }
                        if (!DataStorage.isCoordCheckingEnabled()) {
                            sb.append(PROVIDER_NAME[i])
                                    .append(',')
                                    .append(DISABLED)
                                    .append(',')
                                    .append(DISABLED)
                                    .append(',')
                                    .append(DISABLED)
                                    .append(',')
                                    .append(DISABLED)
                                    .append(',')
                                    .append(DISABLED)
                                    .append(',');
                        } else {
                            if (provider == null) {
                                sb.append(PROVIDER_NAME[i])
                                        .append(',')
                                        .append(PROVIDER_NAME[i].equals("gps") ? gpsState : networkState)
                                        .append(',')
                                        .append(NULL)
                                        .append(',')
                                        .append(NULL)
                                        .append(',')
                                        .append(NULL)
                                        .append(',')
                                        .append(NULL)
                                        .append(',');
                            } else {
                                sb.append(provider.name)
                                        .append(',')
                                        .append(PROVIDER_NAME[i].equals("gps") ? gpsState : networkState)
                                        .append(',')
                                        .append(formatter.format(provider.time))
                                        .append(',')
                                        .append(provider.longitude)
                                        .append(',')
                                        .append(provider.latitude)
                                        .append(',')
                                        .append(provider.accurate)
                                        .append(',');
                            }
                        }
                    }
                    sb.append(satInfo)
                            .append(',');
                    sb.append(gpsErrStr)
                            .append(',');
                    sb.append(charging)
                            .append(',');
                    sb.append(battery)
                            .append(',');
                    sb.append(appVersion)
                            .append(',');
                    sb.append(memory)
                            .append(',');
                    sb.append(installApp)
                            .append(',');
                    sb.append(deniedPerms)
                            .append(',');
                    gpsErrStr = EMPTY;
                }
                sb.append('\n');
                generatedData.objDeleteFromDb.add(tp);
                generatedData.objDeleteFromDb.addAll(providersList);
            }
            generatedData.dataStr = sb.toString();
        }
        return generatedData;
    }

    private boolean itNextDay(Calendar lastCalendar, long newTime) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(newTime);
        return lastCalendar.get(Calendar.YEAR) != day.get(Calendar.YEAR) || lastCalendar.get(Calendar.DAY_OF_YEAR) != day.get(Calendar.DAY_OF_YEAR);
    }

    private static String getFileName(final long time, boolean itsLogs) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        StringBuilder stringBuilder = new StringBuilder();
        if (BuildConfig.DEBUG) {
            stringBuilder.append(DeviceInfo.getIMEI()).append("_").append(day).append(".").append(month).append(".").append(year).append("-debug").append(itsLogs ? "_log.csv" : ".csv");
        } else {
            stringBuilder.append(DeviceInfo.getIMEI()).append("_").append(day).append(".").append(month).append(".").append(year).append(itsLogs ? "_log.csv" : ".csv");
        }
        return stringBuilder.toString();
    }

    private static String initTelemetryFileHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Date")
                .append(',');
        sb.append("Boot time")
                .append(',');
        sb.append("AirMode")
                .append(',');
        sb.append("Time changed")
                .append(',');
        sb.append("GPS fake")
                .append(',');
        sb.append("Network")
                .append(',');
        for (int i = 0; i < PROVIDER_NAME.length; i++) {
            String name = PROVIDER_NAME[i];
            sb.append("Provider")
                    .append(',').append("State ").append(name)
                    .append(',').append("Time ").append(name)
                    .append(',').append("Longitude ").append(name)
                    .append(',').append("Latitude ").append(name)
                    .append(',').append("Accurate ").append(name)
                    .append(',');
        }
        sb.append("Satellite info")
                .append(',');
        sb.append("Gps error")
                .append(',');
        sb.append("Charging")
                .append(',');
        sb.append("Battery")
                .append(',');
        sb.append("App version")
                .append(',');
        sb.append("Space (Total|Busy|Free")
                .append(',');
        sb.append("App's")
                .append(',');
        sb.append("Denied permission")
                .append(',');
        /*sb.append('\n');*/
        return sb.toString();
    }


}
