package de.fs.fintech.geogame.service;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import de.fs.fintech.geogame.BuildConfig;
import de.fs.fintech.geogame.PortalDetailsActivity;
import de.fs.fintech.geogame.R;
import de.fs.fintech.geogame.data.PortalUnique;
import de.fs.fintech.geogame.data.User;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

/**
 * Created by axel on 07.05.17.
 */

public class PortalMapService {
    private static final Logger log = LoggerFactory.getLogger(PortalMapService.class);

    public static final double DEFAULT_INNER_RADIUS = 0.020f;
    public static final double DEFAULT_MAP_RADIUS = 20f; //km

    public static abstract class PortalMarkerDecorator {
    	
    	public abstract MarkerOptions decorate(PortalInfoParcel portal);
    	
    }
    
    public static class SimplePortalMarkerDecorator extends PortalMarkerDecorator {
    	
    	public MarkerOptions decorate(PortalInfoParcel portalFromDb) {
            LatLng pos = new LatLng(portalFromDb.lat, portalFromDb.lon);
            return new MarkerOptions().position(pos).title(portalFromDb.title);
    	}
    	
    }
    
    public static class UniquePortalMarkerDecorator extends PortalMarkerDecorator {
    	
    	public MarkerOptions decorate(PortalInfoParcel portalFromDb) {
    		if(BuildConfig.DEBUG && portalFromDb.title.equals("Grüne Vögel")) {
                log.info("piep");
            }

    		float color;
    		PortalUnique uq=portalFromDb.usersUnique;
    		int count=countBits(uq==null?0:uq.resoBits);
    		if(count==8) {
    			color=BitmapDescriptorFactory.HUE_BLUE;
    		} else if(count>=6) {
    			color=BitmapDescriptorFactory.HUE_AZURE;
            } else if(count>=4) {
                color=BitmapDescriptorFactory.HUE_GREEN;
    		} else if(count==0) {
    			color=BitmapDescriptorFactory.HUE_RED;
    		} else {
    			color=BitmapDescriptorFactory.HUE_YELLOW;
    		}
    		float alpha=0.4f+0.6f*(8f-(float)count)/8f;
    		
            LatLng pos = new LatLng(portalFromDb.lat, portalFromDb.lon);
            return new MarkerOptions()
            		.position(pos)
            		.title(portalFromDb.title)
            		.icon(BitmapDescriptorFactory.defaultMarker(color))
            		.alpha(alpha)
            		;
    	}
    	
    }
    
    public static class PortalMapReadyListener implements OnMapReadyCallback, PlayerLocationService.OnUserStateChangeListener {

        Activity context;
        private Class<? extends Activity> clsActivityDetails;
        private final PortalMarkerDecorator mDecorator;
        private final PlayerLocationService.UserStateHandler mUserState;

        private GoogleMap mMap;
        private Marker mMarker;
        private Circle mSearchCircle;
        private Circle mRangeCircle;

        private float mapRadius=0.6f;//km
        private float mRadiusRange = (float) DEFAULT_INNER_RADIUS;

        private boolean isZoomed = false;
        private float azimut;

        private HashMap<String, PortalInfoParcel> mPortalsOnMap;
        private HashMap<String, Marker> mMarkersOnMap;

        public PortalMapReadyListener(Activity activity, float outerRadius, Class<? extends Activity> clsActivityDetails, PortalMarkerDecorator decorator, PlayerLocationService.UserStateHandler userStateHandler) {
            context=activity;
            this.mapRadius=outerRadius;
            this.clsActivityDetails=clsActivityDetails;
            this.mDecorator=decorator;
            this.mUserState=userStateHandler;
            mUserState.addUserStateChangeListener(this);

            mPortalsOnMap = new HashMap<String, PortalInfoParcel>();
            mMarkersOnMap = new HashMap<String, Marker>();


        }


        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera. In this case,
         * we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to install
         * it inside the SupportMapFragment. This method will only be triggered once the user has
         * installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

            try {
                // Customise the styling of the base map using a JSON object defined
                // in a raw resource file.
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                context, R.raw.gmaps_style_json));

