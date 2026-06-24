package com.tactical.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadActivity extends AppCompatActivity {

    private TextInputEditText etFileName;
    private AutoCompleteTextView spinnerCategory;
    private TextView tvFilePathStatus;
    
    private Uri securedTargetUri = null;
    
    private AppDatabase db;
    private DocumentService service;
    private ExecutorService backgroundExecutor;

    // INGESTION LAUNCHER
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null && data.getClipData().getItemCount() > 1) {
                            updateUIStatus("ERROR: Multiple payloads detected.", false);
                            securedTargetUri = null;
                        } else {
                            securedTargetUri = data.getData();
                            updateUIStatus("Status: Target Acquired\n" + securedTargetUri.getLastPathSegment(), true);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Initialize Architecture
        backgroundExecutor = Executors.newSingleThreadExecutor();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "tactical-db").build();
        service = new DocumentService(this, db); // Pass context and DB to the service layer

        // Bind UI
        etFileName = findViewById(R.id.etFileName);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        tvFilePathStatus = findViewById(R.id.tvFilePathStatus);
        MaterialButton btnSelectFile = findViewById(R.id.btnSelectFile);
        MaterialButton btnSubmitUpload = findViewById(R.id.btnSubmitUpload);

        // Setup Dropdown
        String[] categories = new String[]{"Intel Report", "Map/Blueprint", "Field Manual"};
        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories));

        // Locate Payload Button
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                
                // 1. Broaden the initial type
                intent.setType("*/*"); 
                
                // 2. Define the exact authorized payload types
                String[] validMimeTypes = {"application/pdf", "image/jpeg", "image/png"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, validMimeTypes);
                
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                filePickerLauncher.launch(intent);
            }
        });

        // Submit Button
        btnSubmitUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = etFileName.getText().toString();
                final String category = spinnerCategory.getText().toString();

                updateUIStatus("Processing Ingestion...", true);

                // Command the Service on a background thread
                backgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String path = service.processAndStoreDocument(securedTargetUri, name, category);
                            
                            // Report Success to Main Thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUIStatus("SUCCESS: Vault Secured\n" + path, true);
                                    securedTargetUri = null;
                                    etFileName.setText("");
                                }
                            });
                        } catch (final GeneralException e) {
                            // Report Failure to Main Thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUIStatus("CRITICAL FAILURE: " + e.getMessage(), false);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    // Helper method to keep UI updates clean
    private void updateUIStatus(String message, boolean isSuccess) {
        tvFilePathStatus.setText(message);
        tvFilePathStatus.setTextColor(getResources().getColor(
                isSuccess ? android.R.color.holo_green_light : android.R.color.holo_red_light
        ));
    }
}