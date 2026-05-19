package com.example.conducto2.data.model;

import android.net.Uri;

import com.google.firebase.firestore.Exclude;

/**
 * MusicFile
 * 
 * Represents a single sheet music resource associated with a lesson. 
 * This model bridges the gap between Android's {@link Uri} objects (used for local processing)
 * and Firestore's String requirements for URLs.
 */
public class MusicFile {
    
    /** The display name of the music file. */
    private String title;

    /** 
     * The internal Android Uri representing the resource location. 
     * Excluded from direct Firestore serialization to allow custom string-based mapping.
     */
    @Exclude
    private Uri uri;

    /** Required no-argument constructor for Firestore deserialization. */
    public MusicFile() {}

    /**
     * Initializes a new music file with a title and a Uri.
     */
    public MusicFile(String title, Uri uri) {
        this.title = title;
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Exclude
    public Uri getUri() {
        return uri;
    }

    @Exclude
    public void setUri(Uri uri) {
        this.uri = uri;
    }

    /**
     * Serializes the Uri as a String for Firestore storage under the key 'url'.
     * @return The String representation of the Uri.
     */
    public String getUrl() {
        return uri != null ? uri.toString() : null;
    }

    /**
     * Deserializes a String from Firestore back into an Android Uri object.
     * @param url The string URL retrieved from the database.
     */
    public void setUrl(String url) {
        if (url != null) {
            this.uri = Uri.parse(url);
        }
    }
}