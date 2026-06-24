package com.tactical.app;

import java.time.LocalDateTime;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "docman_files")
public class Document {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String fileName;
    public String category;
    public LocalDateTime uploadedAt;
    public String diskLocation;

    public Document(String fileName, String category) {
        this.fileName = fileName;
        this.category = category;
        this.uploadedAt = LocalDateTime.now();
    }

    public Document setDiskLocation(String diskLocation){
        this.diskLocation=diskLocation;
        return this;
    }
}