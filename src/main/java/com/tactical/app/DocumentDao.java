package com.tactical.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DocumentDao {

    @Insert
    void insertLog(Document file);

    @Query("SELECT * FROM docman_files ORDER BY id DESC")
    List<Document> getAll();

    @Query("DELETE FROM docman_files")
    void deleteAll();
}