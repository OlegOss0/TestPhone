package com.pso.testphone.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TimePoint {
    @PrimaryKey
    public long time;

    public int airMode;

    public int timeChange;

    public int fictitious;

    public int gpsState;

    public int networkState;

    public int charging;

    public int battery;

    public long bootTime;

    public String memory;

    public String installApp;

    @NonNull
    public String satellite;  //add in version 2

    @NonNull
    public String appVersion;

    @NonNull
    public String activeNetwork; //add in version 2

    @NonNull
    public String noPermissions; //add in version 5

}
