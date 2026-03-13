package com.example.conducto2.data.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;

/**
 * Represents a single music file with its title and storage URL.
 * Implements Parcelable to be passed between activities.
 */
public class MusicFile implements Parcelable {
    private String title;

    @Exclude
    private Uri uri;

    // Default constructor is required for calls to DataSnapshot.getValue(MusicFile.class)
    public MusicFile() {}

    public MusicFile(String title, Uri uri) {
        this.title = title;
        this.uri = uri;
    }

    protected MusicFile(Parcel in) {
        title = in.readString();
        uri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<MusicFile> CREATOR = new Creator<MusicFile>() {
        @Override
        public MusicFile createFromParcel(Parcel in) {
            return new MusicFile(in);
        }

        @Override
        public MusicFile[] newArray(int size) {
            return new MusicFile[size];
        }
    };

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
     * This getter is used by Firestore to serialize the Uri as a String.
     * The property name in Firestore will be 'url'.
     */
    public String getUrl() {
        return uri != null ? uri.toString() : null;
    }

    /**
     * This setter is used by Firestore to deserialize a String into a Uri.
     */
    public void setUrl(String url) {
        if (url != null) {
            this.uri = Uri.parse(url);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeParcelable(uri, flags);
    }
}