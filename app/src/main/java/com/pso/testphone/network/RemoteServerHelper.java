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
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.pso.testphone.App;
import com.pso.testphone.AppLogger;
import com.pso.testphone.BuildConfig;
import com.pso.testphone.data.Codes;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.data.DeviceInfo;
import com.pso.testphone.db.DBHelper;
import com.pso.testphone.gui.GuiHelper;
import com.pso.testphone.gui.MainActivityPresenter;
import com.pso.testphone.interfaces.ServerTaskListener;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.ipinfo.api.IPInfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.pso.testphone.data.Codes.IOEXCEPTION_CODE;
import static com.pso.testphone.data.Codes.IOEXCEPTION_MSG;
import static com.pso.testphone.data.Codes.MALFORMED_URL_EXCEPTION_CODE;
import static com.pso.testphone.data.Codes.MALFORMED_URL_EXCEPTION_MSG;
import static com.pso.testphone.data.Codes.NETWORK_NOT_AVAILABLE_MSG;
import static com.pso.testphone.data.Codes.VALUES_NOT_SET;
import static com.pso.testphone.data.Codes.VALUES_SET;
import static com.pso.testphone.data.Codes.VALUES_SET_CODE;

public class RemoteServerHelper {
    private final static String TAG = RemoteServerHelper.class.getSimpleName();
    private final static String SERVER_UPDATE_ADRESS = "https://apk.ak-cloud.ru/";
    private final static String GOOGLE_ADRESS = "http://google.com";
    private final static String PROPERTIES_FILE = "properties";
    private final static String PROPERTIES_FILE_TEST = "properties_test";
    private final static String folder = "uploads";
    private final static String logs_folder = "logs";
    private final static int REMOVED_TASK_TIME_OUT = 1000 * 10;
    private AtomicReference<States> mState = new AtomicReference<>(States.IDLE);
    //private static String fileName;
    private static RemoteServerHelper INSTANCE;
    private final static int CONNECTION_TIMEOUT = 10000;
    private final Object lock = new Object();
    private static ConnectionStateMonitor mConnectionStateMonitor;
    private static ConnectionChangeReceiver mConnectionChangeReceiver;
    private static final String[] IP_SERVICE_URLS = {"https://api.my-ip.io/ip.json", "https://api.myip.com/"};
    private AtomicReference<LinkedList<TaskType>> taskQueue = new AtomicReference<>(new LinkedList<>());
    private AtomicReference<TaskType> currTask = new AtomicReference<>();
    private Handler mainHandler = new Handler();
    private static String mNewAppVersion = "";

    private final String DAY_CHAR = "d";
    private final String HOUR_CHAR = "h";
    private final String MIN_CHAR = "m";
    private final String SEC_CHAR = "s";

    private static final String AIR_MODE = "air_mode";
    private static final String TIME_CH = "time_change";
    private static final String FAKE_COORD = "used_fake_coordinates";
    private static final String COORD = "coordinates";
    private static final String SATELLITES = "satellites";
    private static final String GPS_ERROR = "gps_error";
    private static final String CHARGING = "charging";
    private static final String BATTERY = "battery";
    private static final String MEMORY = "memory_cpace";
    private static final String APPS = "installed_apps";
    private static final String DENIED_PERMS = "denied_perms";
    private static final String NETWORK_STATES = "network_states";

    private static final String ON = "on";
    private static final String OFF = "off";


    enum States {IDLE, BUSY}

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

    private boolean networkAvailableAndFree() {
        return DataStorage.networkAvailable.get() && mState.get() == States.IDLE;
    }

    private void delayTask(TaskType task) {
        if (taskQueue.get().contains(task))
            return;
        taskQueue.get().addLast(task);
        AppLogger.writeLog(Codes.TASK_CODE, Codes.TASK_DELAY_MSG + task.name());
        MainActivityPresenter.addMsg(true, Codes.TASK_DELAY_MSG + task.name());
    }

    private TaskType getNextTask() {
        return taskQueue.get().getFirst();
    }

    private void removeFirstTask() {
        taskQueue.get().removeFirst();
    }

