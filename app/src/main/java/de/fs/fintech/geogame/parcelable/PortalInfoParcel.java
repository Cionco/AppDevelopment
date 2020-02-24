package de.fs.fintech.geogame.parcelable;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.fs.fintech.geogame.data.PortalInfo;
import de.fs.fintech.geogame.data.PortalUnique;

/**
 * Created by axel on 01.05.17.
 */

public class PortalInfoParcel extends PortalInfo implements Parcelable {

	/** set with data from User.uniques when portal enters map.
	 * used by PortalMapService to highlight unique status */
	@JsonIgnore
	public PortalUnique usersUnique;
	
    public PortalInfoParcel() {
        super();
    }

    public PortalInfoParcel(double lon,double lat,String title) {
        super(lon,lat,title);
    }

    protected PortalInfoParcel(Parcel in) {
        String[] data = new String[4];

        in.readStringArray(data);
        // the order needs to be the same as in writeToParcel() method
        this.id = data[0];
        this.title = data[1];
        this.description = data[2];
        this.urlPhoto = data[3];

        this.lon=in.readDouble();
        this.lat=in.readDouble();

        boolean[] data2=new boolean[1];
        this.approved=data2[0];
    }

    public static final Creator<PortalInfoParcel> CREATOR = new Creator<PortalInfoParcel>() {
        @Override
        public PortalInfoParcel createFromParcel(Parcel in) {
            return new PortalInfoParcel(in);
        }

        @Override
        public PortalInfoParcel[] newArray(int size) {
            return new PortalInfoParcel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{this.id,
                this.title,
                this.description,
                this.urlPhoto});
        dest.writeDouble(lon);
        dest.writeDouble(lat);
        dest.writeBooleanArray(new boolean[]{
                this.approved
        });
    }
}
