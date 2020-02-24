package de.fs.fintech.geogame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import de.fs.fintech.geogame.adapter.LeaderBoardAlphaListAdapter;
import de.fs.fintech.geogame.db.DatabaseHelper;
import de.fs.fintech.geogame.parcelable.LeaderBoardAlphaParcel;
import de.fs.fintech.geogame.sort.Sorter;


public class LeaderBoardAlphaActivity extends AppCompatBaseActivity {

    public static String KEY_PLAYER_NAME = "§$%&/&%$§$%&/KEY_PLAYER_NAME(§";
    public static String KEY_RESULT_CODE = "§$%&/&%$§$%&/KEY_RESULT_CODE(§";

    private static Logger log = LoggerFactory.getLogger(LeaderBoardAlphaActivity.class);
    private ListView mList;

    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader_board);

        Intent callingIntent = getIntent();

        mList = (ListView) findViewById(R.id.leaderboard_list);

        String calledName = callingIntent.getStringExtra(KEY_PLAYER_NAME);
        int calledScore = callingIntent.getIntExtra(PortalGameActivity.KEY_RESULT_SCORE, 0);
        int calledResult = callingIntent.getIntExtra(KEY_RESULT_CODE, RESULT_CANCELED);

        ((TextView) findViewById(R.id.text_captured)).setText((calledResult==RESULT_OK)?getString(R.string.capture_successful):getString(R.string.capture_failed));
        ((TextView) findViewById(R.id.text_name)).setText(calledName);
        ((TextView) findViewById(R.id.text_score)).setText(Integer.toString(calledScore));

        ArrayList<LeaderBoardAlphaParcel> array;

        array = loadLeaderBoard();

        LeaderBoardAlphaListAdapter adapter = new LeaderBoardAlphaListAdapter(this, R.layout.lvi_leaderboard, array);
        mList.setAdapter(adapter);
    }

    private DatabaseHelper getHelper() {
        if(databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    private ArrayList<LeaderBoardAlphaParcel> loadLeaderBoard() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        final DatabaseReference leaderboardRef = database.getReference("geogame/leaderboardAlpha");

        leaderboardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<LeaderBoardAlphaParcel> array = new ArrayList<LeaderBoardAlphaParcel>();
                for(DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    try {
                        LeaderBoardAlphaParcel leaderboard = postSnapshot.getValue(LeaderBoardAlphaParcel.class);
                        log.info("Loaded leaderboard entry " + leaderboard.id + " ==> " + leaderboard.name + " ==> " + leaderboard.score);
                        array.add(leaderboard);
                    } catch(Exception e) {
                        log.info("Removing " + postSnapshot.getRef().toString());
                        postSnapshot.getRef().removeValue();
                    }
                }

                Sorter.quicksort(array);
                Sorter.invert(array);

                LeaderBoardAlphaListAdapter adapter = new LeaderBoardAlphaListAdapter(LeaderBoardAlphaActivity.this, R.layout.lvi_leaderboard, array);
                mList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.warn("loadPost:onCancelled", databaseError.toException());
            }
        });

        ArrayList<LeaderBoardAlphaParcel> array = new ArrayList<>();
        return array;
    }

    public void buttonBackPressed(View v) {
        finish();
    }
}
