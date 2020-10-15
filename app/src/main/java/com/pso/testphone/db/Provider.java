package com.pso.testphone.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;


@Entity(foreignKeys = @ForeignKey(entity = TimePoint.class, parentColumns = "time", childColumns = "ownerTime", onDelete = CASCADE))
public class Provider {


    public Provider(){
    }

    public Provider(String name, long time, double lat, double longi, double accurate, int isBest) {
        this.name = name;
        this.time = time;
        this.latitude = lat;
        this.longitude = longi;
        this.accurate = accurate;
        this.isBest = isBest;
    }

    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int id;

    public long ownerTime;

    public String name;

    public long time;

    public double latitude;

    public double longitude;

    public double accurate;

    public int isBest;
}
