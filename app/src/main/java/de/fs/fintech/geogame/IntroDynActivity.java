package de.fs.fintech.geogame;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class IntroDynActivity extends AppCompatBaseActivity implements View.OnClickListener {
    private static final Logger log = LoggerFactory.getLogger(IntroDynActivity.class);
    private String[] lbls;
    private String[] urls;
    private int[] extraIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        initButtonsArray();

    }



    private void initButtonsArray() {
        int[] ids={
                R.id.buttonIntro1,
                R.id.buttonIntro2,
                R.id.buttonIntro3
        };
        for (int id: ids) {
            Button btn = (Button) findViewById(id);
            btn.setOnClickListener(this);
        }

        lbls = getResources().getStringArray(R.array.lbl_intros);
        urls = getResources().getStringArray(R.array.lbl_urls);
        extraIds= new int[lbls.length];

        // dynamically add Buttons to Container
        LinearLayout extra= (LinearLayout) findViewById(R.id.layoutExtraIntros);
        for(int i=0;i<lbls.length;i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.weight=1;
            Button btn = new Button(this);
            btn.setId(i);

            final int id_ = btn.getId();
            log.info("#"+i+"="+id_);
            extraIds[i]=id_;

            btn.setText(lbls[i]);
            //btn.setBackgroundColor(Color.rgb(70, 80, 90));

            extra.addView(btn, params);
            btn = ((Button) findViewById(id_)); // unnötig
            btn.setOnClickListener(this);
        }
    }


    @Override
    public void onClick(View v) {
        String url=null;
        switch (v.getId()) {

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

            default:
                for(int i=0;i<extraIds.length;i++) {
                    if(v.getId()==extraIds[i]) url=urls[i];
                }
                break;
        }
        if(url==null) {
            log.error("no url for "+v.getId());
            return;
        } else {
            startYoutubeActivity(url);
        }
    }

    protected void startYoutubeActivity(String url) {
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
