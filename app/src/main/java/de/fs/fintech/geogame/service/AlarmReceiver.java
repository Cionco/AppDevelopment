package de.fs.fintech.geogame.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fs.fintech.geogame.NavDrawerMapActivity;
import de.fs.fintech.geogame.R;

/**
 * Created by axel on 10.05.17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(AlarmReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        log.info("AlarmReceiver.onReceive ");
        String message = intent.getStringExtra("text");
        // Sets an ID for the notification
        int mNotificationId = 001;
        notify(context, message, mNotificationId);
    }

    protected void notify(Context context, String message, int mNotificationId) {
        // see https://developer.android.com/training/notify-user/build-notification.html
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        // https://romannurik.github.io/AndroidAssetStudio/icons-notification.html#source.type=clipart&source.clipart=ac_unit&source.space.trim=1&source.space.pad=0&name=ic_stat_ac_unit
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Alarm GeoGame")
                        .setContentText(message);


        Intent resultIntent = new Intent(context, NavDrawerMapActivity.class);
        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
}