                if (!success) {
                    log.error("Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                log.error("Can't find style. Error: ", e);
            }

            // Add a marker in Sydney and move the camera -34, 151
            LatLng nowhere = new LatLng(48, -14);
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(nowhere)
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .rotation(90.0f + azimut)
                    .title("Player")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow_back_cyan_100_24dp))

            );
            mSearchCircle = mMap.addCircle(new CircleOptions()
                    .center(nowhere)
                    .radius(mapRadius * 1000f)
                    .strokeWidth(2f)
                    .strokeColor(Color.CYAN)
                    //.fillColor(mFillColorArgb)
                    .clickable(false));

            mRangeCircle = mMap.addCircle(new CircleOptions()
                    .center(nowhere)
                    .radius(mRadiusRange)//m
                    .strokeWidth(2f)
                    .strokeColor(Color.YELLOW)
                    //.fillColor(mFillColorArgb)
                    .clickable(false));
        /* zoom:
        * 1: Welt
        * 5: Landmasse/Kontinent
        * 10: Stadt
        * 15: Straßen
        * 20: Gebäude
         */
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nowhere, 2f));
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    PortalInfoParcel portal = (PortalInfoParcel) marker.getTag();
                    if(portal!=null) {
                        Intent intent = new Intent(context, clsActivityDetails);
                        intent.putExtra(PortalDetailsActivity.EXTRA_PORTAL_PARCEL, portal);
                        context.startActivity(intent);
                    }
                    return false;
                }
            });

        }

        public void setMarkerPosition(LatLng markerPosition) {
            mMarker.setPosition(markerPosition);
            mRangeCircle.setCenter(markerPosition);
            mSearchCircle.setCenter(markerPosition);
        }

        public void setMarkerDirection(float degrees) {
            azimut=degrees;
            if(mMarker!=null)
                mMarker.setRotation(90f+degrees);
        }

        public boolean isZoomed() {
            return isZoomed;
        }

        public void setIsZoomed(boolean isZoomed) {
            this.isZoomed = isZoomed;
        }


        public GoogleMap getMap() {
            return mMap;
        }

        public void addPortalMarker(PortalInfoParcel portalFromDb) {
            User userState=mUserState.getUser();
            PortalUnique newUq = userState.uniques.get(portalFromDb.id);
            if(newUq!=null) {
                portalFromDb.usersUnique=newUq;
            }
            mPortalsOnMap.put(portalFromDb.id, portalFromDb);
            createMarker(portalFromDb);        }

        public void updatePortalMarker(PortalInfoParcel portalFromDb, User userState) {
            PortalUnique newUq = userState.uniques.get(portalFromDb.id);
            if(portalFromDb.usersUnique!=null
                    && newUq.hashCode()==portalFromDb.usersUnique.hashCode()) {
                return;
            }
            portalFromDb.usersUnique=newUq;
            Marker oldMarker = mMarkersOnMap.remove(portalFromDb.id);
            oldMarker.remove();
            createMarker(portalFromDb);
        }

        protected void createMarker(PortalInfoParcel portalFromDb) {
            MarkerOptions options=mDecorator.decorate(portalFromDb);
            Marker marker = mMap.addMarker(options);
            mMarkersOnMap.put(portalFromDb.id, marker);
            marker.setTag(portalFromDb);
            log.debug("add marker "+portalFromDb.id);
        }

        public PortalInfoParcel getPortal(String key) {
            return mPortalsOnMap.get(key);
        }

        public void removePortal(String key) {
            mPortalsOnMap.remove(key);
            Marker marker = mMarkersOnMap.remove(key);
            if(marker!=null) {
                marker.remove();
                log.debug("remove marker "+key);
            }
        }

        public int counter=0;
        @Override
        public void onUserStateChange(User newUserState) {
            log.info("onUserStateChange:"+(counter++));
            for(String id:newUserState.uniques.keySet()) {
                PortalInfoParcel portal;
                if((portal=mPortalsOnMap.get(id))!=null) {
                    updatePortalMarker(portal,newUserState);
                }
            }
        }
    }

	public static int countBits(int resoBits) {
		int count=0;
		for(int i=0;i<8;i++) {
			count+=resoBits & 1;
			resoBits>>=1;
		}
		return count;
	}

}
