package com.pso.testphone;

import android.util.Log;

import com.pso.testphone.data.Codes;
import com.pso.testphone.data.DataStorage;
import com.pso.testphone.db.MyLog;
import com.pso.testphone.gui.MainActivityPresenter;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class AppLogger {

    public static void e(String TAG, String msg) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, msg);
        }
    }

    public static void e(String TAG, String msg, Exception ex) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, msg, ex);
        }
    }

    public static void i(String TAG, String msg) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, msg);
        }
    }

    public static void witnAdditionalMsg(String msg) {
        if (BuildConfig.DEBUG || DataStorage.isAdditionalLoggingEnabled()) {
            MainActivityPresenter.addMsg(true, msg);
        }
    }

    public static void printStackTrace(Exception e) {
        if (BuildConfig.DEBUG || DataStorage.isAdditionalLoggingEnabled()) {
            StackTraceElement[] stackTraceElements = e.getStackTrace();
            MainActivityPresenter.addMsg(true, e.getMessage());
            for (int i = 0; i < stackTraceElements.length; i++) {
                MainActivityPresenter.addMsg(true, "Class " + stackTraceElements[i].getClassName() + ", Method " + stackTraceElements[i].getMethodName() + "," + stackTraceElements[i].getLineNumber());
            }
        }
        e.printStackTrace();
    }

    private static String getStackTraceStr(Exception e){
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        for(StackTraceElement element : stackTraceElements){
            sb.append("Class-" + element.getClassName() + " Method-" + element.getMethodName() + element.getLineNumber());
        }
        return sb.toString();
    }

    public static void writeLog(final String code, final String msg){
        App.getDataBase().myLogDao().insert(new MyLog(System.currentTimeMillis(), code, msg));
    }

    public static void writeNetworkInfoLog(){
        AppLogger.writeLog(Codes.NETWORK_INFO_CODE, "Active network = " + DataStorage.activeNetInfoStr.get() + " Ip = "
                + DataStorage.ip.get() + ".");
    }

    public static void writeLogEx(Exception e) {
        if(e instanceof SocketTimeoutException){
            AppLogger.writeLog(Codes.SOCKET_TIMEOUT_EXCEPTION_CODE, Codes.SOCKET_TIMEOUT_EXCEPTION_MSG + " " + getStackTraceStr(e));
            return;
        }else if(e instanceof UnknownHostException) {
            AppLogger.writeLog(Codes.UNCNOWN_HOST_EXCEPTION_CODE, Codes.UNCNOWN_HOST_EXCEPTION_MSG + " " + getStackTraceStr(e));
            return;
        }else if(e instanceof UnsupportedEncodingException){
            AppLogger.writeLog(Codes.UNSUPPORTED_ENCODING_EXCEPTION_CODE, Codes.UNSUPPORTED_ENCODING_EXCEPTION_MSG + " " + getStackTraceStr(e));
            return;
        }else if(e instanceof JSONException){
            AppLogger.writeLog(Codes.JSONEXCEPTION_EXCEPTION_CODE, Codes.JSONEXCEPTION_EXCEPTION_MSG + " " + getStackTraceStr(e));
            return;
        }else if(e instanceof MalformedURLException){
            AppLogger.writeLog(Codes.MALFORMED_URL_EXCEPTION_CODE, Codes.MALFORMED_URL_EXCEPTION_MSG + " " + getStackTraceStr(e));
            return;
        }else if(e instanceof StringIndexOutOfBoundsException){
            AppLogger.writeLog(Codes.STRING_IND_OUT_OF_BOUND_EXCEPTION_CODE, Codes.STRING_IND_OUT_OF_BOUND_EXCEPTION_MSG + " " + getStackTraceStr(e));
            return;
        }

        AppLogger.writeLog(Codes.IOEXCEPTION_CODE, Codes.IOEXCEPTION_MSG + " " + getStackTraceStr(e));
    }
}
