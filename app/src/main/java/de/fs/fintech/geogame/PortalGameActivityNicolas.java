package de.fs.fintech.geogame;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalGameActivityNicolas extends AppCompatActivity implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {

    private static final Logger log = LoggerFactory.getLogger(PortalGameActivityNicolas.class);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_game_nicolas);

        ListView lst_testliste = (ListView) findViewById(R.id.lst_testliste);

        String[] values = new String[] { "Hallo", "ich", "hab", "noch", "keine", "ahnung", "was ich ", "hier tue!" };


        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                R.layout.lvi_faction, R.id.label, values);

        lst_testliste.setAdapter(arrayAdapter);
        lst_testliste.setOnItemClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        log.info(position + ", " + parent.getItemIdAtPosition(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        log.info(position + ", " + parent.getItemAtPosition(position));
    }
}
