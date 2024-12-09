package com.example.lab8;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Note.class}, version = 1, exportSchema = false)
public abstract class NotesDatabase extends RoomDatabase {

    private static volatile NotesDatabase instance; // Для синглтона

    public abstract NotesDao notesDao();

    // Метод для получения инстанса базы данных
    public static NotesDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (NotesDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            NotesDatabase.class,
                            "notes_database"
                    ).build();
                }
            }
        }
        return instance;
    }
}
