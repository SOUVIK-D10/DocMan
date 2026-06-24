package com.tactical.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.core.content.FileProvider;
import android.webkit.MimeTypeMap;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService ioExecutor;
    private DocumentAdapter adapter;

    // UI Elements
    private LinearLayout tacticalActionBar;
    private FloatingActionButton btnAdd;
    private Document lockedDocument = null;

    // EXPORT PROTOCOL: Opens native picker to "Save As"
    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> {
                if (uri != null && lockedDocument != null) {
                    executeExport(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Architecture Setup
        ioExecutor = Executors.newSingleThreadExecutor();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "tactical-db").build();

        // Bind UI
        tacticalActionBar = findViewById(R.id.tacticalActionBar);
        btnAdd = findViewById(R.id.btnAdd);
        ImageButton btnExport = findViewById(R.id.btnExport);
        ImageButton btnDelete = findViewById(R.id.btnDelete);
        RecyclerView rvDocuments = findViewById(R.id.rvDocuments);
        ImageButton btnShare = findViewById(R.id.btnShare);

        // Setup RecyclerView
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DocumentAdapter(new DocumentAdapter.TargetCommandListener() {
            @Override
            public void onTargetLocked(Document document) {
                lockedDocument = document;
                tacticalActionBar.setVisibility(View.VISIBLE);
                btnAdd.setVisibility(View.GONE);
            }

            @Override
            public void onTargetCleared() {
                lockedDocument = null;
                tacticalActionBar.setVisibility(View.GONE);
                btnAdd.setVisibility(View.VISIBLE);
            }

            @Override
            public void onTargetOpened(Document document) {
                executeViewProtocol(document); // <-- Routes to the new view function
            }
        });
        rvDocuments.setAdapter(adapter);

        // Actions
        btnAdd.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, UploadActivity.class)));

        btnShare.setOnClickListener(v -> triggerShareProtocol());

        btnExport.setOnClickListener(v -> {
            if (lockedDocument != null) {
                // Launch file picker with suggested name
                exportLauncher.launch(lockedDocument.fileName + "_export.pdf");
            }
        });

        // Click Delete -> Prompt Confirmation Dialog
        btnDelete.setOnClickListener(v -> triggerDeleteProtocol());
    }

    // Refresh data every time screen opens (handles returning from UploadActivity)
    @Override
    protected void onResume() {
        super.onResume();
        loadVaultIntelligence();
    }

    private void loadVaultIntelligence() {
        ioExecutor.execute(() -> {
            List<Document> docs = db.documentDao().getAll();
            runOnUiThread(() -> adapter.setDocuments(docs));
        });
    }

    private void executeExport(Uri targetUri) {
        ioExecutor.execute(() -> {
            try (InputStream in = new FileInputStream(new File(lockedDocument.diskLocation));
                    OutputStream out = getContentResolver().openOutputStream(targetUri)) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Extraction Successful", Toast.LENGTH_SHORT).show();
                    adapter.clearLock();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Export Failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private Uri getSecureUri(File file) {
        return FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
    }

    private String getMimeType(String url) {
        String type = "*/*";
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private void executeViewProtocol(Document document) {
        File file = new File(document.diskLocation);
        Uri secureUri = getSecureUri(file);
        String mimeType = getMimeType(file.getAbsolutePath());

        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(secureUri, mimeType);
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Critical security flag

        try {
            startActivity(viewIntent);
        } catch (Exception e) {
            Toast.makeText(this, "ERROR: No civilian app installed to view this format.", Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerShareProtocol() {
        if (lockedDocument == null)
            return;

        File file = new File(lockedDocument.diskLocation);
        Uri secureUri = getSecureUri(file);
        String mimeType = getMimeType(file.getAbsolutePath());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, secureUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Critical security flag

        startActivity(Intent.createChooser(shareIntent, "Transmit Intel Via:"));
    }

    private void triggerDeleteProtocol() {
        if (lockedDocument == null)
            return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("CONFIRM DELETION")
                .setMessage("Permanently erase " + lockedDocument.fileName + " from the Vault?")
                .setPositiveButton("EXECUTE", (dialog, which) -> {
                    // Delete File & DB Entry on background thread
                    ioExecutor.execute(() -> {
                        File file = new File(lockedDocument.diskLocation);
                        if (file.exists())
                            file.delete();
                        db.documentDao().delete(lockedDocument.id);

                        runOnUiThread(() -> {
                            adapter.clearLock();
                            loadVaultIntelligence(); // Refresh list
                        });
                    });
                })
                .setNegativeButton("ABORT", (dialog, which) -> adapter.clearLock())
                .show();
    }
}