    private void notifyListeners(String task) {
        for (ServerTaskListener serverTaskListener : serverTaskListeners) {
            serverTaskListener.onTaskDone(task);
        }
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

    public void goToDirectory(final FTPClient ftpClient, final String directory) throws IOException {
        ftpClient.changeWorkingDirectory(directory);
    }

    public boolean directoryExist(final FTPClient ftpClient, final String directoryName) throws IOException {
        ftpClient.cwd(directoryName);
        return FTPReply.isPositiveCompletion(ftpClient.getReplyCode());
    }

    public void makeMainLogFolder(final FTPClient ftpClient) throws IOException {
        ftpClient.makeDirectory(DeviceInfo.getIMEI());
    }

    public enum TaskType {Idle, SendData, SendLogs, getUpdate, getAppNewVer, getAssistantNewVer}

    public void completeTask(TaskType taskType) {
        if (currTask.get() == taskType)
            return;
        if (!networkAvailableAndFree()) {
            AppLogger.writeLog(Codes.NETWORK_NOT_AVAILABLE_CODE, NETWORK_NOT_AVAILABLE_MSG);
            MainActivityPresenter.addMsg(true, NETWORK_NOT_AVAILABLE_MSG);
            delayTask(taskType);
            return;
        }
        switch (taskType) {
            case SendData:
                currTask.set(TaskType.SendData);
                App.getBgHandler().post(this::sendFile);
                break;
            case SendLogs:
                currTask.set(TaskType.SendLogs);
                App.getBgHandler().post(this::sendLogsFile);
                break;
            case getUpdate:
                currTask.set(TaskType.getUpdate);
                App.getBgHandler().post(this::downloadPropertiesFile);
                break;
            case getAppNewVer:
                currTask.set(TaskType.getAppNewVer);
                App.getBgHandler().post(() -> {
                    String fileName = DataStorage.APP_NAME + DataStorage.getAppAvailableVersion() + ".apk";
                    downloadFile(fileName);
                });
                break;
            case getAssistantNewVer:
                currTask.set(TaskType.getAssistantNewVer);
                App.getBgHandler().post(() -> {
                    String fileName = DataStorage.APK_ASSISTANT_FILE_NAME;
                    downloadFile(fileName);
                });
                break;
        }
    }

    public void completeFirstTask() {
        if (!taskQueue.get().isEmpty()) {
            completeTask(taskQueue.get().getFirst());
        }
    }

    private void sendLogsFile() {
        synchronized (lock) {
            currTask.set(TaskType.SendLogs);
            mState.set(States.BUSY);
            final String uploadServer = DataStorage.getUploadServerAddress();
            boolean done = false;
            boolean hasDateData = true;
            FTPClient ftpClient = new FTPClient();
            if (connectionToUploadServer(ftpClient, uploadServer)) {
                try {
                    goToDirectory(ftpClient, logs_folder);
                    if (!directoryExist(ftpClient, DeviceInfo.getIMEI())) {
                        makeMainLogFolder(ftpClient);
                    }
                    goToDirectory(ftpClient, DeviceInfo.getIMEI());
                } catch (IOException e) {
                    AppLogger.writeLogEx(e);
                    disconnectServer(ftpClient);
                    removeTaskAndGoToIdle(currTask.get());
                    return;
                }
                while (hasDateData && DataStorage.networkAvailable.get()) {
                    DBHelper.GeneretedData gData = DBHelper.getInstance().getLogsData();
                    if (gData != null && gData.data != null && !gData.data.isEmpty()) {
                        InputStream in = null;
                        try {
                            MainActivityPresenter.addMsg(true, "Sending logs");
                            AppLogger.writeLog(Codes.START_UNLOAD_FILE_CODE, Codes.START_UNLOAD_FILE_MSG + " name = " + gData.fileName);
                            int bytes = 0;
                            for (DBHelper.Data d : gData.data) {
                                in = new ByteArrayInputStream(d.dataStr.getBytes("windows-1251"));
                                bytes = bytes + d.dataStr.getBytes().length;
                                if (ftpClient.appendFile(gData.fileName, in)) {
                                    gData.objDeleteFromDb.addAll(d.objs);
                                }
                            }
                            in.close();
                            AppLogger.writeLog(Codes.UNLOAD_FILE_FINISH_CODE, Codes.UNLOAD_FILE_FINISH_MSG + " name = " + gData.fileName +
                                    " size = " + DeviceInfo.bytesToHuman(bytes));
                            MainActivityPresenter.addMsg(true, "Send logs file " + gData.fileName + " done!");
                            hasDateData = gData.hasOtherDayData;
                        } catch (UnsupportedEncodingException e) {
                            AppLogger.writeLogEx(e);
                            hasDateData = false;
                        } catch (IOException e) {
                            AppLogger.writeLogEx(e);
                            hasDateData = false;
                        }
                        DBHelper.deleteSendingDataFromDb(gData.objDeleteFromDb);
                    } else {
                        AppLogger.writeLog(Codes.NO_DATA_CODE, Codes.NO_DATA_MSG);
                        MainActivityPresenter.addMsg(true, Codes.NO_DATA_MSG);
                        hasDateData = false;
                    }
                }
                disconnectServer(ftpClient);
                DataStorage.setLastUnloadLogsTime(System.currentTimeMillis());
            }
            removeTaskAndGoToIdle(currTask.get());
        }
    }

    private void sendFile() {
        synchronized (lock) {
            currTask.set(TaskType.SendData);
            mState.set(States.BUSY);
            final String uploadServer = DataStorage.getUploadServerAddress();
            boolean hasDateData = true;
            FTPClient ftpClient = new FTPClient();
            if (connectionToUploadServer(ftpClient, uploadServer)) {
                try {
                    ftpClient.changeWorkingDirectory(folder);
                } catch (IOException e) {
                    AppLogger.writeLogEx(e);
                    disconnectServer(ftpClient);
                    removeTaskAndGoToIdle(currTask.get());
                    return;
                }
                while (hasDateData && DataStorage.networkAvailable.get()) {
                    DBHelper.GeneretedData gData = DBHelper.getInstance().generateDataString();
                    if (gData != null && gData.data != null && !gData.data.isEmpty()) {
                        InputStream in = null;
                        int bytes = 0;
                        MainActivityPresenter.addMsg(true, "Sending data file");
                        try {
                            AppLogger.writeLog(Codes.START_UNLOAD_FILE_CODE, Codes.START_UNLOAD_FILE_MSG + " name = " + gData.fileName);
                            String headerStr = "";
                            if (!fileExists(ftpClient, gData.fileName)) {
                                headerStr = DBHelper.TelemetryFileHeader + ",Device info: " + DeviceInfo.getFull() + '\n';
                                in = new ByteArrayInputStream(headerStr.getBytes("windows-1251"));
                                ftpClient.appendFile(gData.fileName, in);
                                bytes = bytes + headerStr.getBytes().length;
                            }
                            for (DBHelper.Data d : gData.data) {
                                in = new ByteArrayInputStream(d.dataStr.getBytes("windows-1251"));
                                if (ftpClient.appendFile(gData.fileName, in)) {
                                    bytes = bytes + d.dataStr.getBytes().length;
                                    gData.objDeleteFromDb.addAll(d.objs);
                                }
                            }
                            in.close();
                            AppLogger.writeLog(Codes.UNLOAD_FILE_FINISH_CODE, Codes.UNLOAD_FILE_FINISH_MSG+ " name = " + gData.fileName +
                                    " size = " + DeviceInfo.bytesToHuman(bytes));
                            MainActivityPresenter.addMsg(true, "Send data file " + gData.fileName + " done!");
                            hasDateData = gData.hasOtherDayData;
                        } catch (UnsupportedEncodingException e) {
                            AppLogger.writeLogEx(e);
                            hasDateData = false;
                        } catch (IOException e) {
                            hasDateData = false;
                            AppLogger.writeLogEx(e);
                        }
                        DBHelper.deleteSendingDataFromDb(gData.objDeleteFromDb);
                    } else {
                        AppLogger.writeLog(Codes.NO_DATA_CODE, Codes.NO_DATA_MSG);
                        MainActivityPresenter.addMsg(true, Codes.NO_DATA_MSG);
                        hasDateData = false;
                    }
                }
                disconnectServer(ftpClient);
                DataStorage.setLastUnloadDataFileTime(System.currentTimeMillis());
            }
            removeTaskAndGoToIdle(currTask.get());
        }
    }

    private boolean connectionToUploadServer(final FTPClient ftpClient, final String server) {
        try {
            AppLogger.writeLog(Codes.START_CONNECTION_CODE, Codes.START_CONNECTION_MSG + " " + server);
            AppLogger.writeNetworkInfoLog();
            MainActivityPresenter.addMsg(true, Codes.START_CONNECTION_MSG);

            ftpClient.setDefaultTimeout(CONNECTION_TIMEOUT);
            ftpClient.connect(server);
            ftpClient.login(DataStorage.getLogin(), DataStorage.getPass());
            boolean loget = FTPReply.isPositiveCompletion(ftpClient.getReplyCode());
            if (loget) {
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.ASCII_FILE_TYPE);

                AppLogger.writeLog(Codes.CONNECTION_DONE_CODE, Codes.CONNECTION_DONE_MSG);
                MainActivityPresenter.addMsg(true, Codes.CONNECTION_DONE_MSG);
                return true;
            }
        } catch (SocketTimeoutException sE) {
            AppLogger.writeLogEx(sE);
        } catch (UnknownHostException uE) {
            AppLogger.writeLogEx(uE);
        } catch (IOException e) {
            AppLogger.writeLogEx(e);
        }
        AppLogger.writeLog(Codes.CONNECTION_FAILED_CODE, Codes.CONNECTION_FAILED_MSG);
        MainActivityPresenter.addMsg(true, Codes.CONNECTION_FAILED_MSG);
        return false;
    }

