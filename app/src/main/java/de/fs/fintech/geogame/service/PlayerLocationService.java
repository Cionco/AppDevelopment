package de.fs.fintech.geogame.service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.fs.fintech.geogame.BuildConfig;
import de.fs.fintech.geogame.data.User;
import de.fs.fintech.geogame.db.DatabaseHelper;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

/**
 * Created by axel on 07.05.17.
 */
public class PlayerLocationService {
    private static final Logger log = LoggerFactory.getLogger(PlayerLocationService.class);
    public static final long DEFAULT_UPDATE_INTERVAL = 4000L;

    private static class MyLocationListener implements LocationListener {
        Context context;
        boolean isAnimationInProgress = false;
        protected long mUpdateInterval = 300L;

        private float detailZoom = 17f; // 18f sehr nah
        PortalMapService.PortalMapReadyListener portalMap;
        private boolean startWithNetworkProvider;


        public MyLocationListener(Context context, PortalMapService.PortalMapReadyListener portalMap, long updateInterval,boolean startWithNetworkProvider) {
            this.context = context;
            this.portalMap = portalMap;
            mUpdateInterval = updateInterval;
            this.startWithNetworkProvider=startWithNetworkProvider;
        }

        public void requestLocationUpdates(Activity context) {

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                return;
            } else {
                LocationManager mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                //mLocationListener = new MyLocationListener();
                if (mUpdateInterval < 0 || startWithNetworkProvider) {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, 1000, 100, this);

                } else {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, mUpdateInterval > 0 ? mUpdateInterval : 300, 10, this);
                }
            }
        }

        public void stopLocationUpdates() {
            log.info("stop GPS updates");
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(this);
        }

        @Override
        public void onLocationChanged(Location loc) {
            //
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean toastLocations = prefs.getBoolean("switch_toast_location_updates", false);

            if (toastLocations && BuildConfig.DEBUG) {
                Toast.makeText(
                        context,
                        "Location changed: Lat: " + loc.getLatitude() + " Lng: "
                                + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            }
            String longitude = "Longitude: " + loc.getLongitude();
            String latitude = "Latitude: " + loc.getLatitude();
            log.info("fix:" + latitude + "," + longitude);

            LatLng currentPos = new LatLng(loc.getLatitude(), loc.getLongitude());
            portalMap.setMarkerPosition(currentPos);


            if (!portalMap.isZoomed()) {
                if (isAnimationInProgress) return;
                int duration = 4000;//ms
                GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {

                    @Override
                    public void onFinish() {
                        portalMap.setIsZoomed(true);
                    }

                    @Override
                    public void onCancel() {

                    }
                };
                isAnimationInProgress = true;

                portalMap.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, detailZoom), duration, callback); // 2.0-21.0
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, detailZoom)); // 2.0-21.0
            } else {
                // Wenn der User von Hand zoomed kann er zwar mehr von der Map sehen aber nicht mehr Portale
                portalMap.getMap().moveCamera(CameraUpdateFactory.newLatLng(currentPos));
            }

            if (toastLocations) {

                /*------- To get city name from coordinates -------- */
                String cityName = null;
                Geocoder gcd = new Geocoder(context, Locale.getDefault());
                List<Address> addresses;
                try {
                    addresses = gcd.getFromLocation(loc.getLatitude(),
                            loc.getLongitude(), 1);
                    if (addresses.size() > 0) {
                        log.info("city:" + addresses.get(0).getLocality());
                        cityName = addresses.get(0).getLocality();
                    }
                } catch (IOException e) {
                    log.error("geocoder failed", e);
                }
                String s = longitude + "\n" + latitude + " : " + cityName;
                Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

    }

    public static class GeoFireLocationListener extends MyLocationListener {
        private double mLat;
        private double mLon;
        private double mapRadius;
        private GeoQuery mGeoQuery;

        // Reference of DatabaseHelper class to access its DAOs and other components
        private DatabaseHelper databaseHelper = null;

        public GeoFireLocationListener(Context context, PortalMapService.PortalMapReadyListener portalMap, float outerRadius, long updateInterval,boolean startWithNetworkProvider) {
            super(context, portalMap, updateInterval,startWithNetworkProvider);
            this.mapRadius = outerRadius;
        }

        @Override
        public void onLocationChanged(Location loc) {
            super.onLocationChanged(loc);
            mLat = loc.getLatitude();
            mLon = loc.getLongitude();

            if (mUpdateInterval < 0) {
                stopLocationUpdates();
            }

            final FirebaseDatabase database = FirebaseDatabase.getInstance();

            DatabaseReference ref = database.getReference("geogame/portals-geofire");
            GeoFire geoFire = new GeoFire(ref);

            if (mGeoQuery != null) {
                mGeoQuery.setCenter(new GeoLocation(mLat, mLon));
            } else {
                mGeoQuery = geoFire.queryAtLocation(new GeoLocation(mLat, mLon), mapRadius);
                mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                    @Override
                    public void onKeyEntered(String key, GeoLocation location) {
                        log.info(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                        PortalInfoParcel portal = portalMap.getPortal(key);
                        if (portal != null) {
                            log.debug("portal " + portal.title + " in map");
                            return;
                        }
                        final DatabaseReference myRef = database.getReference("geogame/portals/" + key);

                        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                PortalInfoParcel portalFromDb = dataSnapshot.getValue(PortalInfoParcel.class);
                                log.debug("received portal " + portalFromDb.title);
                                if (portalFromDb != null) {
                                    portalMap.addPortalMarker(portalFromDb);
                                    saveToOrmLite(portalFromDb);

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                log.error(":" + databaseError);
                                //TODO add error obj 2 array
                            }
                        });

                    }

                    @Override
                    public void onKeyExited(String key) {
                        log.info(String.format("Key %s is no longer in the search area", key));
                        portalMap.removePortal(key);

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
        }

        private void saveToOrmLite(PortalInfoParcel portalFromDb) {
            RuntimeExceptionDao<PortalInfoParcel, String> dao = getHelper().getPortalInfoParcelDao();
            List<PortalInfoParcel> result = dao.queryForEq("id", portalFromDb.id);
            if(result.size()==0) {
                dao.create(portalFromDb);
            } else {
                dao.update(portalFromDb);
            }
        }

        // This is how, DatabaseHelper can be initialized for future use
        private DatabaseHelper getHelper() {
            if (databaseHelper == null) {
                databaseHelper = OpenHelperManager.getHelper(context,DatabaseHelper.class);
            }
            return databaseHelper;
        }
    }

    public static interface OnUserStateChangeListener {
        public void onUserStateChange(User newUserState);
    }

    public static class UserStateHandler {
        User mUser;
        ArrayList<OnUserStateChangeListener> listeners=new ArrayList<OnUserStateChangeListener>();

       public UserStateHandler(final String email) {
           final FirebaseDatabase database = FirebaseDatabase.getInstance();
           String pseudonym=email.replace('.','°');
           final DatabaseReference myRef = database.getReference("geogame/users/" + pseudonym);

           myRef.addValueEventListener(new ValueEventListener() { // continuous updates
               @Override
               public void onDataChange(DataSnapshot dataSnapshot) {
                   User user = dataSnapshot.getValue(User.class);
                   if(user!=null) {
                       log.info("user updated " + email + " => " + user.displayName);
                       notifyListeners(user);
                   } else {
                       log.info("error: non existent user  " + email);
                   }
               }

               @Override
               public void onCancelled(DatabaseError databaseError) {
                   log.info("error: non existent  " + email,databaseError);
               }
           });

       }

        private void notifyListeners(User user) {
            mUser=user;
            for (OnUserStateChangeListener l: listeners) {
                log.debug("notifyListeners "+l);
                l.onUserStateChange(user);
            }
        }

        public User getUser() {
            return mUser;
        }

        public void addUserStateChangeListener(OnUserStateChangeListener listener) {
            log.debug("addUserStateChangeListener "+listener);
            listeners.add(listener);
        }

        public boolean removeUserStateChangeListener(OnUserStateChangeListener listener) {
            log.debug("removeUserStateChangeListener "+listener);
            return listeners.remove(listener);
        }
    }
}
