package com.lbynet.connect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.core.SystemManager;
import com.lbynet.connect.backend.networking.FileReceiver;
import com.lbynet.connect.backend.networking.Pairing;
import com.lbynet.connect.frontend.TargetLoader;
import com.lbynet.connect.frontend.Visualizer;

import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

public class SendActivity extends AppCompatActivity {


    TargetLoader targetLoader;

    void grantPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
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
        setTheme(isDarkMode? R.style.AppTheme_Dark : R.style.AppTheme_Light);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        SAL.print("Intent info: \n"
                + "\tAction: "  + getIntent().getAction() + "\n"
                + "\tData: " + getIntent().getData() + "\n"
                + "\tHost: " + getReferrer().getHost() + "\n"
                + "\tAuthority: " + getReferrer().getAuthority() + "\n");

        /*
        //If user attempts to crash the app by 套娃
        if(getReferrer().getAuthority().equals(getApplicationContext().getPackageName())) {
            Utils.printToast(this,getString(R.string.warning_taowa), true);
            finish();
        }
         */

        configureDarkMode();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.send);

        configureDarkMode();

        Utils.hideView(findViewById(R.id.screen),false,0);

        //Blur background
        Blurry.with(this).sampling(10).radius(10).from(Utils.getWallpaper(this)).into(findViewById(R.id.iv_background));

        //Show everything
        Utils.showView(findViewById(R.id.screen),200);


        DataPool.activity = this;

        SAL.print("onCreate");
        try {
            Pairing.start();
            FileReceiver.start();

            //Register listener for notification
            FileReceiver.setOnReceiveListener((senderName,streams) -> {
                Visualizer.showReceiveProgress(this,senderName,streams);
            });

        } catch (Exception e) {
            SAL.print(e);
        }

        //Register Wi-Fi receivers for restarting services when need
        SystemManager.registerReceivers(this);

        //Configure URI
        String action = this.getIntent().getAction();
        ArrayList<Uri> uris = new ArrayList<>();

        if(action == Intent.ACTION_SEND_MULTIPLE) {
            for (Parcelable n : this.getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM)) {
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

        String titleText = String.format(getString(R.string.sendto_main_title),uris.size());
        String subtitle = getString(R.string.sendto_subtitle);

        //Set target select prompt
        ((TextView)findViewById(R.id.tv_send_main_title)).setText(titleText);
        ((TextView)findViewById(R.id.tv_send_subtitle)).setText(subtitle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SAL.print("onResume");

        targetLoader.setPause(false);
    }

    @Override
    protected void onPause() {

        super.onPause();
        SAL.print("onPause");

        targetLoader.setPause(true);
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