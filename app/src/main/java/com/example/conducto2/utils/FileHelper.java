package com.example.conducto2.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;

public class FileHelper {
    public static String getTitleFromUri(Context context, Uri uri) {
        String title = getTitleFromMusicXml(context, uri);
        if (title != null) {
            return title;
        }
        return getFileName(context, uri);
    }

    private static String getTitleFromMusicXml(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

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
        } catch (Exception e) {
            Log.e("FileHelper", "Error parsing MusicXML", e);
        }
        return null;
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
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
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
