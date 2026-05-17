package com.example.conducto2.data.file;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * FileIO
 * 
 * A robust utility class responsible for the ingestion and processing of MusicXML data. 
 * It serves as a format-agnostic reader that supports:
 * 1. Plain text MusicXML (.xml, .musicxml).
 * 2. Compressed MusicXML (.mxl), which are ZIP-encoded archives.
 * 3. Local content providers (Gallery/Files) and remote HTTP/HTTPS URLs.
 * 
 * The class implements standard MusicXML container parsing logic (META-INF/container.xml) 
 * to correctly identify the root score file within compressed archives.
 */
public class FileIO {

    /** Identifier for logging file operation events. */
    private static final String TAG = "FileIO";

    /** Android Context used for interacting with the ContentResolver. */
    private final Context context;

    /**
     * Initializes the FileIO utility.
     * @param context The application or activity context.
     */
    public FileIO(Context context) {
        this.context = context;
    }

    /**
     * Resolves the appropriate InputStream for a given Uri. 
     * Supports both remote network streams and local Android content providers.
     * 
     * @param uri The resource location.
     * @return An open InputStream.
     * @throws Exception If the stream cannot be opened.
     */
    private InputStream openInputStream(Uri uri) throws Exception {
        String scheme = uri.getScheme();
        // Handle remote web-based MusicXML files.
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return new URL(uri.toString()).openStream();
        }
        // Handle local files via the system's ContentResolver.
        return context.getContentResolver().openInputStream(uri);
    }

    /**
     * High-level entry point to retrieve the string content of a MusicXML resource.
     * Automatically detects and handles compression.
     * 
     * @param uri The location of the music file.
     * @return The raw XML string.
     * @throws Exception If parsing or reading fails.
     */
    public String readMusicXmlContent(Uri uri) throws Exception {
        byte[] data = readAllBytes(uri);
        return processMusicXmlData(data);
    }

    /**
     * Reads all data from a Uri into an in-memory byte array.
     * 
     * @param uri The source location.
     * @return Byte array of the file content.
     */
    private byte[] readAllBytes(Uri uri) throws Exception {
        try (InputStream is = openInputStream(uri);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            // Buffer-based read to prevent excessive memory allocation.
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Orchestrates the transformation of raw bytes into an XML string.
     * Performs "Magic Number" detection to identify ZIP archives.
     */
    private String processMusicXmlData(byte[] data) throws Exception {
        if (isZip(data)) {
            // Compressed .mxl files enter the recursive extraction pipeline.
            return readFromZipRecursively(data);
        } else {
            // Plain XML files are decoded directly.
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /**
     * Validates if a byte array represents a ZIP file by checking for the PK header (0x50 0x4B).
     */
    private boolean isZip(byte[] data) {
        return data.length > 4 &&
                data[0] == 0x50 && data[1] == 0x4B &&
                data[2] == 0x03 && data[3] == 0x04;
    }

    /**
     * Implements recursive extraction for .mxl (Compressed MusicXML) files. 
     * It parses the ZIP internal structure and attempts to find the score file
     * based on the standard MusicXML container specification.
     * 
     * @param zipData The raw bytes of the ZIP/MXL file.
     * @return The extracted XML content string.
     */
    private String readFromZipRecursively(byte[] zipData) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        // Step 1: Unpack all files in the ZIP into an in-memory map.
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                }
                entries.put(entry.getName(), baos.toByteArray());
                zis.closeEntry();
            }
        }

        if (entries.isEmpty()) throw new Exception("ZIP file is empty");

        // Step 2: Look for the 'container.xml' meta-file which points to the root score.
        String rootFilePath = null;
        if (entries.containsKey("META-INF/container.xml")) {
            rootFilePath = getRootFilePath(entries.get("META-INF/container.xml"));
        }

        // Step 3: Extract the root file defined by the container.
        if (rootFilePath != null && entries.containsKey(rootFilePath)) {
            byte[] rootFileData = entries.get(rootFilePath);
            // Handle edge case where the root file itself might be another ZIP.
            if (isZip(rootFileData)) {
                return readFromZipRecursively(rootFileData);
            }
            return new String(rootFileData, StandardCharsets.UTF_8);
        }

        // Fallback Logic: Search for any file with typical MusicXML extensions.
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            byte[] data = entry.getValue();

            if (isZip(data)) {
                try { return readFromZipRecursively(data); } catch (Exception ignored) {}
            }

            if (name.endsWith(".xml") || name.endsWith(".musicxml")) {
                // Ignore the meta-info file during the fallback scan.
                if (!name.equals("META-INF/container.xml")) {
                    return new String(data, StandardCharsets.UTF_8);
                }
            }
        }

        // Final Fallback: If only one file exists, check if it starts with an XML declaration.
        if (entries.size() == 1) {
            byte[] data = entries.values().iterator().next();
            String content = new String(data, StandardCharsets.UTF_8);
            if (content.trim().startsWith("<?xml") || content.contains("<score-partwise")) {
                return content;
            }
        }

        throw new Exception("No valid MusicXML content found in ZIP");
    }

    /**
     * Parses the META-INF/container.xml file to extract the full-path of the root MusicXML file.
     * Standard implementation of the MusicXML 3.0+ compressed format specification.
     */
    private String getRootFilePath(byte[] containerXmlData) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(containerXmlData));
            NodeList rootfiles = doc.getElementsByTagName("rootfile");
            if (rootfiles.getLength() > 0) {
                // Extract the "full-path" attribute which indicates where the score is in the ZIP.
                return ((Element) rootfiles.item(0)).getAttribute("full-path");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing container.xml", e);
        }
        return null;
    }

    /**
     * Helper to retrieve the human-readable display name of a file from a Uri.
     */
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
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * Extracts the standardized extension of a file based on its MIME type or URI path.
     */
    public String getExtension(Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.equals("application/vnd.recordare.musicxml.zipped")) return "mxl";
            if (mimeType.equals("application/vnd.recordare.musicxml+xml")) return "musicxml";
            if (mimeType.equals("application/xml") || mimeType.equals("text/xml")) return "xml";
            if (mimeType.equals("application/zip")) return "zip";
        }
        String fileName = getFileName(uri);
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}