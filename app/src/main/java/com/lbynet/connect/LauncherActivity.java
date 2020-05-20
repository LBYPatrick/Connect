package com.lbynet.connect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.networking.FileListener;
import com.lbynet.connect.backend.networking.Pairing;

import jp.wasabeef.blurry.Blurry;

public class LauncherActivity extends AppCompatActivity {

    ProgressBar pb;
    TextView tvDeviceID;
    ImageView background;

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

        grantPermissions();
        setContentView(R.layout.launcher);

        pb = findViewById(R.id.pb_device_id);
        tvDeviceID = findViewById(R.id.tv_device_id);

        background = findViewById(R.id.iv_bkgnd);

        background.setImageBitmap(Utils.getWallpaper(this));

        try {
            Pairing.start();
        } catch (Exception e) {
            SAL.print(e);
        }

        FileListener.start();
        new LoadDeviceID().start();

        Blurry.with(this).async().radius(30).sampling(5).color(Color.argb(30, 0, 0, 0)).from(Utils.getWallpaper(this)).into(background);
        //startActivity(new Intent(this,SettingsActivity.class));

    }

    public void onSettingsButtonClicked(View view) {
        startActivity(new Intent(this,SettingsActivity.class));
    }

    private class LoadDeviceID extends ParallelTask {

        String id;

        @Override
        public void preRun() throws Exception {
            pb.setVisibility(View.VISIBLE);
            tvDeviceID.setVisibility(View.INVISIBLE);
        }

        @Override
        public void run() {
            id = Pairing.getSelfUid();

            runOnUiThread( () -> {
                if(id == null) {
                    tvDeviceID.setText("Failed to obtain device ID, please blame @lbypatrick.");
                }
                else {
                    tvDeviceID.setText(id);
                }

                pb.setVisibility(View.INVISIBLE);
                tvDeviceID.setVisibility(View.VISIBLE);

            });
        }
    }

}
