package com.example.conducto2.data.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a single music file with its title and storage URL.
 * Implements Parcelable to be passed between activities.
 */
public class MusicFile implements Parcelable {
    private String title;
    private String url;

    // Default constructor is required for calls to DataSnapshot.getValue(MusicFile.class)
    public MusicFile() {}

    public MusicFile(String title, String url) {
        this.title = title;
        this.url = url;
    }

    protected MusicFile(Parcel in) {
        title = in.readString();
        url = in.readString();
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(url);
    }
}