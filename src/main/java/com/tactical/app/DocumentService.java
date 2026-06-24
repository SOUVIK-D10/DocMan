package com.tactical.app;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import android.webkit.MimeTypeMap;

public class DocumentService {

    private final Context context;
    private final AppDatabase db;
    private File vaultDirectory;

    // CONSTRUCTOR: Inject the Context and Database
    public DocumentService(Context context, AppDatabase db) {
        this.context = context;
        this.db = db;
        establishVaultDirectory();
    }

    private void establishVaultDirectory() {
        vaultDirectory = new File(context.getFilesDir(), "DocManVault");
        if (!vaultDirectory.exists()) {
            vaultDirectory.mkdirs();
        }
    }

    private long getFileSizeInBytes(Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        long size = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return size;
    }

    /**
     * THE MASTER INGESTION PROTOCOL
     * This must be called from a background thread by the Activity.
     */
    public String processAndStoreDocument(Uri targetUri, String fileName, String category) throws GeneralException {

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new GeneralException("Target name cannot be empty.");
        }
        if (targetUri == null) {
            throw new GeneralException("No payload selected.");
        }

        // 1. Check File Size (Max 10MB)
        long sizeInBytes = getFileSizeInBytes(targetUri);
        if (sizeInBytes > (10 * 1024 * 1024)) {
            throw new GeneralException("Payload exceeds 10MB limit.");
        }

        try {
            // 2. Log to Database FIRST to get the unique ID
            Document newDoc = new Document(fileName, category);
            long generatedId = db.documentDao().insertDocument(newDoc);

            String mimeType = context.getContentResolver().getType(targetUri);

            // 2. Convert the MIME type (e.g., "image/jpeg") into an extension (e.g., "jpg")
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

            // Fallback security just in case the system can't identify it
            if (extension == null) {
                extension = "pdf";
            }
            String finalFileName = fileName.replaceAll("\\s+", "_") + "_" + generatedId + "." + extension;
            File destinationFile = new File(vaultDirectory, finalFileName);

            // 4. Copy IO Streams
            try (InputStream inStream = context.getContentResolver().openInputStream(targetUri);
                    OutputStream outStream = new FileOutputStream(destinationFile)) {

                byte[] buffer = new byte[4096];
                int length;
                while ((length = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, length);
                }
            }

            // 5. Update Database with the physical location
            String absolutePath = destinationFile.getAbsolutePath();
            db.documentDao().setLocation(absolutePath, generatedId);

            return absolutePath; // Return success data to Activity

        } catch (Exception e) {
            throw new GeneralException("IO Failure: " + e.getMessage());
        }
    }
}