    private void disconnectServer(final FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                AppLogger.writeLog(Codes.DISCONNECT_CODE, Codes.DISCONNECT_MSG + " " + ftpClient.getPassiveHost() + ":" + ftpClient.getPassivePort());
                MainActivityPresenter.addMsg(true, Codes.DISCONNECT_MSG);
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadPropertiesFile() {
        synchronized (lock) {
            currTask.set(TaskType.getUpdate);
            mState.set(States.BUSY);
            BufferedReader reader;
            StringBuilder sb;
            String finalResultStr = "";
            HttpURLConnection urlConnection = null;
            try {
                String urlStr = SERVER_UPDATE_ADRESS + (BuildConfig.DEBUG ? PROPERTIES_FILE_TEST : PROPERTIES_FILE);
                AppLogger.writeLog(Codes.START_CONNECTION_CODE, Codes.START_CONNECTION_MSG + " " + urlStr);
                AppLogger.writeNetworkInfoLog();
                URL url = new URL(urlStr);
                urlConnection = (HttpURLConnection) url.openConnection();
                AppLogger.writeLog(Codes.CONNECTION_DONE_CODE, Codes.CONNECTION_DONE_MSG);
                MainActivityPresenter.addMsg(true, Codes.CONNECTION_DONE_MSG);
                urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                reader = new BufferedReader(new InputStreamReader(in));
                sb = new StringBuilder();
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    finalResultStr = sb.toString();
                    AppLogger.writeLog(Codes.DONWLOAD_FILE_FINISH_CODE, Codes.DONWLOAD_FILE_FINISH_MSG);
                } catch (IOException ioe) {
                    AppLogger.writeLogEx(ioe);
                }
            } catch (MalformedURLException mue) {
                AppLogger.writeLogEx(mue);
            } catch (IOException ioe) {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            } catch (SecurityException se) {
                AppLogger.writeLogEx(se);
            }
            if (!finalResultStr.isEmpty()) {
                parseAndSetProperies(finalResultStr);
            } else {
                AppLogger.writeLog(Codes.NO_DATA_CODE, Codes.NO_DATA_MSG);
                MainActivityPresenter.addMsg(true, Codes.NO_DATA_MSG);
            }
            removeTaskAndGoToIdle(currTask.get());
        }
    }

