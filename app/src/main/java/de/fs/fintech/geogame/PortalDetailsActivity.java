package de.fs.fintech.geogame;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Text;

import java.io.File;

import de.fs.fintech.geogame.data.PortalInfo;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

public class PortalDetailsActivity extends AppCompatBaseActivity {
    private static Logger log = LoggerFactory.getLogger(PortalDetailsActivity.class);

    public static final String EXTRA_PORTAL_PARCEL = "portal";
    private TextView mTextDescription;
    private File portalThumbFile;
    private ImageButton mImageButton;
    private PortalInfoParcel portal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_details);

        Intent callingIntent=getIntent();
        portal=callingIntent.getParcelableExtra(EXTRA_PORTAL_PARCEL);
        setTitle(portal.title);
        mTextDescription= (TextView) findViewById(R.id.textDescription);
        mTextDescription.setText(portal.description);

        mImageButton=(ImageButton) findViewById(R.id.imageButton);
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(true) return; // Funktion deaktiviert b.a.w.
                Intent intent=new Intent(PortalDetailsActivity.this,PortalEditorActivity.class);
                intent.putExtra(PortalDetailsActivity.EXTRA_PORTAL_PARCEL,portal);
                startActivity(intent);
                // TODO PortalEditorActivity aus Parcel initialisieren und "UPDATE" senden
            }
        });

        if(portal.urlPhoto!=null) {
        /* ohne WRITE_EXTERNAL_STORAGE permissions
             * für große Files, die jeder sehen darf, ggf. auch auf seinen PC kopieren kann
             * == file dumped to /storage/emulated/0/Android/data/de.fs.fintech.geogame/files/ext_data/dump.json
             */
            portalThumbFile = new File(((Context) this).getExternalFilesDir("portal_thumbs"), portal.id + ".jpg");
            if (!portalThumbFile.exists()) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                // Create a storage reference from our app
                StorageReference storageRef = storage.getReference();
                // Create a reference with an initial file path and name
                StorageReference pathReference = storageRef.child(portal.urlPhoto);
                pathReference.getFile(portalThumbFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Local temp file has been created
                        log.info("file downloaded to cache");
                       setImageButton();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        log.error("unable to download to cache. Caught:",exception);
                    }
                });
            } else { // file exists already
                setImageButton();
            }
        }
    }

    private void setImageButton() {
        String absolutePath = portalThumbFile.getAbsolutePath();
        log.info("setting ImageButton from cache "+ absolutePath);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bm=BitmapFactory.decodeFile(absolutePath, bmOptions);
        mImageButton.setImageBitmap(bm);
    }
}
