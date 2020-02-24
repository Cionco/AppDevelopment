package de.fs.fintech.geogame;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import de.fs.fintech.geogame.data.PortalInfo;
import de.fs.fintech.geogame.parcelable.PortalInfoParcel;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

public class ImportPortalCsvActivity extends AppCompatBaseActivity {
    private static Logger log = LoggerFactory.getLogger(ImportPortalCsvActivity.class);

    private EditText mEditCsv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_portal_csv);

        mEditCsv = (EditText) findViewById(R.id.editText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.import_csv, menu);
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
            String csv = mEditCsv.getText().toString();
            try {
                StringBuffer result=new StringBuffer();
                PortalInfoParcel[] portals = parseCsv2PortalsSimple(csv,result);
                sendPortals2Firebase(portals);
                mEditCsv.setText(result.toString());

                csv=result.toString();
                PortalInfoParcel[] portals2 = parseCsv2Portals(csv,result);
                sendPortals2Firebase(portals2);
                mEditCsv.setText(result.toString());

            } catch (IOException e) {
                log.error("", e);
                // TODO message
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static class LineCsv {
        public String getPortal() {
            return Portal;
        }

        public void setPortal(String portal) {
            Portal = portal;
        }

        public Double getLatitude() {
            return Latitude;
        }

        public void setLatitude(Double latitude) {
            Latitude = latitude;
        }

        public Double getLongitude() {
            return Longitude;
        }

        public void setLongitude(Double longitude) {
            Longitude = longitude;
        }

        String Portal;
        Double Latitude,Longitude;
    }

    private PortalInfoParcel[] parseCsv2Portals(String csv,StringBuffer result) throws IOException {
        ICsvBeanReader beanReader = null;
        ArrayList<PortalInfoParcel> list = new ArrayList<PortalInfoParcel>();

        CsvPreference prefs=new CsvPreference.Builder('"', ',', "\n").build();
        try {
            beanReader = new CsvBeanReader(new StringReader(csv), prefs );

            // the header elements are used to map the values to the bean (names must match)
            final String[] header = beanReader.getHeader(true);
            String[] nameMapping = Arrays.copyOf(new String[]{"Portal","Latitude","Longitude"}, header.length);
            final CellProcessor[] processors = new CellProcessor[] {
                    new NotNull(), // firstName
                    new ParseDouble(),
                    new ParseDouble()
            };
            final CellProcessor[] newProcessors=new CellProcessor[header.length];
            System.arraycopy(processors, 0, newProcessors, 0, processors.length);

            LineCsv lineCsv;
            while( (lineCsv = beanReader.read(LineCsv.class, nameMapping, newProcessors)) != null ) {
                log.info(String.format("lineNo=%s, rowNo=%s, line=%s", beanReader.getLineNumber(),
                        beanReader.getRowNumber(), lineCsv));
                PortalInfoParcel portal=new PortalInfoParcel(lineCsv.getLongitude(), lineCsv.getLatitude(),lineCsv.getPortal());
                list.add(portal);
                result.append(portal.title+","+portal.lat+","+portal.lon+"\n");
            }


        }
        finally {
            if( beanReader != null ) {
                beanReader.close();
            }
        }

        return list.toArray(new PortalInfoParcel[list.size()]);
    }



    private PortalInfoParcel[] parseCsv2PortalsSimple(String csv,StringBuffer result) throws IOException {
        ArrayList<PortalInfoParcel> list = new ArrayList<PortalInfoParcel>();
        LineNumberReader reader = new LineNumberReader(new StringReader(csv));
        String line = reader.readLine();
        result.append(line); // header
        result.append('\n');

        while ((line = reader.readLine()) != null) {
            try {
                if(line.startsWith("#")) continue;

                String[] cols = line.split("[,]");
                String title = cols[0];
                double lat = Double.parseDouble(cols[1]);
                double lon = Double.parseDouble(cols[2]);
                if (title.length() < 4) continue;
                if (Double.isNaN(lat)) continue;
                if (Double.isNaN(lat)) continue;
                if (title.startsWith("\"")) title = title.substring(1, title.length() - 1);
                PortalInfoParcel portal = new PortalInfoParcel(lon, lat, title);
                list.add(portal);
                //result.append(portal.title+","+portal.lat+","+portal.lon+"\n");
            } catch (Throwable t) {
                log.error("unable to parse " + line + "|");
                result.append(line+"\n");
            }
        }
        return list.toArray(new PortalInfoParcel[list.size()]);
    }

    private void sendPortals2Firebase(PortalInfoParcel[] portals) {
        for (PortalInfoParcel portal : portals) {
            savePortalInfo(portal);
        }
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

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geogame/portals-geofire");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(portal.id, new GeoLocation(portal.lat, portal.lon));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("cancelled portal  " + id + " => " + portal.title + ":" + databaseError);
//                Snackbar.make(mEditTitle, "Unable to send portal", Snackbar.LENGTH_LONG)
//                        .setAction(android.R.string.cancel, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                finish(); // close activity
//                            }
//                        }).show();
            }
        });
    }

}
