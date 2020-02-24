package de.fs.fintech.geogame;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.appinvite.AppInviteInvitation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fs.fintech.geogame.service.AlarmReceiver;

public class NavDrawerActivity extends AppCompatBaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final Logger log = LoggerFactory.getLogger(NavDrawerActivity.class);

    private static final int REQUEST_INVITE = 123;
    private static final int REQUEST_GAME_RESULT = 124;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Alternative auch Bottom-Sheets
                // https://material.io/guidelines/components/bottom-sheets.html
                Snackbar.make(view, "Create Portal", Snackbar.LENGTH_LONG)
                        .setAction("Create Portal here", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                createPortalHere();
                            }
                        }).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerLayout = navigationView.getHeaderView(0);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String name = prefs.getString("google.plus.displayName",null);
        String email = prefs.getString("google.plus.email",null);
        TextView drawerName= (TextView) headerLayout.findViewById(R.id.drawerName);
        drawerName.setText(name);
        TextView drawerEmail= (TextView) headerLayout.findViewById(R.id.drawerEmail);
        drawerEmail.setText(email);

    }

    private void createPortalHere() {
        Intent i=new Intent(this,PortalEditorActivity.class);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nav_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent=new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_timer) {
            scheduleTimerAlarm(10);
            return true;
        } else if (id == R.id.action_debug) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_test) {
            Intent intent = new Intent(this, PortalGameSnippetActivity.class);
            startActivityForResult(intent, REQUEST_GAME_RESULT);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Intent intent=new Intent(this, PortalEditorActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_gallery) {
            Intent intent=new Intent(this, PortalListActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_help) {
            Intent intent=new Intent(this, ScrollingHelpActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_share) {
            Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                    .setMessage(getString(R.string.invitation_message))
                    .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                    .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                    .setCallToActionText(getString(R.string.invitation_cta))
                    .build();
            startActivityForResult(intent, REQUEST_INVITE);
            return true;
        } else if (id == R.id.nav_slideshow) {
            Intent intent=new Intent(this, ImportPortalCsvActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_mapoverview) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            double radius = Double.parseDouble(prefs.getString("map.overview.radius", "5"));

            Intent intent=new Intent(this, MapsActivity.class);
            intent.putExtra(MapsActivity.EXTRA_MAP_RADIUS,radius);
            intent.putExtra(MapsActivity.EXTRA_UPDATE_INTERVAL,-1L);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_send) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        } else if(id == R.id.nav_game) {
            Intent intent = new Intent(this, PortalGameActivityNicolas.class);
            startActivity(intent);
        } else if(id == R.id.nav_Jamin) {
            Intent intent = new Intent(this, PortalGameActivityJamin.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void scheduleTimerAlarm(int duration) {
        log.info("scheduleTimerAlarm "+duration);
        // Calculate the time when it expires.
        long wakeupTime = System.currentTimeMillis() + duration*1000L;

        Intent myIntent = new Intent(this, AlarmReceiver.class);
        myIntent.putExtra("text","Message from "+getTitle());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent,0);

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, wakeupTime, pendingIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_GAME_RESULT) {
            if(resultCode == RESULT_OK) {
                log.info("Player reached " + data.getIntExtra("KEY_CORRECT_ANSWERS", 0) + " points!");
            }
            else
                log.info("Player didn´t make it in time. He/She reached " + data.getIntExtra("KEY_CORRECT_ANSWERS", 0) + " points anyways!");
        }
    }
}
