package com.pso.testphone.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TimePointDao {

        @Query("SELECT * FROM timepoint")
        List<TimePoint> getAll();

        @Query("SELECT * FROM timepoint LIMIT :count")
        List<TimePoint> get(int count);

        @Query("SELECT COUNT(*) FROM timepoint")
        Integer getCounts();

        @Insert
        void insert(TimePoint timePoint);

        @Update
        void update(TimePoint timePoint);

        @Delete
        void delete(TimePoint timePoint);

}
