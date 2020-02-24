package de.fs.fintech.geogame;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fs.fintech.geogame.parcelable.PortalInfoParcel;
import de.fs.fintech.geogame.touch.TouchPaintActivity;

/*
 *  Changes to Application:
 *
 *  25. - 26.05. Nicolas Kepper
 *  Layout finished. Simulation View is added correctly to contentView.
 *  Next Important Steps:
 *  - Make Balls spawn in the middle of the bottom Border               (Check)
 *  - Make Balls shootable                                              (Check)
 *  - Check if Balls return from borders the way they should            (Check)
 *  -> In ResolveCollisionWithBounds: Make Balls stick to bottom border (Check)
 *
 *  27. - 28.05. Nicolas Kepper
 *  Balls are flying almost correctly through the View. Only Problem is that sometimes they fall a little off
 *  track. Might be because the View doesn´t update often enough so the balls change direction after they have
 *  actually crossed the border
 *  Next Important Steps:
 *  - Make Balls that didn´t hit Y-Bound at the same place as the first one move slowly to the return place (205-215)
 *
 *
 *  29.05. Nicolas Kepper
 *  Implemented Touch Ball following the Finger as aiming tool.
 *  Next Important Steps:
 *  - Make the aiming tool appear as an arrow or something similar
 *  - (Make the touchpoint a class itself and) make the touchpoint be always the last
 *      in the ArrayList or don´t add the touchpoint to the ArrayList                           (Check)
 *  - Implement the calculation of X and Y Velocity based on how far (X and Y direction)
 *      the touchpoint is off the FirstReturnPosition                                           (Check)
 *
 *  29. - 30.05. Nicolas Kepper
 *  Implemented method to calculate X and Y Velocity based on x and y difference between
 *  starting position and touch point. Balls are flying the right way
 *  Next Important Steps:
 *  - Make balls bounce off the border the right way (if x or y coordinate is < or > than
 *      x/y Bound don´t only revert velocity in that direction but also reflect the ball on
 *      that bound                                                                              (Check)
 *
 *  30.05. Nicolas Kepper
 *  Added some additional stabilization to the flight track of the balls (Make the Bound reflect).
 *  It still doesn't completely fix the problem of the balls not flying the correct line but the tracks are
 *  a nearly perfect.
 *  Moved Aim out of ArrayList.
 *  Next Important Step:
 *  - Move the code part which should make the balls that landed somewhere else out of the
 *      resolveCollisionWithBounds method
 *
 *  02. - 11.06. Nicolas Kepper
 *  Balls don't resolve collisions with bounds when they stick to Y bound already.
 *  Started implementing Box Particles -> adapting Simulationview's size, calculating xPosition based on
 *  index of the Box, set up array for the Boxes (maybe gonna change it to be an ArrayList (or a pool) as well)
 *  Removed a bug, that game would exit when you touch the screen before all balls have returned and take the
 *  finger off the screen after all balls have returned
 *  Extracted Position calculation based on pixels from the origin and pixels from origin calculation based on position
 *  to the enum Orientation
 *  Positions are now always determined by the middle of the Balls/Boxes, i. e. calculation of mXOrigin, mYOrigin and
 *  mVerticalBound, mHorizontalBound had to be changed
 *  Changed SimulationView's height to be as high as wide + 2 boxes
 *  Started setting up the Box Structure
 *  Started implementing Hitboxes for the Boxes
 *  Next Steps:
 *  - Make boxes appear at their "meant to be" position                                 (Check)
 *  - Make boxes contain their Number (hits needed)
 *  - Make boxes have a hitbox/make a resolvecollsionwithbox or something similar       (Check)
 *
 *  12.06. Nicolas Kepper
 *  Bouncing Structure completed
 *  Box disappears after mHitsNeeded hits
 *  Next Steps:
 *  - Make boxes appear randomly                                                        (Check)
 *
 *  13.06. Nicolas Kepper
 *  All spawned boxes disappear after mHitsNeeded
 *  Boxes spawn randomly
 *  Boxes move down one row after all Balls returned
 *  Next Steps:
 *  - Make boxes have a TextView with their mHitsNeeded                                 (Check)
 *  - Implement Game Over                                                               (Check)
 *
 *  Boxes now have the TextView with the number
 *
 *  15.06. Nicolas Kepper
 *  Started Leaderboard Database
 *  Implemented GameOver
 *  Fixed a bug where the BoxesDestroyed counter would count up too fast
 *  because more than one ball would hit the box before it disappeared and
 *  would make the hitsneeded counter go below 0 before disappearing
 *  Added a Points(boxes destroyed counter) to the Activity in form of a
 *  button
 *  - Remove Database error
 *  - make Leaderboard layout look better and let it have a successful/unsuccessful
 *
 *  17.06. Nicolas Kepper
 *  LeaderBoardParcel and LeaderBoard finished
 *
 *  21.06. Nicolas Kepper
 *  Layout improved
 *  LeaderBoard adapted so it doesnt write into the same table as group Bravo
 *  Leaderboard database finished
 *  Added/changed some logs, extracted 4 string resources
 */






