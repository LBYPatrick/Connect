package com.lbynet.connect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.networking.FileListener;
import com.lbynet.connect.backend.networking.Pairing;

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

        grantPermissions();
        setContentView(R.layout.launcher);

        pb = findViewById(R.id.pb_device_id);
        tvDeviceID = findViewById(R.id.tv_device_id);

        try {
            Pairing.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FileListener.start();
        new LoadDeviceID().start();


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
