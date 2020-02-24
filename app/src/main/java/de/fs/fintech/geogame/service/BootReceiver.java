package de.fs.fintech.geogame.service;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by axel on 10.05.17.
 */

public class BootReceiver extends AlarmReceiver {
    private static final Logger log = LoggerFactory.getLogger(BootReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        log.info("BootReceiver.onReceive ");
        String message = "booted";
        // Sets an ID for the notification
        int mNotificationId = 001;
        notify(context, message, mNotificationId);
    }
}
