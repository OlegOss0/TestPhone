package com.pso.testphone.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MyLog {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public long time;

    @NonNull
    public String code;

    @NonNull
    public String msg;

    public MyLog(@NonNull long time, @NonNull String code, @NonNull String msg){
        this.time = time;
        this.code = code;
        this.msg = msg;
    }
}
