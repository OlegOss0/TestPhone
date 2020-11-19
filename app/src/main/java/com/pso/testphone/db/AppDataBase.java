package com.pso.testphone.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;


@Database(entities = {TimePoint.class, Provider.class, MyLog.class}, version = 5)
public abstract class AppDataBase extends RoomDatabase {
    public abstract TimePointDao timePointDao();
    public abstract ProviderDao providerDao();
    public abstract MyLogDao myLogDao();

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

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE TimePoint ADD COLUMN noPermissions TEXT NOT NULL DEFAULT ' '");
            database.execSQL("CREATE TABLE MyLog (" +
                    "id INTEGER PRIMARY KEY NOT NULL," +
                    "time INTEGER NOT NULL," +
                    "code TEXT NOT NULL DEFAULT ''," +
                    "msg TEXT NOT NULL DEFAULT '')");
        }
    };
}
