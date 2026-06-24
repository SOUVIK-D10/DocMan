package com.tactical.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DocumentDao {

    // RETURN THE GENERATED ID
    @Insert
    long insertDocument(Document data);

    @Query("SELECT * FROM docman_files ORDER BY id DESC")
    List<Document> getAll();

    @Query("SELECT * FROM docman_files WHERE id = :id")
    Document getById(int id);

    @Query("DELETE FROM docman_files WHERE id = :id")
    void delete(int id);

    // UPDATE THE LOCATION USING THE ID
    @Query("UPDATE docman_files SET diskLocation = :location WHERE id = :id")
    void setLocation(String location, long id);
}