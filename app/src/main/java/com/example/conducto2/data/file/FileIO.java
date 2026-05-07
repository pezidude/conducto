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

public class FileIO {
    /**
     * This class's purpose is to handle reading MusicXML files,
     * supporting both plain XML and compressed .mxl formats (ZIP).
     */
    private static final String TAG = "FileIO";
    private final Context context;

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

    public String readMusicXmlContent(Uri uri) throws Exception {
        byte[] data = readAllBytes(uri);
        return processMusicXmlData(data);
    }

    private byte[] readAllBytes(Uri uri) throws Exception {
        try (InputStream is = openInputStream(uri);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        }
    }

    private String processMusicXmlData(byte[] data) throws Exception {
        if (isZip(data)) {
            return readFromZipRecursively(data);
        } else {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private boolean isZip(byte[] data) {
        return data.length > 4 &&
                data[0] == 0x50 && data[1] == 0x4B &&
                data[2] == 0x03 && data[3] == 0x04;
    }

    private String readFromZipRecursively(byte[] zipData) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
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

        if (entries.isEmpty()) {
            throw new Exception("ZIP file is empty");
        }

        // Check if there's a container.xml
        String rootFilePath = null;
        if (entries.containsKey("META-INF/container.xml")) {
            rootFilePath = getRootFilePath(entries.get("META-INF/container.xml"));
        }

        if (rootFilePath != null && entries.containsKey(rootFilePath)) {
            byte[] rootFileData = entries.get(rootFilePath);
            if (isZip(rootFileData)) {
                return readFromZipRecursively(rootFileData);
            }
            return new String(rootFileData, StandardCharsets.UTF_8);
        }

        // Fallback: search for any .xml or .musicxml file, or another ZIP
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            byte[] data = entry.getValue();

            if (isZip(data)) {
                try {
                    return readFromZipRecursively(data);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to recurse into nested ZIP: " + name);
                }
            }

            if (name.endsWith(".xml") || name.endsWith(".musicxml")) {
                if (!name.equals("META-INF/container.xml")) {
                    return new String(data, StandardCharsets.UTF_8);
                }
            }
        }

        // If we only have one file and it's not identified, just return it as string if it looks like XML
        if (entries.size() == 1) {
            byte[] data = entries.values().iterator().next();
            String content = new String(data, StandardCharsets.UTF_8);
            if (content.trim().startsWith("<?xml") || content.contains("<score-partwise")) {
                return content;
            }
        }

        throw new Exception("No valid MusicXML content found in ZIP");
    }

    private String getRootFilePath(byte[] containerXmlData) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(containerXmlData));
            NodeList rootfiles = doc.getElementsByTagName("rootfile");
            if (rootfiles.getLength() > 0) {
                return ((Element) rootfiles.item(0)).getAttribute("full-path");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing container.xml", e);
        }
        return null;
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

    public String getMimeType(Uri uri) {
        return context.getContentResolver().getType(uri);
    }

    public String getExtension(Uri uri) {
        String mimeType = getMimeType(uri);
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