    ArrayList<String> notSetValues = new ArrayList<>();

    private void parseAndSetProperies(String finalResultStr) {
        String version = "";
        String assitantVersion = "";
        String updateInterval = "";
        String unloadDataFileInt = "";
        String writeInterval = "";
        String showRebootMsg = "";
        String unloadServerAddress = "";
        String password = "";
        String uploadLogInt = "";
        //exchange
        String exchangeInt = "";
        //telemetry settings
        try {
            JSONObject jsonObject = new JSONObject(finalResultStr);
            String VERSION_STR = "version";
            version = jsonObject.getString(VERSION_STR);
            String ASSISTANT_VERSION_STR = "assistant_version";
            assitantVersion = jsonObject.getString(ASSISTANT_VERSION_STR);
            String UPDATE_STR = "ch_update_int";
            updateInterval = jsonObject.getString(UPDATE_STR);
            String UNLOAD_STR = "unload_int";
            unloadDataFileInt = jsonObject.getString(UNLOAD_STR);
            String WR_INT_STR = "write_int";
            writeInterval = jsonObject.getString(WR_INT_STR);
            String SHOW_REBOOT_MSG = "show_reboot_msg";
            showRebootMsg = jsonObject.getString(SHOW_REBOOT_MSG);
            String UPLOAD_SERVER = "upload_server";
            unloadServerAddress = jsonObject.getString(UPLOAD_SERVER);
            String PASSWORD = "password_admin_menu";
            password = jsonObject.getString(PASSWORD);
            String UP_LOGS = "unload_logs";
            uploadLogInt = jsonObject.getString(UP_LOGS);

            //exchange
            String EX_TIME = "exchange_time";
            exchangeInt = jsonObject.getString(EX_TIME);

            //telemetry_settings
            String TELEMETRY_SET_OBJ = "telemetry_settings";

            JSONObject tel_settings = jsonObject.getJSONObject(TELEMETRY_SET_OBJ);

            setTelemetrySetting(AIR_MODE, tel_settings.getString(AIR_MODE));
            setTelemetrySetting(TIME_CH, tel_settings.getString(TIME_CH));
            setTelemetrySetting(FAKE_COORD, tel_settings.getString(FAKE_COORD));
            setTelemetrySetting(FAKE_COORD, tel_settings.getString(FAKE_COORD));
            setTelemetrySetting(COORD, tel_settings.getString(COORD));
            setTelemetrySetting(SATELLITES, tel_settings.getString(SATELLITES));
            setTelemetrySetting(GPS_ERROR, tel_settings.getString(GPS_ERROR));
            setTelemetrySetting(CHARGING, tel_settings.getString(CHARGING));
            setTelemetrySetting(BATTERY, tel_settings.getString(BATTERY));
            setTelemetrySetting(MEMORY, tel_settings.getString(MEMORY));
            setTelemetrySetting(APPS, tel_settings.getString(APPS));
            setTelemetrySetting(DENIED_PERMS, tel_settings.getString(DENIED_PERMS));
            setTelemetrySetting(NETWORK_STATES, tel_settings.getString(NETWORK_STATES));
            DataStorage.neadDownloadSettings.set(false);
        } catch (JSONException e) {
            AppLogger.writeLogEx(e);
            e.printStackTrace();
            return;
        }
        setValue(ValueType.APP_AVAILABLE_VERSION, version);
        setValue(ValueType.APP_ASSISTANT_AVAILABLE_VERSION, assitantVersion);
        /*if (needUpdate(version)) {
            taskQueue.get().addFirst(TaskType.getAppNewVer);
            mNewAppVersion = version;
        } else {
            removeLastUpdateFiles();
        }*/
        if (BuildConfig.DEBUG) {
            unloadDataFileInt = "2 m";
            updateInterval = "3 m";
        }

        if (setValue(ValueType.UPLOAD_SERVER_ADDRESS, unloadServerAddress) && setValue(ValueType.ADMIN_PASS, password) && setValue(ValueType.WRITE_INT, writeInterval)
                && setValue(ValueType.UNLOAD_DATA_FILE_INT, unloadDataFileInt) && setValue(ValueType.UPDATE_INT, updateInterval) && setValue(ValueType.SHOW_REBOOT_MSG_TIME, showRebootMsg)
                && setValue(ValueType.UNLOAD_LOG_INT, uploadLogInt) && setValue(ValueType.EXCHANGE_INT, exchangeInt)) {
            AppLogger.writeLog(VALUES_SET_CODE, VALUES_SET);
            MainActivityPresenter.addMsg(true, VALUES_SET);
            DataStorage.setLastUpdateTime(System.currentTimeMillis());
        } else {
            AppLogger.writeLog(VALUES_SET_CODE, VALUES_NOT_SET);
            MainActivityPresenter.addMsg(true, VALUES_NOT_SET);
        }
        notSetValues.clear();
    }

