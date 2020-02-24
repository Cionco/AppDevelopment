package de.fs.fintech.geogame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.fs.fintech.geogame.adapter.PortalListAdapter;
import de.fs.fintech.geogame.db.DatabaseHelper;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

public class PortalListActivity extends AppCompatBaseActivity implements AdapterView.OnItemClickListener {
    private boolean MIGRATE_DATA = false;
    private static Logger log = LoggerFactory.getLogger(PortalListActivity.class);
    private ListView mList;
    private double mLat;
    private double mLon;
    // Reference of DatabaseHelper class to access its DAOs and other components
    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_list);

        mList = (ListView) findViewById(R.id.portalListView);

        Intent callingIntent=getIntent();
        mLat=callingIntent.getDoubleExtra("lat",Double.NaN);
        mLon=callingIntent.getDoubleExtra("lon",Double.NaN);
        if(Double.isNaN(mLat)||Double.isNaN(mLon)) {
            MIGRATE_DATA=true;
            setTitle("Portal Data Migration");
        } else {
            setTitle("Nearby Portals");
        }
        ArrayList<PortalInfoParcel> array;
        if(callingIntent.getBooleanExtra("fromCache",false)) {
            array = loadPortalsFromDb(0.6);
        } else {
            array = loadPortals(0.6);
        }
        PortalListAdapter adapter = new PortalListAdapter(this, R.layout.lvi_portal, array);
        mList.setAdapter(adapter);
        mList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PortalInfoParcel item = (PortalInfoParcel) mList.getAdapter().getItem(position);
        Toast.makeText(this, item + " selected", Toast.LENGTH_LONG).show();
        log.info(item + " selected");

        Intent intent = new Intent(this, PortalDetailsActivity.class);
        //intent.putExtra("id",item.id);
        // DONE PortalInfo als Parcelable
        intent.putExtra(PortalDetailsActivity.EXTRA_PORTAL_PARCEL, item);
        startActivity(intent);
    }

    // This is how, DatabaseHelper can be initialized for future use
    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this,DatabaseHelper.class);
        }
        return databaseHelper;
    }

    /** from cache
     *
     * @return
     */
    private ArrayList<PortalInfoParcel> loadPortalsFromDb(double radius) {
        log.info("loadPortalsFromDb");
        double distLat=0.1; // TODO http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates
        double distLon=0.1; // see more http://www.movable-type.co.uk/scripts/latlong.html

        ArrayList<PortalInfoParcel> result=new ArrayList<PortalInfoParcel>();
        RuntimeExceptionDao<PortalInfoParcel, String> dao = getHelper().getPortalInfoParcelDao();
        QueryBuilder<PortalInfoParcel, String> qb = dao.queryBuilder();
        try {
            Where<PortalInfoParcel, String> where = qb.where();
            where.between("lat",mLat-distLat,mLat+distLat) // einfache Näherung die aber nicht am 0 Meridian funktioniert!
            .between("lon",mLon-distLon,mLon-distLon);
            PreparedQuery<PortalInfoParcel> preparedQuery = qb.prepare();
            log.info("PreparedQuery: " + preparedQuery.toString());
            List<PortalInfoParcel> resultSet = dao.query(preparedQuery);
            result.addAll(resultSet);
        } catch (SQLException e) {
           log.error("",e);
        }
        return result;
    }

    private ArrayList<PortalInfoParcel> loadPortals(double radius) {

        log.info("loadPortals");
        /*
        for(int i=0;i<15;i++) {
            PortalInfoParcel portal=new PortalInfoParcel(0,0,"Portal#"+i);
            array.add(portal);
        }
        */
        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        if(MIGRATE_DATA) {
            final DatabaseReference portalsRef = database.getReference("geogame/portals");

            //portalsRef.addValueEventListener(new ValueEventListener() {
            portalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    log.info("onDataChange");
                    ArrayList<PortalInfoParcel> arraylist = new ArrayList<PortalInfoParcel>();

                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        PortalInfoParcel portal = postSnapshot.getValue(PortalInfoParcel.class);
                        arraylist.add(portal);

                        if (MIGRATE_DATA) {
                            // Migration von Alt-Daten
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geogame/portals-geofire");
                            GeoFire geoFire = new GeoFire(ref);
                            geoFire.setLocation(portal.id, new GeoLocation(portal.lat, portal.lon));
                        }
                    }

                    PortalListAdapter adapter = new PortalListAdapter(PortalListActivity.this, R.layout.lvi_portal, arraylist);
                    mList.setAdapter(adapter);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    log.warn("loadPost:onCancelled", databaseError.toException());
                    // ...
                }
            });
        } else {
            final ArrayList<PortalInfoParcel> array = new ArrayList<PortalInfoParcel>();
            DatabaseReference ref = database.getReference("geogame/portals-geofire");
            GeoFire geoFire = new GeoFire(ref);
            // creates a new query around [37.7832, -122.4056] with a radius of 0.6 kilometers

            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLat, mLon), radius);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    log.info(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                    final DatabaseReference myRef = database.getReference("geogame/portals/" + key);

                    myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            PortalInfoParcel portalFromDb = dataSnapshot.getValue(PortalInfoParcel.class);
                            if (portalFromDb != null) {
                                array.add(portalFromDb);
                                log.info("add #"+array.size());
                                PortalListAdapter adapter = new PortalListAdapter(PortalListActivity.this, R.layout.lvi_portal, array);
                                mList.setAdapter(adapter);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            log.error( ":" + databaseError);
                            //TODO add error obj 2 array
                        }
                    });

                }

                @Override
                public void onKeyExited(String key) {
                    log.info(String.format("Key %s is no longer in the search area", key));
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    log.info(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
                }

                @Override
                public void onGeoQueryReady() {
                    log.info("All initial data has been loaded and events have been fired!");
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    log.error("There was an error with this query: " + error);
                }
            });
        }
        ArrayList<PortalInfoParcel> array = new ArrayList<PortalInfoParcel>();
        return array;
    }


}
