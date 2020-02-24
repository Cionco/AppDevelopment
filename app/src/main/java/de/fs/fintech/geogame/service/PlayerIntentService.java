package de.fs.fintech.geogame.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fs.fintech.geogame.PortalUniqueEditorActivity;
import de.fs.fintech.geogame.data.PortalUnique;
import de.fs.fintech.geogame.parcelable.PortalUniqueParcel;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class PlayerIntentService extends IntentService {
    private static Logger log = LoggerFactory.getLogger(PlayerIntentService.class);

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_UNIQUE = "de.fs.fintech.geogame.service.action.UNIQUE";

    public static final String EXTRA_PORTAL_ID = "de.fs.fintech.geogame.service.extra.PORTAL_ID";
    public static final String EXTRA_UNIQUE = "de.fs.fintech.geogame.service.extra.PORTAL_UNIQUE";

    public static final String ACTION_BCST_RESPONSE =
            "de.fs.fintech.geogame.intent.action.MESSAGE_PROCESSED_UNIQUE";
    public static final String EXTRA_BCST_OK = "ok";

    public PlayerIntentService() {
        super("PlayerIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFoo(Context context, String param1, PortalUniqueParcel param2) {
        Intent intent = new Intent(context, PlayerIntentService.class);
        intent.setAction(ACTION_UNIQUE);
        intent.putExtra(EXTRA_PORTAL_ID, param1);
        intent.putExtra(EXTRA_UNIQUE, param2);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UNIQUE.equals(action)) {
                final String portalId = intent.getStringExtra(EXTRA_PORTAL_ID);
                final PortalUniqueParcel unique = intent.getParcelableExtra(EXTRA_UNIQUE);
                handleActionUnique(portalId, unique);
            }
        }
    }

    @NonNull
    public static String getUniquePortalId(Context context,String portalId) {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        String email = prefs.getString("google.plus.email",null);
        String pseudonym=email.replace('.','°');

        return "geogame/users/" + pseudonym+"/uniques/"+portalId;
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUnique(String portalId, PortalUniqueParcel uq) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String uniquesPortal=getUniquePortalId(this,portalId);
        final DatabaseReference myRef = database.getReference(uniquesPortal);
        myRef.setValue(uq);

        broadcastUpdate(portalId,true);
    }

    private void broadcastUpdate(String portalId, boolean ok) {
        Intent bcst=new Intent();
        bcst.setAction(ACTION_BCST_RESPONSE);
        bcst.putExtra(EXTRA_PORTAL_ID,portalId);
        bcst.putExtra(EXTRA_BCST_OK,ok);
        log.info("send broadcast ok="+ok);
        sendBroadcast(bcst);
    }


}
