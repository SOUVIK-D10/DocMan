package com.tactical.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity; // CRITICAL: Required for Material themes
import androidx.room.Room;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,
        // "tactical-db").build();
        // databaseExecutor = Executors.newSingleThreadExecutor();
        FloatingActionButton btnAddDoc = findViewById(R.id.btnAdd);
        btnAddDoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Formulate the transition order: FROM MainActivity TO UploadActivity
                Intent intent = new Intent(MainActivity.this, UploadActivity.class);
                // Execute the transition
                startActivity(intent);
            }
        });
    }
}