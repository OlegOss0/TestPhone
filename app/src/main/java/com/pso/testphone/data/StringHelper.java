package com.pso.testphone.data;

import com.pso.testphone.db.DBHelper;

public class StringHelper {
    public static String addHeader(String dataStr) {
        String headerStr = DBHelper.TelemetryFileHeader + ",Device info: " + DeviceInfo.getFull() + '\n';
        return headerStr.concat(dataStr);
    }
}
