package com.pso.testphone.network;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.pso.testphone.App;
import com.pso.testphone.AppLogger;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.R;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;
import com.pso.testphone.db.Provider;
import com.pso.testphone.db.TimePoint;
import com.pso.testphone.gui.MainActivityPresenter;
import com.pso.testphone.interfaces.ServerTaskListener;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteServerHelper {
    private final static String TAG = RemoteServerHelper.class.getSimpleName();
    private final static String SERVER_UPDATE_ADRESS = "https://apk.ak-cloud.ru/";
    private final static String GOOGLE_ADRESS = "http://google.com";
    private final static String PROPERTIES_FILE = "properties";
    private final static String folder = "uploads";
    private AtomicReference<States> mState = new AtomicReference<>(States.IDLE);
    //private static String fileName;
    private static RemoteServerHelper INSTANCE;
    private ArrayList<TimePoint> timePointsReadyToSend;
    private ArrayList<Provider> providersNeedToDelete;
    private final static int CONNECTION_TIMEOUT = 10000;
    private final Object lock = new Object();
    private static ConnectionStateMonitor mConnectionStateMonitor;
    private static ConnectionChangeReceiver mConnectionChangeReceiver;
    private long lastGpsTime = -1;

    private final int SEND_FILE_MSG = 4431;


    private final String DAY_CHAR = "d";
    private final String HOUR_CHAR = "h";
    private final String MIN_CHAR = "m";
    private final String SEC_CHAR = "s";

    enum States {IDLE, BUSY}

    ;

    private ArrayList<ServerTaskListener> serverTaskListeners = new ArrayList<>();

    public void bindServerTaskListener(ServerTaskListener listener) {
        serverTaskListeners.add(listener);
    }

    public void unBindServerTaskListener(ServerTaskListener listener) {
        serverTaskListeners.remove(listener);
    }

    public boolean deleteFile(String fileName) {
        File file = new File(App.getContext().getFilesDir() + "/" + fileName);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    public static RemoteServerHelper getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new RemoteServerHelper();
            reqisterConnectionListener();
        }
        return INSTANCE;
    }


    private static void reqisterConnectionListener() {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mConnectionChangeReceiver = new ConnectionChangeReceiver();
        App.getContext().registerReceiver(mConnectionChangeReceiver, intentFilter);
    }


    private static String generateFileName(final long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        StringBuilder stringBuilder = new StringBuilder();
        if (BuildConfig.DEBUG) {
            stringBuilder.append(DeviceInfo.getIMEI()).append("_").append(day).append(".").append(month).append(".").append(year).append("-debug").append(".csv");
        } else {
            stringBuilder.append(DeviceInfo.getIMEI()).append("_").append(day).append(".").append(month).append(".").append(year).append(".csv");
        }
        return stringBuilder.toString();
    }

    public void downloadUpdateFile() {
        if (mState.get() == States.IDLE) {
            App.getBgHandler().post(this::downloadFile);
        }
    }

    private void notifyListeners(String task) {
        for (ServerTaskListener serverTaskListener : serverTaskListeners) {
            serverTaskListener.onTaskDone(task);
        }
    }

    private boolean setWriteInterval(String write) {
        boolean result = false;
        if (write.isEmpty()) {
            MainActivityPresenter.addMsg(true, "Failed set write inteval");
            AppLogger.e(TAG, "Empty write value in properties file");
        } else {
            try {
                long timeValue = Long.parseLong((write.substring(0, write.indexOf(' '))));
                String timeUnitStr = write.substring(write.lastIndexOf(' ') + 1);
                long newInterval = convertToCalendarValue(timeUnitStr, timeValue);
                if (newInterval != -1) {
                    MainActivityPresenter.addMsg(true, "Write interval = " + timeValue + " " + timeUnitStr);
                    DataStorage.setWriteInterval(newInterval);
                    result = true;
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
        return result;
    }

    private boolean setUploadInterval(String upLoad) {
        boolean result = false;
        if (upLoad.isEmpty()) {
            MainActivityPresenter.addMsg(true, "Failed set upload inteval");
            AppLogger.e(TAG, "Empty upLoad value in properties file");
        } else {
            try {
                long timeValue = Long.parseLong((upLoad.substring(0, upLoad.indexOf(' '))));
                String timeUnitStr = upLoad.substring(upLoad.lastIndexOf(' ') + 1);
                long newInterval = convertToCalendarValue(timeUnitStr, timeValue);
                if (newInterval != -1) {
                    MainActivityPresenter.addMsg(true, "Upload interval = " + timeValue + " " + timeUnitStr);
                    DataStorage.setUploadInterval(newInterval);
                    result = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    private boolean setUpdateInterval(String chUpdate) {
        boolean result = false;
        if (chUpdate.isEmpty()) {
            MainActivityPresenter.addMsg(true, "Failed set update inteval");
            AppLogger.e(TAG, "Empty chUpdate value in properties file");
        } else {
            try {
                long timeValue = Long.parseLong((chUpdate.substring(0, chUpdate.indexOf(' '))));
                String timeUnitStr = chUpdate.substring(chUpdate.lastIndexOf(' ') + 1);
                long newInterval = convertToCalendarValue(timeUnitStr, timeValue);
                if (newInterval != -1) {
                    MainActivityPresenter.addMsg(true, "Update interval = " + timeValue + " " + timeUnitStr);
                    DataStorage.setUpdateInterval(newInterval);
                }
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    private boolean setShowRebootMsgTime(String time) {
        boolean result = false;
        if (time.isEmpty()) {
            MainActivityPresenter.addMsg(true, "Failed set show reboot msg time value");
            AppLogger.e(TAG, "Failed set show reboot msg time value");
        } else {
            try {
                long timeValue = Long.parseLong((time.substring(0, time.indexOf(' '))));
                String timeUnitStr = time.substring(time.lastIndexOf(' ') + 1);
                long newInterval = convertToCalendarValue(timeUnitStr, timeValue);
                if (newInterval != -1) {
                    MainActivityPresenter.addMsg(true, "Show reboot msg time = " + timeValue + " " + timeUnitStr);
                    DataStorage.setShowRebootMsgInTime(newInterval);
                    result = true;
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
        return result;
    }

    private long convertToCalendarValue(String timeUnitStr, long timeValue) {
        switch (timeUnitStr) {
            case DAY_CHAR:
                return TimeUnit.DAYS.toMillis(timeValue);
            case HOUR_CHAR:
                return TimeUnit.HOURS.toMillis(timeValue);
            case MIN_CHAR:
                return TimeUnit.MINUTES.toMillis(timeValue);
            case SEC_CHAR:
                return TimeUnit.SECONDS.toMillis(timeValue);

        }
        return -1;
    }

    private boolean setUploadServerAddress(String uploadServerAddress) {
        if (uploadServerAddress.isEmpty()) return false;
        if (uploadServerAddress.equals(DataStorage.getUploadServerAddress())) return true;

        DataStorage.setUploadServerAddress(uploadServerAddress);
        return true;
    }

    private boolean setAdminPassword(String password) {
        if (password.isEmpty()) return false;
        DataStorage.setAdminPassword(password);
        return true;
    }

    private boolean needUpdate(String version) {
        boolean newVersionready;
        Double curVersion = Double.parseDouble(BuildConfig.VERSION_NAME);
        if (version.isEmpty()) {
            MainActivityPresenter.addMsg(true, "Failed read new version app");
            AppLogger.e(TAG, "Empty version value in properties file");
            return false;
        }
        Double newVersion = Double.parseDouble(version);
        newVersionready = newVersion > curVersion;
        if (newVersionready) {
            MainActivityPresenter.addMsg(true, "App need an update");
        } else {
            MainActivityPresenter.addMsg(true, "App version current");
        }
        return newVersionready;
    }

    private void clearTempSendTimePoint() {
        timePointsReadyToSend = null;
        providersNeedToDelete = null;
    }

    private void deleteSentTimePointsFromDB() {
        if (timePointsReadyToSend != null && !timePointsReadyToSend.isEmpty()) {
            for (TimePoint tp : timePointsReadyToSend) {
                App.getDataBase().timePointDao().delete(tp);
            }
        }
        if (providersNeedToDelete != null && !providersNeedToDelete.isEmpty()) {
            for (Provider provider : providersNeedToDelete) {
                App.getDataBase().providerDao().delete(provider);
            }
        }
        clearTempSendTimePoint();
    }

    public boolean fileExists(final FTPClient ftpClient, final String fileName) throws IOException {
        String[] files = ftpClient.listNames();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].equals(fileName))
                    return true;
            }
        }
        return false;
    }

    public void sendFileToServer() {
        if (mState.get() == States.IDLE) {
            if (DataStorage.networkAvailable.get()) {
                App.getBgHandler().post(this::sendFile);
            } else {
                MainActivityPresenter.addMsg(true, App.getContext().getString(R.string.check_internet_connection));
            }
        } else {
            MainActivityPresenter.addMsg(true, "File upload is delayed and the network is busy");
        }
    }

    private class GenerateData {
        private String data;
        private String fileName;
        private boolean hasOtherDayData;
    }

    private GenerateData generateDataString(final FTPClient ftpClient) {
        //if new date fileExist always false;
        GenerateData generatedData = new GenerateData();
        String gpsErrStr = "";
        Calendar tmpDay = null;
        boolean needHeaderInit = false;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        List<TimePoint> timePointList;
        try {
            timePointList = App.getDataBase().timePointDao().getAll();
        } catch (Exception e) {
            AppLogger.printStackTrace(e);
            MainActivityPresenter.addMsg(true, "Exception when trying to read data from a database");
            return null;
        }
        timePointsReadyToSend = new ArrayList<>();
        providersNeedToDelete = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        if (timePointList != null) {
            for (TimePoint tp : timePointList) {
                final long time = tp.time;
                if (tmpDay == null) {
                    tmpDay = Calendar.getInstance();
                    tmpDay.setTimeInMillis(time);
                    generatedData.fileName = generateFileName(time);
                    try {
                        if (!fileExists(ftpClient, generatedData.fileName)) {
                            needHeaderInit = true;
                        }
                    } catch (IOException e) {
                        AppLogger.printStackTrace(e);
                        MainActivityPresenter.addMsg(true, "Couldn't get a list of files on the server");
                        return null;
                    } catch (Exception e) {
                        AppLogger.printStackTrace(e);
                        return null;
                    }
                } else if (timePoinIsOtherDate(tmpDay, time)) {
                    generatedData.hasOtherDayData = true;
                    generatedData.data = sb.toString();
                    return generatedData;
                }
                final String airMode = (tp.airMode == 1 ? ON : OFF);
                final String timeCh = (tp.timeChange == 1 ? CHANGE : EMPTY);
                final String locationFake = (tp.fictitious == 1 ? FAKE : EMPTY);
                final String gpsState = (tp.gpsState == 1 ? ON : OFF);
                final String activeNetwork = tp.activeNetwork;
                final String networkState = (tp.networkState == 1 ? ON : OFF);
                final String charging = (tp.charging == 1 ? ON : OFF);
                final String battery = ((tp.battery) + "%");
                final long bootTime = tp.bootTime;
                final String installApp = tp.installApp;
                final String memory = tp.memory;
                final String satInfo = tp.satellite;
                final String appVersion = tp.appVersion;

                List<Provider> providersList = null;
                try {
                    providersList = App.getDataBase().providerDao().getProvidersByTime(time);
                } catch (Exception e) {
                    AppLogger.printStackTrace(e);
                    MainActivityPresenter.addMsg(true, "Exception when trying to read time point data from a database, time point was deleted");
                    App.getDataBase().timePointDao().delete(tp);
                    continue;
                }
                if (needHeaderInit) {
                    initCSVCollumsName(sb, providersList);
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
                    }
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
                sb.append(installApp);
                if (needHeaderInit) {
                    sb.append(',')
                            .append(DeviceInfo.getFull())
                            .append(',');
                    needHeaderInit = false;
                }
                sb.append('\n');
                gpsErrStr = EMPTY;
                timePointsReadyToSend.add(tp);
                providersNeedToDelete.addAll(providersList);
            }
            generatedData.data = sb.toString();
        }
        return generatedData;
    }

    private boolean timePoinIsOtherDate(Calendar lastCalendar, long newTime) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(newTime);
        return lastCalendar.get(Calendar.YEAR) != day.get(Calendar.YEAR) || lastCalendar.get(Calendar.DAY_OF_YEAR) != day.get(Calendar.DAY_OF_YEAR);
    }

    private final String[] PROVIDER_NAME = {"network", "gps"};
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


    private void initCSVCollumsName(StringBuilder sb, List<Provider> providerList) {
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
        sb.append("Device info")
                .append(',');
        sb.append('\n');
    }

    private void sendFile() {
        synchronized (lock) {
            if (!DataStorage.networkAvailable.get()) return;
            mState.set(States.BUSY);
            final String uploadServer = DataStorage.getUploadServerAddress();
            boolean done = false;
            boolean hasDateData = true;
            FTPClient ftpClient = new FTPClient();
            if (/*isURLReachable(uploadServer) && */connectionToUploadServer(ftpClient, uploadServer)) {
                while (hasDateData && DataStorage.networkAvailable.get()) {
                    GenerateData gData = generateDataString(ftpClient);
                    if (gData != null && DataStorage.networkAvailable.get()) {
                        if (gData.data != null && !gData.data.isEmpty()) {
                            InputStream in;
                            try {
                                in = new ByteArrayInputStream(gData.data.getBytes("windows-1251"));
                                done = ftpClient.appendFile(gData.fileName, in);
                                in.close();
                            } catch (UnsupportedEncodingException e) {
                                AppLogger.printStackTrace(e);
                                done = false;
                            } catch (IOException e) {
                                AppLogger.printStackTrace(e);
                                done = false;
                            }
                            hasDateData = gData.hasOtherDayData;
                            if (done) {
                                deleteSentTimePointsFromDB();
                            } else {
                                clearTempSendTimePoint();
                            }
                        } else {
                            MainActivityPresenter.addMsg(true, "No data to send");
                            hasDateData = false;
                        }
                    } else {
                        hasDateData = false;
                    }
                }
                disconnectUploadServer(ftpClient);
            }
            if (done) {
                MainActivityPresenter.addMsg(true, "Upload done!");
                DataStorage.setLastUploadTime(System.currentTimeMillis());
                AppLogger.e(TAG, "Send file to server success");
            } else {
                AppLogger.e(TAG, "Send file to server failure");
            }
            mState.set(States.IDLE);
        }
    }

    private boolean connectionToUploadServer(final FTPClient ftpClient, final String server) {
        try {
            MainActivityPresenter.addMsg(true, "Connection...");
            ftpClient.setDefaultTimeout(CONNECTION_TIMEOUT);
            ftpClient.connect(server);
            if (ftpClient.login(DataStorage.getLogin(), DataStorage.getPass())) {
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
                if (ftpClient.changeWorkingDirectory(folder)) {
                    MainActivityPresenter.addMsg(true, "Upload...");
                    return true;
                }
            }
        } catch (SocketTimeoutException sE) {
            MainActivityPresenter.addMsg(true, "SocketTimeoutException. No response from the server within " + CONNECTION_TIMEOUT + " mil. sec.");
            AppLogger.printStackTrace(sE);
            AppLogger.e(RemoteServerHelper.class.getSimpleName(), "Connection failed");
        }catch (UnknownHostException uE){
            MainActivityPresenter.addMsg(true, "The hostname cannot be resolved.");
            AppLogger.printStackTrace(uE);
            AppLogger.e(RemoteServerHelper.class.getSimpleName(), "Connection failed");
        } catch (IOException e) {
            MainActivityPresenter.addMsg(true, "Upload failed. Check internet connection!");
            AppLogger.printStackTrace(e);
            AppLogger.e(RemoteServerHelper.class.getSimpleName(), "Connection failed");
        }
        return false;
    }

    private void disconnectUploadServer(final FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                //ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadFile() {
        if (!DataStorage.networkAvailable.get()) {
            MainActivityPresenter.addMsg(true, App.getContext().getString(R.string.check_internet_connection));
            return;
        }
        synchronized (lock) {
            mState.set(States.BUSY);
            MainActivityPresenter.clearMsg();
            MainActivityPresenter.addMsg(true, "Check updates...");
            BufferedReader reader;
            StringBuilder sb;
            String finalResultStr = "";
            HttpURLConnection urlConnection = null;
            try {
                String urlStr = SERVER_UPDATE_ADRESS + PROPERTIES_FILE;
                URL url = new URL(urlStr);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                MainActivityPresenter.addMsg(true, "Connection done!");
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                reader = new BufferedReader(new InputStreamReader(in));
                sb = new StringBuilder();
                String line;
                try {
                    MainActivityPresenter.addMsg(true, "Downloading the properties file.");
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    MainActivityPresenter.addMsg(true, "Done.");
                    finalResultStr = sb.toString();
                } catch (IOException ioe) {
                    MainActivityPresenter.addMsg(true, "Failed to read the properties file! Check the file structure!");
                    AppLogger.e(TAG, ioe.getMessage() + "Failed to read the properties file! Check the file structure!");
                }
            } catch (MalformedURLException mue) {
                AppLogger.e(TAG, "malformed url error", mue);
            } catch (IOException ioe) {
                MainActivityPresenter.addMsg(true, "Check update failed! Check internet connection!");
                AppLogger.e(TAG, ioe.getMessage() + ". Check connection");
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            } catch (SecurityException se) {
                MainActivityPresenter.addMsg(true, "Check update failed! Check permission INTERNET ");
                AppLogger.e(TAG, se.getMessage() + ". Check permission INTERNET");
            }
            if (!finalResultStr.isEmpty()) {
                parseAndSetProperies(finalResultStr);
            }
            mState.set(States.IDLE);
        }
    }

    @SuppressLint("SetWorldReadable")
    private void downloadNewVersionApp(final String version) {
        if (!DataStorage.networkAvailable.get()) return;
        synchronized (lock) {
            mState.set(States.BUSY);
            boolean result = false;
            byte[] buffer = new byte[4096];
            String remoteFile = DataStorage.APP_NAME + version + ".apk";
            HttpURLConnection urlConnection = null;
            String urlStr = SERVER_UPDATE_ADRESS + remoteFile;
            File downloadFile = new File(App.getContext().getFilesDir() + "/" + remoteFile);
            if (downloadFile.exists()) {
                MainActivityPresenter.addMsg(true, "Update is ready");
                result = true;
            } else {
                MainActivityPresenter.addMsg(true, "Download update");
                try {
                    downloadFile.createNewFile();
                } catch (IOException e) {
                    AppLogger.printStackTrace(e);
                }
                URL url;
                InputStream in = null;
                OutputStream out = null;
                try {
                    url = new URL(urlStr);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                    in = new BufferedInputStream(urlConnection.getInputStream());
                    out = new BufferedOutputStream(new FileOutputStream(downloadFile));
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        AppLogger.i(TAG, "Download " + len + " bytes");
                        MainActivityPresenter.addMsg(false, ".");
                        out.write(buffer, 0, len);
                    }
                    out.close();
                    MainActivityPresenter.addMsg(true, "Done.");
                    result = downloadFile.setReadable(true, false);
                } catch (MalformedURLException e) {
                    AppLogger.printStackTrace(e);
                } catch (IOException e) {
                    AppLogger.printStackTrace(e);
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (result) {
                DataStorage.setUpdateFileName(remoteFile);
                App.getMainHandler().post(() -> {
                    notifyListeners(ServerTaskListener.UPDATE_APP);
                });
            }
        }
        mState.set(States.IDLE);
    }

    private void parseAndSetProperies(String finalResultStr) {
        String version = "";
        String chUpdate = "";
        String upLoad = "";
        String write = "";
        String showRebootMsg = "";
        String uploadServerAddress = "";
        String password = "";
        try {
            JSONObject jsonObject = new JSONObject(finalResultStr);
            String VERSION_STR = "version";
            version = jsonObject.getString(VERSION_STR);
            String UPDATE_STR = "ch_update_int";
            chUpdate = jsonObject.getString(UPDATE_STR);
            String UNLOAD_STR = "unload_int";
            upLoad = jsonObject.getString(UNLOAD_STR);
            String WR_INT_STR = "write_int";
            write = jsonObject.getString(WR_INT_STR);
            String SHOW_REBOOT_MSG = "show_reboot_msg";
            showRebootMsg = jsonObject.getString(SHOW_REBOOT_MSG);
            String UPLOAD_SERVER = "upload_server";
            uploadServerAddress = jsonObject.getString(UPLOAD_SERVER);
            String PASSWORD = "password_admin_menu";
            password = jsonObject.getString(PASSWORD);
        } catch (JSONException e) {
            AppLogger.e(TAG, "Сan't read one of the values. Check the signature");
            e.printStackTrace();
            return;
        }

        if (needUpdate(version)) {
            String finalVersion = version;
            App.getBgHandler().postDelayed(() -> {
                downloadNewVersionApp(finalVersion);
            }, 50);
        }
        if (BuildConfig.DEBUG) {
            upLoad = "2 m";
            chUpdate = "3 m";
        }

        if (setUpdateInterval(chUpdate) && setUploadInterval(upLoad) && setWriteInterval(write) && setShowRebootMsgTime(showRebootMsg)
                && setUploadServerAddress(uploadServerAddress) && setAdminPassword(password)) {
            AppLogger.i(TAG, "New values set");
            DataStorage.setNextUpdateTime(System.currentTimeMillis() + DataStorage.getUpdateInterval());
        }
    }

    static public boolean isURLReachable(String serverName) {
        AppLogger.witnAdditionalMsg("Check server available.");

        ConnectivityManager cm = (ConnectivityManager) App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            try {
                serverName = serverNameFormating(serverName);
                URL url = new URL(serverName);   // Change to "http://google.com" for www  test.
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(5 * 1000);          // 10 s.
                urlc.connect();
                if (urlc.getResponseCode() == 200) {        // 200 = "OK" code (http connection is fine).
                    AppLogger.i(TAG, "Upload server is available");
                    AppLogger.witnAdditionalMsg("Server available!");
                    urlc.disconnect();
                    return true;
                } else {
                    urlc.disconnect();
                    return false;
                }
            } catch (SocketTimeoutException sE) {
                MainActivityPresenter.addMsg(false, "SocketTimeoutException. No response from the server within " + CONNECTION_TIMEOUT + " mil. sec.");
                AppLogger.printStackTrace(sE);
                AppLogger.e(RemoteServerHelper.class.getSimpleName(), "Connection failed");
                return false;
            } catch (IOException e) {
                MainActivityPresenter.addMsg(false, "IOException. Failed!");
                AppLogger.printStackTrace(e);
                AppLogger.e(RemoteServerHelper.class.getSimpleName(), "Connection failed");
                return false;
            }
        } else {
            AppLogger.witnAdditionalMsg(App.getContext().getString(R.string.check_internet_connection));
            return false;
        }
    }

    private static String serverNameFormating(String serverName) {
        if(!serverName.contains("http")){
            serverName = "https://" + serverName;
        }
        return serverName;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {
        final NetworkRequest networkRequest;

        public ConnectionStateMonitor() {
            networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
        }

        public void enable(Context context) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.registerNetworkCallback(networkRequest, this);
        }

        // Likewise, you can have a disable method that simply calls ConnectivityManager.unregisterNetworkCallback(NetworkCallback) too.

        @Override
        public void onAvailable(Network network) {

            Toast.makeText(App.getContext(), "ConnectionStateMonitor", Toast.LENGTH_SHORT).show();

            // Do what you need to do here
        }
    }

    public static class ConnectionChangeReceiver extends BroadcastReceiver {
        public static String activeNetInfoStr = "";

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo == null || !activeNetInfo.isAvailable()) {
                DataStorage.networkAvailable.set(false);
                activeNetInfoStr = "";
                return;
            }
            if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                activeNetInfoStr = "WIFI";
                DataStorage.networkAvailable.set(true);
                return;
            }

            int netSubtype = activeNetInfo.getSubtype();
            switch (netSubtype) {
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    activeNetInfoStr = "2G";
                    DataStorage.networkAvailable.set(false);
                    break;
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_GSM:
                    activeNetInfoStr = "2G";
                    DataStorage.networkAvailable.set(true);
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                    activeNetInfoStr = "3G";
                    DataStorage.networkAvailable.set(true);
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                    activeNetInfoStr = "4G";
                    DataStorage.networkAvailable.set(true);
                    break;
                case TelephonyManager.NETWORK_TYPE_NR:
                    activeNetInfoStr = "5G";
                    DataStorage.networkAvailable.set(true);
                    break;
                default:
                    activeNetInfoStr = "";
                    DataStorage.networkAvailable.set(false);
                    break;
            }
        }
    }
}