public class MainActivity extends AppCompatBaseActivity implements View.OnClickListener {

    private static  Logger log = LoggerFactory.getLogger(MainActivity.class);

    private static boolean isToastLifecycleEnabled=false;
    private int counter=0; // DR. JAN-NIKLAS LUDWIG (Copyright by Daniel Hofinger)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log.debug("onCreate debug"); //
        log.info("onCreate info");
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main_simple_linear);
        setContentView(R.layout.activity_main);

        Activity activity=this;
        Context context =activity;// oder auch Service
        // preferences der App (für alle Activities, Services etc)
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        // preferences der Activity
        SharedPreferences activityPref = activity.getPreferences(Context.MODE_PRIVATE);
        // preferences der Activity
        SharedPreferences privatePref = context.getSharedPreferences("mypackagekey",Context.MODE_PRIVATE);

        counter=sharedPref.getInt("counter",0);
        log.info("get counter "+counter);

        String email = sharedPref.getString("google.plus.email",null);
        setTitle(getTitle()+" "+email);

        /**
         * #1 einzelne Buttons an this.onClick() delegieren
         */
        Button btnNorth = (Button) findViewById(R.id.button_north);
        btnNorth.setOnClickListener(this);

        Button btnWest = (Button) findViewById(R.id.button_west);
        btnWest.setOnClickListener(this);

        /**
         * #2 eigene anonymous inner class pro Button
         */
        Button btnSouth= (Button) findViewById(R.id.button_south);
        btnSouth.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                counter--;
                updatePrefs();
                Log.d("MainActivity","-counter="+counter);
                log.debug("slf4j --counter="+counter);
            }

        });

        Button btnNext= (Button) findViewById(R.id.button_next);
        btnNext.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,LoginActivity.class);
                startActivity(i);
            }

        });

        /**
         * #3 gemeinsame static inner class als Click-Dispatcher (eigenes Objekt)
         */
        Button btnLogin= (Button) findViewById(R.id.button_login);
        btnLogin.setOnClickListener(new ClickDispatcher());

        Button btnLoginFB= (Button) findViewById(R.id.button_login_firebase);
        btnLoginFB.setOnClickListener(new ClickDispatcher());


        /**
         * #4 gemeinsame static inner class als Click-Dispatcher (gemeinsames Objekt)
         */
        ClickDispatcher dispatcher=new ClickDispatcher();
        Button btnInvite= (Button) findViewById(R.id.button_profile);
        btnInvite.setOnClickListener(dispatcher);

        Button btnIntro= (Button) findViewById(R.id.button_intro);
        btnIntro.setOnClickListener(dispatcher);

        /**
         * #5 gemeinsame static inner class als Click-Dispatcher (gemeinsames Objekt)
         * ohne lokale Variable für den "found view"
         */
        ((Button) findViewById(R.id.button_map)).setOnClickListener(dispatcher);
        ((Button) findViewById(R.id.button_accelerometer)).setOnClickListener(dispatcher);
        ((Button) findViewById(R.id.button_snippet_game)).setOnClickListener(dispatcher);
        ((Button) findViewById(R.id.button_jump_game)).setOnClickListener(dispatcher);
        ((Button) findViewById(R.id.button_background_test)).setOnClickListener(dispatcher);
        ((Button) findViewById(R.id.button_portal_unique_editor)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, PortalUniqueEditorActivity.class);
                PortalInfoParcel portal = new PortalInfoParcel();
                portal.title = "Test";
                i.putExtra("portal", portal);
                startActivity(i);
            }
        });
        ((Button) findViewById(R.id.button_portal_list)).setOnClickListener(dispatcher);

        /**
         * #6 Iteration über ids gleichartig zu behandelnder Views
         */
        int[] ids={
                R.id.button_touchpaint,
                R.id.button_navdrawer

        };
        for(int j=0;j<ids.length;j++) {
            ((Button) findViewById(ids[j])).setOnClickListener(dispatcher);
        }
    }

    @Override
    public void onClick(View v) {
        /** hier kann switch oder if()..else if.. verwendet werden
         * Achtung! R.id.xxx sind in App-Projekten Konstanten und können deswegen in switch verwendet werden.
         * Bei Library-Projekten sind R.id.xxx nur statisch und nicht-Konstant. Daher kann dort kein(!)
         * switch verwendet werden.
         */
        if(v.getId()==R.id.button_north) {
            counter++;
        } else if (v.getId()==R.id.button_west) {
            counter/=2;
        }
        updatePrefs();
        Log.d("MainActivity","+counter="+counter);
        log.info("counter="+counter);
    }


    public void buttonEastPress(View v) {
        counter*=2;
        updatePrefs();
        Log.d("MainActivity","+counter="+counter);
    }

    private void updatePrefs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences activityPref = this.getPreferences(Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("counter", counter);
        editor.commit();
        log.info("store counter "+counter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(isToastLifecycleEnabled) Toast.makeText(getBaseContext(),"onStart "+counter,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isToastLifecycleEnabled) Toast.makeText(getBaseContext(),"onResume",Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isToastLifecycleEnabled) Toast.makeText(getBaseContext(),"onPause",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isToastLifecycleEnabled) Toast.makeText(getBaseContext(),"onStop",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isToastLifecycleEnabled) Toast.makeText(getBaseContext(),"onDestroy",Toast.LENGTH_SHORT).show();
    }

    private class ClickDispatcher implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Class target;
            String url;
            /**
             * switch mit R.id.xxx kann nicht in Library Projekten verwendet werden.
             */
            switch(v.getId()) {
                default:
                case R.id.button_login:
                    target=LoginActivity.class;
                    break;
                case R.id.button_login_firebase:
                    target=GoogleSignInActivity.class;
                    break;
                case R.id.button_intro:
                    target=IntroDynActivity.class;
                    break;
                case R.id.button_map:
                    target=MapsActivity.class;
                    break;
                case R.id.button_profile:
                    target=ProfileActivity.class;
                    break;
                case R.id.button_portal:
                    target=PortalDetailsActivity.class;
                    break;
                case R.id.button_accelerometer:
                    target=AccelerometerPlayActivity.class;
                    break;
                case R.id.button_touchpaint:
                    target=TouchPaintActivity.class;
                    break;
                case R.id.button_navdrawer:
                    target=NavDrawerActivity.class;
                    break;
                case R.id.button_selectfaction:
                    target=SelectFactionFromListActivity.class;
                    break;
                case R.id.button_snippet_game:
                    target=PortalGameSnippetActivity.class;
                    break;
                case R.id.button_jump_game:
                    target=PortalGameActivity.class;
                    break;
                case R.id.button_background_test:
                    target=BackgroundTestActivity.class;
                    break;
                case R.id.button_portal_list:
                    target = PortalListActivity.class;
                    break;
            }
            Intent i=new Intent(MainActivity.this,target);
            startActivity(i);
        }
    }


}
