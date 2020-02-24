package de.fs.fintech.geogame.parcelable;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.fs.fintech.geogame.data.PortalInfo;
import de.fs.fintech.geogame.data.PortalUnique;


public class PortalUniqueParcel extends PortalUnique implements Parcelable {


    public PortalUniqueParcel() {
        super();
    }


    protected PortalUniqueParcel(Parcel in) {

        // the order needs to be the same as in writeToParcel() method
        this.resoBits = in.readInt();

        boolean[] data2=new boolean[2];
        in.readBooleanArray(data2);
        this.captured = data2[0];
        this.visited = data2[1];
    }

    public static final Creator<PortalUniqueParcel> CREATOR = new Creator<PortalUniqueParcel>() {
        @Override
        public PortalUniqueParcel createFromParcel(Parcel in) {
            return new PortalUniqueParcel(in);
        }

        @Override
        public PortalUniqueParcel[] newArray(int size) {
            return new PortalUniqueParcel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.resoBits);
        dest.writeBooleanArray(new boolean[]{
                this.captured, this.visited
        });
    }
}
