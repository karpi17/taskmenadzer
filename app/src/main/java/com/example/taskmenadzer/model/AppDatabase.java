package com.example.taskmenadzer.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {TaskEntity.class}, version = 4, exportSchema = false) // <<<< ZMIANA WERSJI NA 4
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();

    private static volatile AppDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Istniejąca migracja z wersji 1 do 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN notified_near_deadline INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0");
            // Kolumna is_done już prawdopodobnie istniała lub była dodawana inaczej,
            // jeśli nie, to ten SQL jest poprawny.
            // Sprawdź swoją oryginalną strukturę tabeli dla wersji 1.
            database.execSQL("ALTER TABLE tasks ADD COLUMN is_done INTEGER NOT NULL DEFAULT 0");
            Log.i("AppDatabase", "Migracja bazy danych z wersji 1 do 2 zakończona.");
        }
    };

    // Istniejąca migracja z wersji 2 do 3
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN notified_individually_near_deadline INTEGER NOT NULL DEFAULT 0");
            Log.i("AppDatabase", "Migracja bazy danych z wersji 2 do 3 zakończona (dodano notified_individually_near_deadline).");
        }
    };

    // NOWA MIGRACJA z wersji 3 do 4
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Dodajemy nową kolumnę dla znacznika czasu archiwizacji.
            // INTEGER w SQLite może przechowywać typ Long z Javy (timestamp).
            // Domyślnie będzie NULL, co jest w porządku.
            database.execSQL("ALTER TABLE tasks ADD COLUMN archived_at_timestamp INTEGER");
            Log.i("AppDatabase", "Migracja bazy danych z wersji 3 do 4 zakończona (dodano archived_at_timestamp).");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "tasks-db")
                            // Dodaj wszystkie migracje w odpowiedniej kolejności
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // <<<< DODANO MIGRATION_3_4
                            // .fallbackToDestructiveMigration() // Rozważ tylko podczas developmentu, jeśli nie chcesz pisać migracji
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}