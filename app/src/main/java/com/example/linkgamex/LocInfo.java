package com.example.linkgamex;

import android.os.Parcel;
import android.os.Parcelable;

public class LocInfo implements Parcelable {
    int x,y;

    public LocInfo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.x);
        dest.writeInt(this.y);
    }

    protected LocInfo(Parcel in) {
        this.x = in.readInt();
        this.y = in.readInt();
    }

    public static final Parcelable.Creator<LocInfo> CREATOR = new Parcelable.Creator<LocInfo>() {
        @Override
        public LocInfo createFromParcel(Parcel source) {
            return new LocInfo(source);
        }

        @Override
        public LocInfo[] newArray(int size) {
            return new LocInfo[size];
        }
    };
}
