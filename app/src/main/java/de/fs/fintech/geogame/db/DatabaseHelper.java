package de.fs.fintech.geogame.db;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import de.fs.fintech.geogame.BuildConfig;
import de.fs.fintech.geogame.R;
import de.fs.fintech.geogame.parcelable.LeaderBoardAlphaParcel;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

/**
 * Created by axel on 10.05.17.
 */

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "geogame.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 1;

    // the DAO object we use to access the PortalInfoParcel table
    private Dao<PortalInfoParcel, String> portalDao = null;
    private RuntimeExceptionDao<PortalInfoParcel, String> portalRuntimeDao = null;
    private RuntimeExceptionDao<LeaderBoardAlphaParcel, Integer> leaderboardRuntimeDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, PortalInfoParcel.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }

        // here we try inserting data in the on-create as a test
        RuntimeExceptionDao<PortalInfoParcel, String> dao = getPortalInfoParcelDao();
         // create some entries in the onCreate
        PortalInfoParcel simple = new PortalInfoParcel(0,0,"xx");
        dao.create(simple);
        dao.delete(simple);
        Log.i(DatabaseHelper.class.getName(), "created new entries in onCreate: ");
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onUpgrade");
            // ************************************************/
            // TODO ACHTUNG !!!! SO IST DAS NICHT PRODUCTIV NUTZBAR !!! hier gehört das übliche ALTER TABLE hin
            // ************************************************/
            if(BuildConfig.DEBUG==false) throw new RuntimeException("FEHLENDER onUpgrade");
            TableUtils.dropTable(connectionSource, PortalInfoParcel.class, true);
            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our PortalInfoParcel class. It will create it or just give the cached
     * value.
     */
    public Dao<PortalInfoParcel, String> getDao() throws SQLException {
        if (portalDao == null) {
            portalDao = getDao(PortalInfoParcel.class);
        }
        return portalDao;
    }

    /**
     * Returns the RuntimeExceptionDao (Database Access Object) version of a Dao for our PortalInfoParcel class. It will
     * create it or just give the cached value. RuntimeExceptionDao only through RuntimeExceptions.
     */
    public RuntimeExceptionDao<PortalInfoParcel, String> getPortalInfoParcelDao() {
        if (portalRuntimeDao == null) {
            portalRuntimeDao = getRuntimeExceptionDao(PortalInfoParcel.class);
        }
        return portalRuntimeDao;
    }

    public RuntimeExceptionDao<LeaderBoardAlphaParcel, Integer> getLeaderBoardAlphaParcelDao() {
        if(leaderboardRuntimeDao == null) {
            leaderboardRuntimeDao = getRuntimeExceptionDao(LeaderBoardAlphaParcel.class);
        }
        return leaderboardRuntimeDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        portalDao = null;
        portalRuntimeDao = null;
    }
}