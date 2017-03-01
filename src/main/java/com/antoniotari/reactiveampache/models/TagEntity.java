package com.antoniotari.reactiveampache.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import java.util.ArrayList;

/**
 * Created by mike on 2/24/17.
 */
public class TagEntity implements Parcelable {
    @Attribute(name = "id", required = false)
    String id;

    @Element(name = "name", required = false)
    String name;

    @Element(name = "songs", required = false)
    int songs;

    /**
     * Required for deserialization
     */
    public TagEntity() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public int getSongs() { return songs; }

    protected TagEntity(Parcel in) {
        id = in.readString();
        name = in.readString();
        songs = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeInt(songs);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<TagEntity> CREATOR = new Parcelable.Creator<TagEntity>() {
        @Override
        public TagEntity createFromParcel(Parcel in) {
            return new TagEntity(in);
        }

        @Override
        public TagEntity[] newArray(int size) {
            return new TagEntity[size];
        }
    };
}
