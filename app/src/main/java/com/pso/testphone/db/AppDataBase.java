package com.pso.testphone.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;


@Database(entities = {TimePoint.class, Provider.class}, version = 4)
public abstract class AppDataBase extends RoomDatabase {
    public abstract TimePointDao timePointDao();
    public abstract ProviderDao providerDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE TimePoint ADD COLUMN satellite TEXT NOT NULL DEFAULT ' '");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE TimePoint ADD COLUMN appVersion TEXT NOT NULL DEFAULT ' '");
        }
    };
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE TimePoint ADD COLUMN activeNetwork TEXT NOT NULL DEFAULT ' '");
        }
    };
}
