package de.fs.fintech.geogame.parcelable;

import android.os.Parcel;
import android.os.Parcelable;
import de.fs.fintech.geogame.data.LeaderBoardAlpha;

/**
 * Created by nicolaskepper on 15.06.17.
 */

public class LeaderBoardAlphaParcel extends LeaderBoardAlpha implements Parcelable {

    public LeaderBoardAlphaParcel() {
        super();
    }

    public LeaderBoardAlphaParcel(String name, int score) {
        super(name, score);
    }

    protected LeaderBoardAlphaParcel(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.score = in.readInt();

    }

    public static final Creator<LeaderBoardAlphaParcel> CREATOR = new Creator<LeaderBoardAlphaParcel>() {
        @Override
        public LeaderBoardAlphaParcel createFromParcel(Parcel in) { return new LeaderBoardAlphaParcel(in); }

        @Override
        public LeaderBoardAlphaParcel[] newArray(int size) { return new LeaderBoardAlphaParcel[size]; }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeInt(score);
    }
}
