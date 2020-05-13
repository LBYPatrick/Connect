package com.lbynet.connect;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
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
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.networking.FileSender;
import com.lbynet.connect.backend.networking.Pairing;

import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

public class SendActivity extends AppCompatActivity {

    LinearLayout selectList;

    ArrayList<FrameLayout> deviceHolders = new ArrayList<>();
    ArrayList<Pairing.Device> devices = new ArrayList<>();
    LoadTargets targetLoader;

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

        SAL.print("onCreate");

        try {
            Pairing.start();
        } catch (Exception e) {
            //Shhhh
        }

        grantPermissions();
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

        if (targetLoader == null) {
            targetLoader = new LoadTargets(this);
            targetLoader.requestReset(true);
            targetLoader.start();
        } else {
            targetLoader.requestReset(true);
            targetLoader.setPause(false);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        SAL.print("onResume");
        if(targetLoader != null) {
            targetLoader.requestReset(true);
        }
    }

    boolean onTargetSelected(View v) {

        String targetUid = ((TextView) v.findViewById(R.id.tv_uid)).getText().toString();
        String targetIp = "";
        ProgressBar pb = v.findViewById(R.id.pb_status);

        if(v.getForeground().getAlpha() == 0) {
            return true;
        }

        v.getForeground().setAlpha(0);
        targetLoader.setPause(true);

        for (Pairing.Device i : devices) {
            if (targetUid.equals(i.uid)) {
                targetIp = i.ip;
                break;
            }
        }

        if (targetIp.length() == 0) {
            SAL.print(SAL.MsgType.ERROR, "onTargetSelected", "Failed to find target IP in paired device list.");
            return true;
        }


        ArrayList<String> filePaths = new ArrayList<>();

        String action = this.getIntent().getAction();

        //SEND_MULTIPLE
        if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {

            int i = 0;

            for (Parcelable n : this.getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM)) {

                String path = Utils.getPath(this, (Uri) n);

                if (path != null) {
                    filePaths.add(path);
                }
            }
        }
        //SEND
        else {
            Uri uri = this.getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            String path = Utils.getPath(this,uri);

            if(path != null) {
                filePaths.add(path);
            }
        }

        if(filePaths.size() == 0) {
            SAL.print(SAL.MsgType.ERROR,"onTargetSelected","None of the files are valid");
            return true;
        }


        FileSender sender = new FileSender(targetIp, filePaths.stream().toArray(String[]::new));
        sender.start();

        new Thread(() -> {
            try {
                while (true) {

                    FileSender.NetStatus status = sender.getNetStatus();

                    //In Progress
                    if (status == FileSender.NetStatus.TRANSFERRING || status == FileSender.NetStatus.DONE) {
                        //TODO: Do something to notify user
                        if(pb.getVisibility() == View.INVISIBLE) {
                            runOnUiThread( ()-> {
                                Utils.showView(pb,100);
                            });
                        }

                        runOnUiThread( ()-> {
                            pb.setProgress((int)(sender.getPercentDone() * 100),true);
                            SAL.print("Progress: " + sender.getPercentDone() * 100);
                        });

                        if(status == FileSender.NetStatus.DONE) {
                            SAL.print("File transfer complete.");
                            break;
                        }
                    }
                    //Failure
                    else if (status != FileSender.NetStatus.IDLE) {
                        //TODO: Do something to notify user
                        SAL.print("File transfer failed.");
                        Toast.makeText(this,"Failed to establish connection with target.",Toast.LENGTH_SHORT).show();
                        break;
                    }

                    Thread.sleep(50);
                }

                targetLoader.setPause(false);

            } catch (Exception e) {
                SAL.print(e);
            }
        }).start();

        //TODO: Finish this

        return true;
    }

    class LoadTargets extends ParallelTask {

        Context c;
        boolean isPaused = false;
        boolean needReset = false;

        public LoadTargets(Context context) {
            c = context;
        }


        public void setPause(boolean isPaused) {
            this.isPaused = isPaused;
        }

        public void requestReset(boolean value)  {
            needReset = value;
        }

        @Override
        public void run() throws Exception {

            ProgressBar pb = findViewById(R.id.pb_targets);

            while (true) {

                Thread.sleep(200);

                if(isPaused) continue;

                devices = Pairing.getPairedDevices();

                int nTotalDevices = 0;

                for (int i = 0; i < DataPool.NUM_TARGET_PLACEHOLDERS; ++i) {

                    FrameLayout parent = deviceHolders.get(i);

                    if(needReset) {
                        runOnUiThread( ()-> {
                            SAL.print("Resetting...");
                            parent.getForeground().setAlpha(255);
                            parent.findViewById(R.id.pb_status).setVisibility(View.INVISIBLE);
                        });
                    }

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

                needReset = false;

                final int temp = nTotalDevices;

                runOnUiThread(() -> {
                    if (temp == 0 && pb.getVisibility() == View.INVISIBLE) {
                        Utils.showView(pb, 500);
                    } else if (temp > 0 && pb.getVisibility() == View.VISIBLE) {
                        Utils.hideView(pb, false, 500);
                    }
                });



            }
        }
    }

}
