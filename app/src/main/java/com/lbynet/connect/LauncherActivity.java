package com.lbynet.connect;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Timer;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.core.SystemManager;
import com.lbynet.connect.backend.frames.NetCallback;
import com.lbynet.connect.backend.networking.FileReceiver;
import com.lbynet.connect.backend.networking.Pairing;
import com.lbynet.connect.frontend.Visualizer;

import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

//TODO: Chinese Language Switching
//TODO: values-<qualifier>
//TODO: File Explorer layout & activity
//TODO: Permission activity
//TODO: FileRecvStreamer improvement: create directories, delete file if the connection is interrupted

public class LauncherActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();

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

        SAL.print(TAG,"Got result");

        for (int i = 0; i < grantResults.length; ++i) {

            boolean isGranted = (grantResults[i] == android.content.pm.PackageManager.PERMISSION_GRANTED);
            SAL.print(TAG,"Permission: " + permissions[i] + "\tGrant status: " + isGranted);

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


        Utils.hideView(findViewById(R.id.master),false,0);
        Utils.hideView(main, false, 0);


        ((ImageView)findViewById(R.id.iv_background_clear)).setImageBitmap(Utils.getWallpaper(this));
        Blurry.with(this).sampling(5).radius(30).from(Utils.getWallpaper(this)).into(findViewById(R.id.iv_background_blur));

        try {
            FileReceiver.setOnReceiveListener((senderName, streams) -> {
                Visualizer.showRecvNotification(this, senderName, streams);
            });
        } catch (Exception e) {
            SAL.print(e);
        }

        //For debugging purposes only

        /*
        new Thread( () -> {

            ArrayList<Pairing.Device> devices = new ArrayList<>();


            while(true) {
                boolean isChanged = Pairing.getFilteredDevices(devices);

                String msg = "";

                msg += "Is list changed: " + isChanged + "\n";
                msg += "Device list: \n";

                for(Pairing.Device i : devices) {
                    msg += "\tName: " + i.deviceName + " UID: " + i.uid + " IP: " + i.ip + "\n";
                }

                SAL.print(SAL.MsgType.VERBOSE,TAG,msg);

                Utils.sleepFor(500);
            }

        }).start();
        */

        //Register Wi-Fi receivers for restarting services when need
        SystemManager.registerReceivers(this);

        setPairingCallback();

        //Force update the status in case the view is re-created
        Visualizer.updateFsnStatusOnLauncher(this);

        SAL.print(TAG,"LaunchActivity took " + renderTimer.getElaspedTimeInMs() + "ms to render interface.");

        Utils.showView(main, 200);
        Utils.showView(findViewById(R.id.iv_background_blur),300);
        Utils.showView(findViewById(R.id.master),300);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SAL.print(SAL.MsgType.VERBOSE,TAG,"onResume");
        setPairingCallback();

        DataPool.isAppHiberated = false;

    }

    @Override
    protected void onPause() {
        super.onPause();

        DataPool.isAppHiberated = true;
    }

    public void setPairingCallback() {

        AppCompatActivity activity = this;
        Pairing.setStatusCallback(new NetCallback() {
            @Override
            public void onConnect() {
                SAL.print("Pairing Possible");
                new Thread( () -> {
                    //Wait for Wi-Fi stuff
                    //Utils.sleepFor(500);
                    Visualizer.updateFsnStatusOnLauncher(activity);
                }).start();
            }

            @Override
            public void onLost() {
                SAL.print("Pairing bad");

                new Thread( () -> {
                    //Wait for Wi-Fi stuff
                    //Utils.sleepFor(500);
                    Visualizer.updateFsnStatusOnLauncher(activity);
                }).start();
            }
        });
    }

    public void onTriButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_help:
                break;
            case R.id.btn_info:
                break;
            case R.id.btn_received:
                startActivity(new Intent("CHECK_RECEIVED_ITEMS"));
                break;
            default:
                break;
        }
    }
}
