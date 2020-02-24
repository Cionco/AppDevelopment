package de.fs.fintech.geogame;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fs.fintech.geogame.data.User;

public class SplashActivity  extends Activity {
    private static Logger log = LoggerFactory.getLogger(SplashActivity.class);

    private static final int REQUEST_REGISTER = 345;
    private static final int REQUEST_FIREBASE = 456;
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 1300;

    private Class<NavDrawerMapActivity> clsMain=NavDrawerMapActivity.class;
    private Class<LoginActivity> clsLogin=LoginActivity.class;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity or registration
                checkRegisteredAndContinue();
            }
        }, SPLASH_TIME_OUT);
    }

    protected void checkRegisteredAndContinue() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);
        String email = prefs.getString("google.plus.email",null);
        String gid = prefs.getString("google.plus.id",null);

        if(email==null) {
            Intent i=new Intent(SplashActivity.this,clsLogin);
            startActivityForResult(i, REQUEST_REGISTER);
        } else if(gid==null) {
            Intent i=new Intent(SplashActivity.this,clsLogin);
            startActivityForResult(i, REQUEST_FIREBASE);
        } else {
            boolean loggedIn=checkFirebaseLogin(email,gid);
            if(!loggedIn) {
                Intent i = new Intent(SplashActivity.this, clsLogin);
                startActivityForResult(i,REQUEST_FIREBASE);
                return;
            }
            Intent i = new Intent(SplashActivity.this,clsMain);
            startActivity(i);

            // close this activity
            finish();

        }
    }

    private boolean checkFirebaseLogin(final String email,final String gid) {
        try {
            if(email!=null && gid!=null) return true;

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            final String pseudonym=email.replace('.','°');
            final DatabaseReference myRef = database.getReference("geogame/users/" + pseudonym);

            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    log.info("user exists " + pseudonym + " => " + user.displayName);

                    Intent i = new Intent(SplashActivity.this, clsMain);
                    startActivity(i);

                    // close this activity
                    finish();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Intent i2 = new Intent(SplashActivity.this, clsLogin);
                    startActivityForResult(i2,REQUEST_FIREBASE);
                }
            });
            return true;
        } catch(Throwable t) {
            log.error("",t);
            return false;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == REQUEST_REGISTER) {
            if(resultCode==RESULT_OK) {
                String id = data.getStringExtra(LoginActivity.EXTRA_GOOGLE_ID);
                log.info("google id aus login ist "+id);
                checkRegisteredAndContinue();
            } else {
                finish(); // App Ende
            }

        } else if (requestCode == REQUEST_FIREBASE) {
            if(resultCode==RESULT_OK) {
                String id = data.getStringExtra(LoginActivity.EXTRA_GOOGLE_ID);
                log.info("google id aus login ist "+id);

                Intent i = new Intent(SplashActivity.this, clsMain);
                startActivity(i);

                // close this activity
                finish();
            } else {
                finish(); // App Ende
            }

        }
    }
}
