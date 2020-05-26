package com.lbynet.connect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.networking.FileListener;
import com.lbynet.connect.backend.networking.Pairing;

import jp.wasabeef.blurry.Blurry;

public class LauncherActivity extends AppCompatActivity {

    ProgressBar pb;
    TextView tvDeviceID;

    void grantPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };

        for (String p : permissions) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{p}, 1);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SAL.print("OnCreate");

        setContentView(R.layout.launcher);

        DataPool.context = this;

        //Get permissions
        grantPermissions();

        ((ImageView)findViewById(R.id.iv_master_background)).setImageBitmap(Utils.getWallpaper(this));

        try {
            Pairing.start();
            FileListener.start();
        } catch (Exception e) {
            SAL.print(e);
        }

        LinearLayout main = findViewById(R.id.master);




        //Add buttons
        if((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
            main.addView(makeMainButton(getDrawable(R.drawable.ic_description_black_24dp), "Select from file..."));
            main.addView(makeMainButton(getDrawable(R.drawable.ic_info_black_24dp), "Info"));
            main.addView(makeMainButton(getDrawable(R.drawable.ic_settings_black_24dp), "Settings"));
        }
        else {
            main.addView(makeMainButton(getDrawable(R.drawable.ic_description_white_24dp), "Select from file..."));
            main.addView(makeMainButton(getDrawable(R.drawable.ic_info_white_24dp), "Info"));
            main.addView(makeMainButton(getDrawable(R.drawable.ic_settings_white_24dp), "Settings"));
        }

        new Thread( () -> {
            for (int i = 0; i < main.getChildCount(); ++i) {
                Utils.showView(this,main.getChildAt(i),100);
                Utils.sleepFor(100);
            }
        }).start();


    }

    public CardView makeMainButton(Drawable avatar, String text) {

        CardView r = (CardView) getLayoutInflater().inflate(R.layout.main_button,null);

        ((ImageView)r.findViewById(R.id.avatar)).setImageDrawable(avatar);
        ((TextView)r.findViewById(R.id.text)).setText(text);

        r.setVisibility(View.INVISIBLE);

        r.setOnClickListener(v1 -> onMainButtonClicked(v1));

        return r;

    }

    public void onMainButtonClicked(View v) {
        if(((TextView)v.findViewById(R.id.text)).getText() == "Settings") {
            startActivityForResult(new Intent(this,SettingsActivity.class),0);
        }
    }

    public void onSettingsButtonClicked(View view) {

        startActivityForResult(new Intent(this,SettingsActivity.class),0);
    }


}