    public void removeLastUpdateFiles() {
        File[] files = App.getContext().getFilesDir().listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                boolean deleted;
                String fName = f.getName();
                if (fName.contains(DataStorage.APP_NAME)) {
                    deleted = f.delete();
                    File[] files1 = App.getContext().getFilesDir().listFiles();
                    int l = files1.length;
                }
            }
        }
    }

    public boolean updateFileExist() {
        File installApk = new File(App.getContext().getFilesDir() + "/" + DataStorage.getUpdateFileName());
        return installApk.exists();
    }

    private boolean setTelemetrySetting(String setting, String value) {
        try {
            switch (setting) {
                case AIR_MODE:
                    DataStorage.setAirModeCheckingSettings(strToBool(value));
                    return true;
                case TIME_CH:
                    DataStorage.setTimeChangeCheckingSettings(strToBool(value));
                    return true;
                case FAKE_COORD:
                    DataStorage.setFakeCoordCheckingSettings(strToBool(value));
                    return true;
                case COORD:
                    DataStorage.setCoordCheckingSettings(strToBool(value));
                    return true;
                case SATELLITES:
                    DataStorage.setSatellitesCheckingSettings(strToBool(value));
                    return true;
                case GPS_ERROR:
                    DataStorage.setGpsErrorCheckingSettings(strToBool(value));
                    return true;
                case CHARGING:
                    DataStorage.setChargeCheckingSettings(strToBool(value));
                    return true;
                case BATTERY:
                    DataStorage.setBatteryCheckingSettings(strToBool(value));
                    return true;
                case MEMORY:
                    DataStorage.setMemoryCheckingSettings(strToBool(value));
                    return true;
                case APPS:
                    DataStorage.setAppsCheckingSettings(strToBool(value));
                    return true;
                case DENIED_PERMS:
                    DataStorage.setPermissionsCheckingSettings(strToBool(value));
                    return true;
                case NETWORK_STATES:
                    DataStorage.setNetworkStateCheckingSettings(strToBool(value));
                    return true;
                default:
                    return false;
            }
        } catch (IllegalAccessException e) {
            AppLogger.writeLogEx(e);
            notSetValues.add(setting);
        }
        return false;
    }


    private void removeTaskAndGoToIdle(TaskType taskType) {
        currTask.set(TaskType.Idle);
        taskQueue.get().remove(taskType);
        mState.set(States.IDLE);
        if (taskQueue.get().isEmpty()) {
            return;
        } else {
            mainHandler.post(() -> {
                completeTask(taskQueue.get().getFirst());
            });
        }
    }

    private void delayRemoveTaskAndGoToIdle(TaskType taskType) {
        App.getBgHandler().postDelayed(() -> {
            removeTaskAndGoToIdle(taskType);
        }, REMOVED_TASK_TIME_OUT);
    }

    @SuppressLint("SetWorldReadable")
    private void downloadFile(final String remoteFileName) {
        synchronized (lock) {
            mState.set(States.BUSY);
            MainActivityPresenter.addMsg(true, "Start download file " + remoteFileName);
            boolean result = false;
            byte[] buffer = new byte[8192];
            File lFileName = null;
            if (remoteFileName.contains(DataStorage.TP_ASSISTANT)) {
                lFileName = DataStorage.getAssistantUpdateFile();
            } else if (remoteFileName.contains(DataStorage.APP_NAME)) {
                lFileName = DataStorage.getAppUpdateFile();
            }
            String urlStr = SERVER_UPDATE_ADRESS + remoteFileName;
            try {
                lFileName.createNewFile();
            } catch (IOException e) {
                AppLogger.writeLog(IOEXCEPTION_CODE, IOEXCEPTION_MSG);
                AppLogger.writeLogEx(e);
                delayRemoveTaskAndGoToIdle(currTask.get());
                return;
            }
            URL url;
            InputStream in = null;
            OutputStream out = null;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                AppLogger.writeLog(MALFORMED_URL_EXCEPTION_CODE, MALFORMED_URL_EXCEPTION_MSG);
                AppLogger.writeLogEx(e);
                lFileName.delete();
                delayRemoveTaskAndGoToIdle(currTask.get());
                return;
            }
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                AppLogger.writeLog(IOEXCEPTION_CODE, IOEXCEPTION_MSG);
                AppLogger.writeLogEx(e);
                lFileName.delete();
                delayRemoveTaskAndGoToIdle(currTask.get());
                return;
            }
            urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
            try {
                in = new BufferedInputStream(urlConnection.getInputStream());
            } catch (IOException e) {
                AppLogger.writeLog(IOEXCEPTION_CODE, IOEXCEPTION_MSG);
                AppLogger.writeLogEx(e);
                lFileName.delete();
                delayRemoveTaskAndGoToIdle(currTask.get());
                return;
            }
            try {
                out = new BufferedOutputStream(new FileOutputStream(lFileName));
            } catch (FileNotFoundException e) {
                lFileName.delete();
                AppLogger.writeLogEx(e);
                delayRemoveTaskAndGoToIdle(currTask.get());
                return;
            }

            try {
                int len;
                while ((len = in.read(buffer)) != -1) {
                    AppLogger.i(TAG, "Download " + len + " bytes");
                    MainActivityPresenter.addMsg(false, ".");
                    out.write(buffer, 0, len);
                }
                out.close();
            } catch (IOException e) {
                AppLogger.writeLog(IOEXCEPTION_CODE, IOEXCEPTION_MSG);
                AppLogger.writeLogEx(e);
                lFileName.delete();
                delayRemoveTaskAndGoToIdle(currTask.get());
                return;

            }
            MainActivityPresenter.addMsg(true, "Done.");
            result = lFileName.setReadable(true, false);
            if (result) {
                if (remoteFileName.contains(DataStorage.TP_ASSISTANT)) {
                    App.getMainHandler().post(() -> {
                        GuiHelper.startDialogActivity(ServerTaskListener.UPDATE_ASSISTANT);
                    });
                } else if (remoteFileName.contains(DataStorage.APP_NAME)) {
                    App.getMainHandler().post(() -> {
                        GuiHelper.startDialogActivity(ServerTaskListener.UPDATE_APP);
                    });
                }
                removeTaskAndGoToIdle(currTask.get());
            } else {
                lFileName.delete();
                delayRemoveTaskAndGoToIdle(currTask.get());
            }
        }
    }

    private boolean strToBool(String value) throws IllegalAccessException {
        if (value.equals(ON) || value.equals(OFF)) {
            return value.equals(ON);
        } else {
            throw new IllegalAccessException("Can't set value " + value);
        }
    }

    private enum ValueType {APP_AVAILABLE_VERSION, APP_ASSISTANT_AVAILABLE_VERSION, WRITE_INT, UNLOAD_DATA_FILE_INT, UPDATE_INT, SHOW_REBOOT_MSG_TIME, UPLOAD_SERVER_ADDRESS, ADMIN_PASS, UNLOAD_LOG_INT, EXCHANGE_INT}

    private boolean setValue(ValueType type, String value) {
        switch (type) {
            case UPLOAD_SERVER_ADDRESS:
                return DataStorage.setUploadServerAddress(value);
            case ADMIN_PASS:
                return DataStorage.setAdminPassword(value);
            case APP_AVAILABLE_VERSION:
                DataStorage.setAppAvailableVersion(value);
                break;
            case APP_ASSISTANT_AVAILABLE_VERSION:
                DataStorage.setAssistantAvailableVersion(value);
                break;
            case WRITE_INT:
            case UNLOAD_DATA_FILE_INT:
            case UPDATE_INT:
            case SHOW_REBOOT_MSG_TIME:
            case UNLOAD_LOG_INT:
            case EXCHANGE_INT:
                return setTimeValue(type, value);
        }
        return false;
    }

    private long convertPropValueToLong(String value) {
        long newInterval = -1;
        String regex = "\\d+";
        if (value.matches(regex)) {
            return Long.parseLong(value);
        }
        try {
            long timeValue = Long.parseLong((value.substring(0, value.indexOf(' '))));
            String timeUnitStr = value.substring(value.lastIndexOf(' ') + 1);
            newInterval = convertToCalendarValue(timeUnitStr, timeValue);
        } catch (StringIndexOutOfBoundsException e) {
            AppLogger.printStackTrace(e);
            AppLogger.writeLogEx(e);
            MainActivityPresenter.addMsg(true, "Exception when trying to read data from a database");
        }
        return newInterval;
    }

    private boolean setTimeValue(ValueType type, String value) {
        if (value.isEmpty()) {
            return false;
        } else {
            long newInterval = convertPropValueToLong(value);
            switch (type) {
                case WRITE_INT:
                    DataStorage.setWriteInterval(newInterval);
                    return true;
                case UNLOAD_DATA_FILE_INT:
                    DataStorage.setUnloadDataFileInt(newInterval);
                    return true;
                case UPDATE_INT:
                    DataStorage.setUpdateInterval(newInterval);
                    return true;
                case SHOW_REBOOT_MSG_TIME:
                    DataStorage.setShowRebootMsgInTime(newInterval);
                    return true;
                case UNLOAD_LOG_INT:
                    DataStorage.setUnloadLogsInterval(newInterval);
                    return true;
                case EXCHANGE_INT:
                    DataStorage.setExchangeTime(newInterval);
                    return true;
            }
        }
        return false;
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

        @Override
        public void onAvailable(Network network) {
            Toast.makeText(App.getContext(), "ConnectionStateMonitor", Toast.LENGTH_SHORT).show();
        }
    }

    public static class ConnectionChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo == null || !activeNetInfo.isConnected()/*|| !activeNetInfo.isAvailable()*/) {
                DataStorage.networkAvailable.set(false);
                DataStorage.activeNetInfoStr.set("");
                refreshIp();
                return;
            }
            if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                DataStorage.activeNetInfoStr.set("WIFI");
                setNetworkAvailable();
                refreshIp();
                return;
            }

            int netSubtype = activeNetInfo.getSubtype();
            switch (netSubtype) {
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_GSM:
                    DataStorage.activeNetInfoStr.set("2G");
                    setNetworkAvailable();
                    refreshIp();
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
                    DataStorage.activeNetInfoStr.set("3G");
                    setNetworkAvailable();
                    refreshIp();
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                    DataStorage.activeNetInfoStr.set("4G");
                    setNetworkAvailable();
                    refreshIp();
                    break;
                case TelephonyManager.NETWORK_TYPE_NR:
                    DataStorage.activeNetInfoStr.set("5G");
                    setNetworkAvailable();
                    refreshIp();
                    break;
                default:
                    DataStorage.activeNetInfoStr.set("");
                    DataStorage.networkAvailable.set(false);
                    break;
            }
        }

        private void setNetworkAvailable() {
            DataStorage.networkAvailable.set(true);
            RemoteServerHelper.getINSTANCE().completeFirstTask();
        }

        private void refreshIp() {
            App.getBgHandler().post(() -> {
                if (!DataStorage.networkAvailable.get()) {
                    DataStorage.ip.set("");
                    AppLogger.e("IP", "IP = " + DataStorage.ip);
                    MainActivityPresenter.refreshTitle();
                    return;
                }
               /* boolean ipreceived = false;
                try {
                    ipreceived = getIpFromIpInfo();
                }catch (Exception e){}*/
                /*if (!ipreceived) {*/
                for (String url : IP_SERVICE_URLS) {
                    if (getIpFromService(url)) {
                        MainActivityPresenter.refreshTitle();
                        return;
                    }
                }
                MainActivityPresenter.refreshTitle();

            });
        }

        private boolean getIpFromService(final String url) {
            String responce = "";
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                responce = response.body().string();
                JSONObject obj = new JSONObject(responce);
                String ip = obj.getString("ip");
                if (!ip.isEmpty()) {
                    DataStorage.ip.set(ip);
                    AppLogger.e("IP", "IP = " + DataStorage.ip);
                }
                return true;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean getIpFromIpInfo() throws ClassNotFoundException {
            try {
                IPInfo ipInfo = IPInfo.builder().setToken(DataStorage.getIpInfoToken()).build();
                IPResponse response = ipInfo.lookupIP("");
                DataStorage.ip.set(response.getIp());
                AppLogger.e("IP", "IP = " + DataStorage.ip);
                return true;
            } catch (RateLimitedException ex) {
            }
            return false;
        }
    }
}
