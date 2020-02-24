package de.fs.fintech.geogame;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class IntroActivity extends AppCompatBaseActivity implements View.OnClickListener {
    private static final Logger log = LoggerFactory.getLogger(IntroActivity.class);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        initButtonsExplicit();

    }

    private void initButtonsExplicit() {
        Button btn1 = (Button) findViewById(R.id.buttonIntro1);
        Button btn2 = (Button) findViewById(R.id.buttonIntro2);
        Button btn3 = (Button) findViewById(R.id.buttonIntro3);
        Button btnQ = (Button) findViewById(R.id.buttonQuit);
        btn1.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn1.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        String url;
        switch (v.getId()) {
            default:
            case R.id.buttonIntro1:
                url = getString(R.string.url_intro1);
                break;
            case R.id.buttonIntro2:
                url = getString(R.string.url_intro2);
                break;
            case R.id.buttonIntro3:
                url = getString(R.string.url_intro3);
                break;
            case R.id.buttonQuit:
                finish();
                return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // intent.setComponent(new ComponentName("com.google.android.youtube", "com.google.android.youtube.PlayerActivity"));
            intent.setPackage("com.google.android.youtube");
            Context context = this;
            PackageManager manager = context.getPackageManager();
            List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
            if(log.isInfoEnabled()) {
                for (ResolveInfo info : infos) {
                    log.info(url + " url info:" + info.activityInfo.packageName + "/" + info.activityInfo.targetActivity);
                }
           }

            startActivity(intent);
        } catch(ActivityNotFoundException ex) {
            log.warn("No YouTube App installed");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }
}
