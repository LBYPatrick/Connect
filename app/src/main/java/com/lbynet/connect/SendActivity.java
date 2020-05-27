package com.lbynet.connect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.networking.FileReceiver;
import com.lbynet.connect.backend.networking.Pairing;
import com.lbynet.connect.frontend.TargetLoader;

import java.util.ArrayList;

import com.lbynet.connect.backend.Timer;

import jp.wasabeef.blurry.Blurry;

public class SendActivity extends AppCompatActivity {


    TargetLoader targetLoader;
    Timer elapsedTime = new Timer("Splash Screen Timer");

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

        setContentView(R.layout.send);

        grantPermissions();
        configureDarkMode();


        DataPool.activity = this;

        SAL.print("onCreate");

        //Splash Screen
        View splashScreen = getLayoutInflater().inflate(R.layout.splash_screen,null);
        ((FrameLayout)findViewById(R.id.master_overlay)).addView(splashScreen);

        Utils.showView(splashScreen,0);

        elapsedTime.start();

        try {
            Pairing.start();
            FileReceiver.start();
        } catch (Exception e) {
            SAL.print(e);
        }


        //Configure URI
        String action = this.getIntent().getAction();
        ArrayList<Uri> uris = new ArrayList<>();

        if(action == Intent.ACTION_SEND_MULTIPLE) {
            for (Parcelable n : this.getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM)) {

                //SAL.printUri((Uri) n, getContentResolver());

                uris.add((Uri)n);
            }
        }
        else if(action == Intent.ACTION_SEND) {
            Uri uri = this.getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

            uris.add(uri);
        }

        if(targetLoader == null) {
            targetLoader = new TargetLoader(findViewById(R.id.fl_select),this);
            targetLoader.setUris(uris);
            targetLoader.start();
        }
        else {
            targetLoader.setUris(uris);
        }

        String titleText;
        String subtitle;

        if(Utils.isChinese()) {
            titleText = "发送" + uris.size() + "个文件至...";
            subtitle = "轻触设备名称以发送文件";
        }
        //See, English is more complex
        else {
            titleText = "Send " + uris.size() + " " + (uris.size() > 1 ? "files" : "file") + " to...";
            subtitle = "Tap device name(s) to send";
        }


        //Set Target Select Prompt
        ((TextView)findViewById(R.id.tv_send_main_title)).setText(titleText);
        ((TextView)findViewById(R.id.tv_send_subtitle)).setText(subtitle);

        //Blur background
        Blurry.with(this).sampling(3).radius(60).from(Utils.getWallpaper(this)).into(findViewById(R.id.iv_bkgnd_blur));
        Utils.showView(this,findViewById(R.id.iv_bkgnd_blur),0);


        //Hide Splash screen
        if(elapsedTime.getElaspedTimeInMs() > 500) {
            Utils.hideView(this,splashScreen,true,200);
        }
        else {

            final long remainingTime = 500 - elapsedTime.getElaspedTimeInMs();
            new Thread(() -> {
                Utils.sleepFor(remainingTime);
                Utils.hideView(this,splashScreen,true,200);
            }).start();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        SAL.print("onResume");
    }

    public void onSettingsButtonClicked(View view) {

    }

    public boolean onFEClicked(View view) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(Uri.parse(Utils.getOutputPath()),"*/*");

        startActivity(intent);
        return true;
    }

}
