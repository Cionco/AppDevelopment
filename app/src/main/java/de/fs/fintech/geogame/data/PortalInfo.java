package de.fs.fintech.geogame.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="portals")
public class PortalInfo {

    @DatabaseField(id=true)
    public String id;
    @DatabaseField(canBeNull = false)
    public String title;
    @DatabaseField(canBeNull = true)
    public String description;
    @DatabaseField(canBeNull = true)
    public String urlPhoto;
    @DatabaseField(canBeNull = false)
    public double lon;
    @DatabaseField(canBeNull = false)
    public double lat;
    @DatabaseField(canBeNull = false)
    public boolean approved;

    /** empty constructor always required for JSON serialization */
    public PortalInfo() {
    }

    public PortalInfo(double lon,double lat,String title) {
        this.title=title;
        this.lon=lon;
        this.lat=lat;

        long lonL= (long) (lon*1000);
        long latL= (long) (lat*1000);
        id=lonL+","+latL+","+title.hashCode();
    }

}
