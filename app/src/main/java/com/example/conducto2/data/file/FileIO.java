package com.example.conducto2.data.file;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileIO {
    /**
     * This class's purpose is to unzips the .mxl file
     * and finds the first valid .xml entry inside the zip.
     */
    Context context;

    public FileIO(Context context) {
        this.context = context;
    }

    private InputStream openInputStream(Uri uri) throws Exception {
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return new URL(uri.toString()).openStream();
        }
        return context.getContentResolver().openInputStream(uri);
    }

    public String readZippedXMLFromUri(Uri uri) throws Exception {
        try (InputStream inputStream = openInputStream(uri);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();

                // We ignore the container metadata file and look for the actual music sheet
                if (!name.contains("META-INF") && (name.endsWith(".xml") || name.endsWith(".musicxml"))) {

                    // Found the xml file inside the ZIP! Read it.
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    return stringBuilder.toString();
                }
            }
        }
        throw new Exception("No valid MusicXML file found inside .mxl package");
    }

    public String readTextFromUri(Uri uri) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    public String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    /**
     * Extracts the file extension from a Uri.
     * @param uri The URI of the file.
     * @return The extension (e.g., "xml", "mxl") or an empty string if not found.
     */
    public String getExtension(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}
