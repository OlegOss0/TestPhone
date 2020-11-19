package com.pso.testphone.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MyLogDao {
    @Query("SELECT * FROM MyLog")
    List<MyLog> getAll();

    @Insert
    void insert(MyLog myLog);

    @Update
    void update(MyLog myLog);

    @Delete
    void delete(MyLog myLog);
}
