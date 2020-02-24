package de.fs.fintech.geogame;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.fs.fintech.geogame.data.PortalInfo;
import de.fs.fintech.geogame.data.User;

public class PortalEditorActivity extends AppCompatBaseActivity {
    public static final String THUMB_SUFFIX = ".th.jpg";
    private static Logger log = LoggerFactory.getLogger(PortalEditorActivity.class);

    static final int REQUEST_IMAGE_CAPTURE = 432;
    private ImageButton mImageButton;
    private String mCurrentPhotoPath;
    private double mLat=Double.NaN;
    private double mLon=Double.NaN;
    private EditText mEditTitle;
    private EditText mEditDescription;
    private int mOrientation;
    private String mThumbPhotoPath;
    private CurrentLocationListener mLocationListener;
    private boolean mIsPictureUploaded;
    private boolean mIsPortalInfoUploaded;
    private boolean mIsHiresPictureUploaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_editor);

        // TODO: onSaveInstanceState etc.

        mImageButton= (ImageButton) findViewById(R.id.imageButtonTakePhoto);
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            /**
             * see https://developer.android.com/training/camera/photobasics.html
             */
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // TODO: wenn die WRITE_EXTERNAL_STORAGE nicht granted ist könnte man den Thumbnail skaliert in den Button setzen.
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        log.error("",ex);
                    }
                    // Continue only if the File was successfully created
                    // (BTW: bad style exception handling in example)
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(PortalEditorActivity.this,
                                "de.fs.fintech.geogame.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    }
                    // wenn kein output gesetzt wurde kommt nur das Thumbnail
                    // http://stackoverflow.com/questions/9890757/android-camera-data-intent-returns-null
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                }

            }
        });

        mEditTitle = (EditText) findViewById(R.id.editTextTitle);
        mEditDescription = (EditText) findViewById(R.id.editTextDescription);

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // receive the thumbnail while JPEG full image is witten in the background to slow SD
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data != null && data.getExtras()!=null) {
                // data sollte null sein wenn wir ein richtiges Photo bekommen
                // Android 7.1.2 kann extras auch null sein
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                mImageButton.setImageBitmap(imageBitmap);
            } else {
                try {
                    setPic();
                } catch (IOException e) {
                    log.error("",e);
                }
            }
        }
    }

    private void setPic() throws IOException {

        ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
        mOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        log.info(mCurrentPhotoPath+" Orientation="+mOrientation);

        /* nicht im main thread:
        * Bitmap thumbnail= Picasso.with(this).load(new File(mCurrentPhotoPath)).rotate(exifToDegrees(Orientation)).resize(300, 300).centerInside().get();
        * String thumbname=mCurrentPhotoPath+ THUMB_SUFFIX;
        * saveFile(thumbnail,thumbname);
        */
        new ThumbnailTask().execute();

        // Get the dimensions of the View
        int targetW = mImageButton.getWidth();
        int targetH = mImageButton.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        //bmOptions.inJustDecodeBounds=true;

        Bitmap bitmap =null;
        try {
            bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            //if(Orientation!=ExifInterface.ORIENTATION_NORMAL) {
            // rotate to portrait
            Matrix matrix = new Matrix();
            int w=bitmap.getWidth();
            int h=bitmap.getHeight();

            if (mOrientation == ExifInterface.ORIENTATION_ROTATE_90) { // 6
                log.debug("rot 90");
                matrix.postRotate(90);
            }
            else if (mOrientation == ExifInterface.ORIENTATION_ROTATE_180) { // 3
                log.debug("rot 180");
                matrix.postRotate(180);
            }
            else if (mOrientation == ExifInterface.ORIENTATION_ROTATE_270) { // 8
                log.debug("rot 270");
                matrix.postRotate(270);
            }
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
            bitmap.recycle();
            bitmap=rotatedBitmap;

            // bei 0 Grad wird die JPEG Qualität reduziert
            saveFile(bitmap, mCurrentPhotoPath);

        } catch (Exception e) {
            log.error("",e);
        } finally {
            if(bitmap!=null) bitmap.recycle();
        }

        //}
        //mImageButton.setImageBitmap(bitmap);
    }

    private static void saveFile(Bitmap rotatedBitmap, String path) throws IOException {
        FileOutputStream out = new FileOutputStream(path);
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        out.flush();
        out.close();
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.portal_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send) {
            if(Double.isNaN(mLon) || Double.isNaN(mLat)) {
                Toast.makeText(this,"no position, yet",Toast.LENGTH_SHORT).show();
                return true;
            }

            String title=mEditTitle.getText().toString();
            PortalInfo portal=new PortalInfo(mLon,mLat,title);
            portal.description=mEditDescription.getText().toString();
            if(mThumbPhotoPath!=null) {
                portal.urlPhoto=mThumbPhotoPath;
            } else {
                portal.urlPhoto=mCurrentPhotoPath;
            }
            savePortal(portal);
            return true;
        } else if(id == R.id.action_nearby_portals) {
            Intent intent=new Intent(this,PortalListActivity.class);
            intent.putExtra("lat",mLat);
            intent.putExtra("lon",mLon);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private void savePortal(final PortalInfo portal) {
        try {
            uploadPhoto(portal,portal.id, portal.urlPhoto);
            // TODO Thumbnail uploaden
        } catch(IOException ioe) {
            log.error("",ioe);
            // TODO error dem User anzeigen
        }
    }


    /** see https://firebase.google.com/docs/storage/android/upload-files
     *
     * @param localUrl
     * @return
     */
    private void uploadPhoto(final PortalInfo portalInfo,String id,String localUrl) throws FileNotFoundException {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        final StorageReference storageRef = storage.getReference();
        // Child references can also take paths
        // spaceRef now points to "images/space.jpg
        // imagesRef still points to "images"
        final String remoteUrl="portals/"+id+".jpg";
        StorageReference portalImageRef = storageRef.child(remoteUrl);
        InputStream stream = new FileInputStream(new File(localUrl));

        UploadTask uploadTask = portalImageRef.putStream(stream);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                // TODO show error message
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                //nur für UnitTest erlaubt: Uri downloadUrl = taskSnapshot.getDownloadUrl();
                if(portalInfo!=null) {
                    portalInfo.urlPhoto=remoteUrl;
                    savePortalInfo(portalInfo);
                }

                final String remoteUrl="portals-hires/"+portalInfo.id+".jpg";
                StorageReference portalImageRef = storageRef.child(remoteUrl);

                try {
                    // mind the scope of variable names !!!
                    InputStream stream = new FileInputStream(new File(mCurrentPhotoPath));
                    UploadTask uploadTask = portalImageRef.putStream(stream);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            //fail on hires will not bother us, will it?
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            log.info("successfully uploaded original hires image");
                            mIsHiresPictureUploaded=true;
                            checkForFinish();
                        }
                    });
                } catch (FileNotFoundException e) {
                    log.error("",e);
                }
            }
        });
    }

    public void savePortalInfo(final PortalInfo portal) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        final String id = portal.id;
        final DatabaseReference myRef = database.getReference("geogame/portals/" + id);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PortalInfo portalFromDb = dataSnapshot.getValue(PortalInfo.class);
                if (portalFromDb != null) {
                    log.info("portal exists " + id + " => " + portal.title);
                } else {
                    log.info("new portal  " + id + " => " + portal.title);
                    myRef.setValue(portal);
                }
                mIsPortalInfoUploaded=true;

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geogame/portals-geofire");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(portal.id, new GeoLocation(portal.lat,portal.lon));

                checkForFinish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("cancelled portal  " + id + " => " + portal.title + ":" + databaseError);
                Snackbar.make(mEditTitle, "Unable to send portal", Snackbar.LENGTH_LONG)
                        .setAction(android.R.string.cancel, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish(); // close activity
                            }
                        }).show();
            }
        });
    }


    private void checkForFinish() {
        if(mIsPictureUploaded && mIsHiresPictureUploaded && mIsPortalInfoUploaded) {
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mLocationListener!=null) {
            log.info("stop GPS updates");
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(mLocationListener);
            mLocationListener=null;
        }
    }

    protected void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ||  ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        } else {
            log.info("start GPS updates");
            LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            mLocationListener = new CurrentLocationListener();
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 25, 10, mLocationListener);
            //TODO Setting einbauen && DEBUG
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 2, mLocationListener);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    log.info("granted");
                    requestLocationUpdates();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    log.info("permission denied");
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private class CurrentLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {

            Toast.makeText(
                    getBaseContext(),
                    "Location: Lat: " + loc.getLatitude() + " Lng: "
                            + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            String longitude = "Longitude: " + loc.getLongitude();
            String latitude = "Latitude: " + loc.getLatitude();
            log.info("gps fix:" + latitude + "," + longitude);
            mLat=loc.getLatitude();
            mLon=loc.getLongitude();

            //LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //locationManager.removeUpdates(this);
            //mLocationListener=null;
            mIsPictureUploaded=true;
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

    private class ThumbnailTask extends AsyncTask<Void, Integer, String> {
        protected String doInBackground(Void... paths) {
            // nicht im main thread
            Bitmap thumbnail= null;
            try {
                thumbnail = Picasso.with(PortalEditorActivity.this)
                        .load(new File(mCurrentPhotoPath))
                        .rotate(exifToDegrees(mOrientation))
                        .resize(300, 300)
                        .centerInside()
                        .get();
                String thumbname=mCurrentPhotoPath+ THUMB_SUFFIX;
                PortalEditorActivity.saveFile(thumbnail,thumbname);
                return thumbname;
            } catch (IOException e) {
                log.error("",e);
                return null;
            }
        }

        protected void onProgressUpdate(Integer... progress) {
            log.info("progress="+progress[0]);
        }

        protected void onPostExecute(String result) {
            // UI Thread...
            log.info("task finished "+result);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bm=BitmapFactory.decodeFile(result, bmOptions);
            mImageButton.setImageBitmap(bm);
            mThumbPhotoPath=result;
            //showDialog("Downloaded " + result);
        }
    }

}
