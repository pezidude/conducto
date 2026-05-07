package com.example.conducto2.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileHelper {
    public static String getTitleFromUri(Context context, Uri uri) {
        String title = getTitleFromMusicXml(context, uri);
        if (title != null) {
            return title;
        }
        return getFileName(context, uri);
    }

    private static InputStream openInputStream(Context context, Uri uri) throws Exception {
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return new URL(uri.toString()).openStream();
        }
        return context.getContentResolver().openInputStream(uri);
    }

    private static String getTitleFromMusicXml(Context context, Uri uri) {
        try (InputStream inputStream = openInputStream(context, uri)) {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            bis.mark(4);
            byte[] header = new byte[4];
            int read = bis.read(header);
            bis.reset();

            if (read >= 2 && header[0] == 'P' && header[1] == 'K') {
                return getTitleFromZippedMusicXml(bis);
            } else {
                return parseTitleFromStream(bis);
            }
        } catch (Exception e) {
            Log.e("FileHelper", "Error parsing MusicXML", e);
        }
        return null;
    }

    private static String getTitleFromZippedMusicXml(InputStream is) throws Exception {
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            if (!entry.isDirectory() && name.toLowerCase().endsWith(".xml")
                    && !name.equalsIgnoreCase("META-INF/container.xml")
                    && !name.equalsIgnoreCase("container.xml")) {
                return parseTitleFromStream(zis);
            }
        }
        return null;
    }

    private static String parseTitleFromStream(InputStream is) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(is, null);

        int eventType = parser.getEventType();
        boolean inWorkTitle = false;
        boolean inMovementTitle = false;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if ("work-title".equals(tagName)) {
                        inWorkTitle = true;
                    } else if ("movement-title".equals(tagName)) {
                        inMovementTitle = true;
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (inWorkTitle || inMovementTitle) {
                        String parsedTitle = parser.getText();
                        if (parsedTitle != null && !parsedTitle.trim().isEmpty()) {
                            return parsedTitle.trim();
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if ("work-title".equals(tagName)) {
                        inWorkTitle = false;
                    } else if ("movement-title".equals(tagName)) {
                        inMovementTitle = false;
                    }
                    break;
            }
            eventType = parser.next();
        }
        return null;
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
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
}
