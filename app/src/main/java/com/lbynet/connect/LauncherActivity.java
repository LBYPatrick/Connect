package com.lbynet.connect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
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

    void configureDarkMode() {

        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        //Do things here if you need
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SAL.print("OnCreate");
        setContentView(R.layout.launcher);

        DataPool.activity = this;


        //Splash Screen
        View splashScreen = getLayoutInflater().inflate(R.layout.splash_screen,null);
        ((FrameLayout)findViewById(R.id.screen)).addView(splashScreen);

        //Configuration
        grantPermissions();
        configureDarkMode();

        new Thread( ()-> {
            Utils.sleepFor(500);
            Utils.hideView(splashScreen,true,200);
        }).start();

        try {
            Pairing.start();
            FileListener.start();
        } catch (Exception e) {
            SAL.print(e);
        }

        LinearLayout main = findViewById(R.id.master);

        Blurry.with(this).sampling(3).radius(60).from(Utils.getWallpaper(this)).into(findViewById(R.id.iv_master_background));

    }

    public CardView makeMainButton(Drawable avatar, String text) {

        CardView r = (CardView) getLayoutInflater().inflate(R.layout.main_button,null);

        ((ImageView)r.findViewById(R.id.avatar)).setImageDrawable(avatar);
        ((TextView)r.findViewById(R.id.text)).setText(text);

        r.setOnClickListener(this::onMainButtonClicked);

        return r;

    }

    public boolean onMainButtonClicked(View v) {
        if(((TextView)v.findViewById(R.id.text)).getText() == "Settings") {
            startActivityForResult(new Intent(this,SettingsActivity.class),0);
        }
        return true;
    }

    public void onSettingsButtonClicked(View view) {

        startActivityForResult(new Intent(this,SettingsActivity.class),0);
    }


}
