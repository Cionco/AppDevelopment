package de.fs.fintech.geogame;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

/**
 * Created by axel on 02.04.17.
 */

public class GeoGameApplication extends Application {

    public GeoGameApplication() {
        /**
         * http://stackoverflow.com/questions/19984838/how-to-get-slf4j-android-to-honor-logcat-logging-level
         * http://stackoverflow.com/questions/34998237/slf4j-android-1-7-not-logging-on-logcat
         * https://jira.qos.ch/browse/SLF4J-314
         *
         */
      //  HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG;
        System.setProperty("log.tag.de.fs.fintech.geogame.MainActivity", String.valueOf(Log.VERBOSE));
        System.setProperty("log.tag.MainActivity", String.valueOf(Log.VERBOSE));

        // see https://medium.com/@chrisbanes/appcompat-v23-2-daynight-d10f90c83e94
        // selecting dark theme permanently
        // https://android-developers.googleblog.com/2016/02/android-support-library-232.html
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }
}
