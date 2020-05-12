package com.lbynet.connect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.DataPool;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Timer;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.networking.Pairing;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.wasabeef.blurry.Blurry;

public class SendActivity extends AppCompatActivity {

    LinearLayout selectList;

    ArrayList<FrameLayout> deviceHolders = new ArrayList<>();
    ArrayList<Pairing.Device> devices = new ArrayList<>();

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

        try {
            Pairing.start();
        } catch (Exception e) {
            //Shhhh
        }

        grantPermissions();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send);

        ImageView background = findViewById(R.id.iv_bkgnd);
        Blurry.with(this).radius(50).sampling(8).color(Color.argb(30, 0, 0, 0)).from(Utils.getWallpaper(this)).into(background);

        //Make Placeholders
        selectList = findViewById(R.id.ll_select);
        LinearLayout linearSelect = (LinearLayout) findViewById(R.id.ll_select);

        for (int i = 0; i < DataPool.NUM_TARGET_PLACEHOLDERS; ++i) {
            FrameLayout v = (FrameLayout) getLayoutInflater().inflate(R.layout.target_button, null);
            v.setOnClickListener((View view) -> onTargetSelected(view));
            v.setVisibility(View.GONE);
            selectList.addView(v);
            deviceHolders.add(v);
        }

        new LoadTargets(this).start();

    }

    boolean onTargetSelected(View v) {

        String targetUid = ((TextView) v.findViewById(R.id.tv_uid)).getText().toString();

        Toast.makeText(this, targetUid, Toast.LENGTH_SHORT).show();

        //TODO: Finish this

        return true;
    }

    class LoadTargets extends ParallelTask {

        Context c;

        public LoadTargets(Context context) {
            c = context;
        }


        @Override
        public void run() throws Exception {

            ProgressBar pb = findViewById(R.id.pb_targets);

            while (true) {

                devices = Pairing.getPairedDevices();

                int nTotalDevices = 0;

                for (int i = 0; i < DataPool.NUM_TARGET_PLACEHOLDERS; ++i) {

                    FrameLayout parent = deviceHolders.get(i);

                    //If the object is fresh enough
                    if (i < devices.size() && devices.get(i).getFreshness() < 500) {

                        nTotalDevices++;

                        TextView tv = parent.findViewById(R.id.tv_uid);

                        String text = devices.get(i).uid;
                        runOnUiThread(() -> {
                            tv.setText(text);

                            if (parent.getVisibility() == View.GONE) {
                                Utils.showView(parent, 200);
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (parent.getVisibility() == View.VISIBLE) {
                                Utils.hideView(parent, true, 200);
                            }
                        });
                    }
                }

                final int temp = nTotalDevices;

                runOnUiThread(() -> {
                    if (temp == 0 && pb.getVisibility() == View.INVISIBLE) {
                        Utils.showView(pb, 500);
                    } else if (temp > 0 && pb.getVisibility() == View.VISIBLE) {
                        Utils.hideView(pb, false, 500);
                    }
                });

                Thread.sleep(200);

            }
        }
    }

}
