package de.fs.fintech.geogame.db;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import de.fs.fintech.geogame.parcelable.LeaderBoardAlphaParcel;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

/**
 * Created by axel on 10.05.17.
 */
public class ConfigCreatorUtil extends OrmLiteConfigUtil {
    private static final Class<?>[] classes = new Class[]{
            PortalInfoParcel.class,
            LeaderBoardAlphaParcel.class,
    };


    public static void main(String[] args) throws IOException, SQLException {
        writeConfigFile(new File("/Users/Anni/AndroidStudioProjects/2017ss-appdev/GeoGame/app/src/main/res/raw/ormlite_config.txt"), classes );
    }
}
