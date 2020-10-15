package com.pso.testphone.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProviderDao {

    @Query("SELECT * FROM provider")
    List<Provider> getAll();

    @Query("SELECT * FROM provider WHERE isBest  = 'true'")
    Provider getBest();

    @Query("SELECT * FROM provider WHERE ownerTime IS :ownerTime")
    List<Provider> getProvidersByTime(long ownerTime);

    @Insert
    void insert(Provider provider);

    @Update
    void update(Provider provider);

    @Delete
    void delete(Provider provider);
}
