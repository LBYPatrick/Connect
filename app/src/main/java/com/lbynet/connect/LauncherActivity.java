package com.lbynet.connect;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Timer;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.core.SystemManager;
import com.lbynet.connect.backend.networking.FileReceiver;
import com.lbynet.connect.backend.networking.Pairing;
import com.lbynet.connect.frontend.Visualizer;

import jp.wasabeef.blurry.Blurry;

//TODO: Chinese Language Switching
//TODO: values-<qualifier>
//TODO: File Explorer layout & activity
//TODO: Permission activity
//TODO: FileRecvStreamer improvement: create directories, delete file if the connection is interrupted

public class LauncherActivity extends AppCompatActivity {

    Timer renderTimer = new Timer("Launcher Timer");

    void grantPermissions() { requestPermissions(DataPool.permissions, 1); }

    void configureDarkMode() {
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (isDarkMode) {
            setTheme(R.style.AppTheme_Dark);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        SAL.print("Got result");

        for (int i = 0; i < grantResults.length; ++i) {

            boolean isGranted = (grantResults[i] == android.content.pm.PackageManager.PERMISSION_GRANTED);
            SAL.print("Permission: " + permissions[i] + "\tGrant status: " + isGranted);

            if (!isGranted) {
                Utils.printToast(this, "Failed to obtain necessary permissions, please try again.", true);
                finish();
                return;
            }
        }

        continueWork();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        DataPool.activity = this;

        configureDarkMode();

        super.onCreate(savedInstanceState);
        SAL.print("OnCreate");

        if (!Utils.isPermissionGranted(this)) {
            grantPermissions();
        }
        else {
            continueWork();
        }

        //startActivity(new Intent("CHECK_RECEIVED_ITEMS"));
    }

    public void continueWork() {

        setTheme(R.style.AppTheme);
        setContentView(R.layout.launcher);
        FrameLayout main = findViewById(R.id.screen);
        //Configuration

        Utils.hideView(main, false, 0);
        Utils.showView(main, 200);


        runOnUiThread(() -> {
            Blurry.with(this).sampling(5).radius(30).from(Utils.getWallpaper(this)).into(findViewById(R.id.iv_master_background));
        });


        //((ImageView)findViewById(R.id.iv_master_background)).setImageBitmap(Utils.getWallpaper(this));

        try {

            //Pairing.start();

            FileReceiver.setOnReceiveListener((senderName, streams) -> {
                Visualizer.showReceiveProgress(this, senderName, streams);
            });

            //FileReceiver.start();
        } catch (Exception e) {
            SAL.print(e);
        }

        //Register Wi-Fi receivers for restarting services when need
        SystemManager.registerReceivers(this);

        new Thread(() -> {
            boolean isGood = false;

            while (true) {

                boolean temp = (DataPool.wifiStatus == DataPool.WifiStatus.CONNECTED);

                if (temp != isGood) {
                    isGood = temp;
                    Visualizer.updateFsnStatusOnLauncher(this, isGood);
                }

                Utils.sleepFor(500);
            }

        }).start();

        SAL.print("LaunchActivity took " + renderTimer.getElaspedTimeInMs() + "ms to render interface.");

    }

    @Override
    protected void onResume() {
        super.onResume();
        DataPool.isLauncherActivity = true;
    }

    public boolean onMainButtonClicked(View v) {
        if (((TextView) v.findViewById(R.id.text)).getText() == "Settings") {
            startActivityForResult(new Intent(this, SettingsActivity.class), 0);
        }
        return true;
    }

    public void onSettingsButtonClicked(View view) {

        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }

}
