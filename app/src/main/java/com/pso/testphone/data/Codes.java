package com.pso.testphone.data;

import com.pso.testphone.AppLogger;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

public class Codes {
    //LifeCycles
    public static final String ON_CREATE_CODE = "001";
    public static final String ON_START_CODE = "002";
    public static final String ON_LOW_MEMORY_CODE = "003";
    public static final String ON_TRIM_MEMORY_CODE = "004";
    public static final String ON_TASK_REMOVED_CODE = "005";
    public static final String ON_DESTROY_CODE = "006";

    public static final String ON_CREATE_MSG = "Create";
    public static final String ON_START_MSG = "Start";
    public static final String ON_LOW_MEMORY_MSG = "Low memory";
    public static final String ON_TRIM_MEMORY_MSG= "Trim memory ";
    public static final String ON_TASK_REMOVED_MSG = "Task removed";
    public static final String ON_DESTROY_MSG = "Destroy";

    //networks
    public static final String NETWORK_NOT_AVAILABLE_CODE = "010";
    public static final String NETWORK_NOT_AVAILABLE_MSG = "The network is not available now";
    public static final String START_CONNECTION_CODE = "011";
    public static final String START_CONNECTION_MSG = "Start connection to";
    public static final String NETWORK_INFO_CODE = "012";
    public static final String CONNECTION_DONE_CODE = "013";
    public static final String CONNECTION_DONE_MSG = "Connection done";
    public static final String CONNECTION_FAILED_CODE = "014";
    public static final String CONNECTION_FAILED_MSG = "Connection failed";
    public static final String START_UNLOAD_FILE_CODE = "015";
    public static final String START_UNLOAD_FILE_MSG = "Start unload file";
    public static final String UNLOAD_FILE_FINISH_CODE = "016";
    public static final String UNLOAD_FILE_FINISH_MSG = "Unload file finish";
    public static final String DONWLOAD_FILE_FINISH_CODE = "017";
    public static final String DONWLOAD_FILE_FINISH_MSG = "Download file finish";
    public static final String DISCONNECT_CODE = "018";
    public static final String DISCONNECT_MSG = "Disconnect from";
    public static final String NO_DATA_CODE = "019";
    public static final String NO_DATA_MSG = "No data";
    public static final String VALUES_SET_CODE = "020";
    public static final String VALUES_SET = "New values set";
    public static final String VALUES_NOT_SET = "New values not set";

    //exeptions 040 >
    public static final String IOEXCEPTION_CODE = "040";
    public static final String IOEXCEPTION_MSG = "IOException";
    public static final String SOCKET_TIMEOUT_EXCEPTION_CODE = "041";
    public static final String SOCKET_TIMEOUT_EXCEPTION_MSG = "SocketTimeoutException";
    public static final String UNCNOWN_HOST_EXCEPTION_CODE = "042";
    public static final String UNCNOWN_HOST_EXCEPTION_MSG = "UnknownHostException";
    public static final String UNSUPPORTED_ENCODING_EXCEPTION_CODE = "043";
    public static final String UNSUPPORTED_ENCODING_EXCEPTION_MSG = "UnsupportedEncodingException";
    public static final String JSONEXCEPTION_EXCEPTION_CODE = "044";
    public static final String JSONEXCEPTION_EXCEPTION_MSG = "JSONException";
    public static final String MALFORMED_URL_EXCEPTION_CODE = "045";
    public static final String MALFORMED_URL_EXCEPTION_MSG = "MalformedURLException";
    public static final String STRING_IND_OUT_OF_BOUND_EXCEPTION_CODE = "046";
    public static final String STRING_IND_OUT_OF_BOUND_EXCEPTION_MSG= "StringIndexOutOfBoundsException";

    public static final String TASK_CODE = "050";
    public static final String TASK_DELAY_MSG = "Task was delayed ";
    public static final String TASK_EXIST_MSG = "Task is already waiting to be completed ";

    public static final String BTN_PRESSED = "060";
    public static final String SEND_DATA_FILE_SEND_PRESSED_MSG = "Send data file button pressed";
    public static final String SEND_LOG_FILE_SEND_PRESSED_MSG = "Send log file button pressed";
    public static final String CLOSED_PRESSED_MSG = "Closed button pressed";
    public static final String CLEAR_DB_PRESSED_MSG = "Clear database button pressed";
    public static final String CHECK_UPDATE_PRESSED_MSG = "Check update button pressed";










}
