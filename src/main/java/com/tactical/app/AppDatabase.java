package com.tactical.app;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// Declare the entities (tables) inside this database and set the version
@Database(entities = {Document.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    // Provide an abstract method to access the DAO
    public abstract DocumentDao documentDao();
    
}