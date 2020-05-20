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
import androidx.cardview.widget.CardView;

import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.frames.FileInfo;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.networking.FileListener;
import com.lbynet.connect.backend.networking.FileSender;
import com.lbynet.connect.backend.networking.Pairing;

import java.io.InputStream;
import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

public class SendActivity extends AppCompatActivity {

    LinearLayout selectList;

    ArrayList<FrameLayout> deviceHolders = new ArrayList<>();
    ArrayList<Pairing.Device> devices = new ArrayList<>();
    LoadTargets targetLoader;
    boolean isBkgrBlurred = false;

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
            FileListener.start();
        } catch (Exception e) {
            SAL.print(e);
        }

        grantPermissions();
        setContentView(R.layout.send);

        ImageView background = findViewById(R.id.iv_bkgnd);


        if (!isBkgrBlurred) {
            Blurry.with(this).radius(20).sampling(5).color(Color.argb(30, 0, 0, 0)).animate(200).from(Utils.getWallpaper(this)).into(background);
            isBkgrBlurred = true;
        }

        //Make Placeholders
        selectList = findViewById(R.id.ll_select);
        LinearLayout linearSelect = (LinearLayout) findViewById(R.id.ll_select);

        for (int i = 0; i < DataPool.NUM_TARGET_PLACEHOLDERS; ++i) {
            CardView v = (CardView) getLayoutInflater().inflate(R.layout.target_button, null);
            v.setOnClickListener(v1 -> onTargetSelected(v1));
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
        if (targetLoader != null) {
            targetLoader.requestReset(true);
        }
    }

    void onTargetSelected(View v) {

        try {

            TextView uidView = v.findViewById(R.id.tv_uid);
            String targetUid = uidView.getText().toString();
            String targetIp = "";
            ProgressBar pb = v.findViewById(R.id.pb_status);
            String trimmedUid;

            if(targetUid.length() > 15) {
                trimmedUid = targetUid.substring(0,16) + "...";
            }
            else {
                trimmedUid = targetUid;
            }


            if (!v.isClickable()) {
                SAL.print("Button is busy");
                return;
            }

            v.setClickable(false);

            for (Pairing.Device i : devices) {
                if (targetUid.equals(i.uid)) {
                    targetIp = i.ip;
                    break;
                }
            }

            if (targetIp.length() == 0) {
                SAL.print(SAL.MsgType.ERROR, "onTargetSelected", "Failed to find target IP in paired device list.");
                return;
            }


            ArrayList<FileInfo> infos = new ArrayList<>();
            ArrayList<InputStream> streams = new ArrayList<>();
            String action = this.getIntent().getAction();

            //SEND_MULTIPLE
            if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {

                int i = 0;

                for (Parcelable n : this.getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM)) {

                    SAL.printUri((Uri) n, getContentResolver());

                    infos.add(Utils.getFileInfo((Uri) n, getContentResolver()));
                    streams.add(getContentResolver().openInputStream((Uri) n));
                }
            }
            //SEND
            else {
                Uri uri = this.getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

                SAL.printUri(uri, getContentResolver());

                infos.add(Utils.getFileInfo(uri, getContentResolver()));
                streams.add(getContentResolver().openInputStream(uri));

            }

            if (infos.size() == 0) {
                SAL.print(SAL.MsgType.ERROR, "onTargetSelected", "None of the files are valid");
                return;
            }


            FileSender sender = new FileSender(targetIp, infos, streams);
            sender.start();

            new Thread(() -> {
                try {
                    while (true) {

                        FileSender.NetStatus status = sender.getNetStatus();

                        //In Progress
                        if (status == FileSender.NetStatus.TRANSFERRING || status == FileSender.NetStatus.DONE) {
                            //TODO: Do something to notify user
                            if (pb.getVisibility() == View.INVISIBLE) {
                                runOnUiThread(() -> {
                                    Utils.showView(pb, 200);
                                });
                            }

                            runOnUiThread(() -> {

                                String text = trimmedUid + " | ";

                                double rawSpeed = sender.getSpeedInKilobytesPerSec();

                                if(rawSpeed < 1024) {
                                    text += Utils.numToString(rawSpeed,0) + " KB/s";
                                }
                                //MBps
                                else {
                                    text += Utils.numToString(rawSpeed / 1024,2) + " MB/s";
                                }

                                pb.setProgress((int) (sender.getPercentDone() * 100), true);
                                uidView.setText(text);

                                if(sender.getPercentDone() == 1) {
                                    uidView.setText(trimmedUid + " | " + " Done");
                                }
                            });

                            if (status == FileSender.NetStatus.DONE) {
                                SAL.print("File transfer complete.");
                                break;
                            }
                        }
                        //Failure
                        else if (status != FileSender.NetStatus.IDLE) {
                            //TODO: Do something to notify user
                            SAL.print("File transfer failed.");
                            Toast.makeText(this, "Failed to establish connection with target.", Toast.LENGTH_SHORT).show();
                            break;
                        }

                        //Don't update UI too fast
                        Thread.sleep(100);
                    }

                } catch (Exception e) {
                    SAL.print(e);
                }
            }).start();

        } catch (Exception e) {
            SAL.print(e);
        }
        //TODO: Finish this
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

        public void requestReset(boolean value) {
            needReset = value;
        }

        @Override
        public void run() throws Exception {

            ProgressBar pb = findViewById(R.id.pb_targets);

            while (true) {

                Thread.sleep(200);

                if (isPaused) continue;

                devices = Pairing.getPairedDevices();

                int nTotalDevices = 0;

                for (int i = 0; i < DataPool.NUM_TARGET_PLACEHOLDERS; ++i) {

                    FrameLayout parent = deviceHolders.get(i);

                    if (needReset) {
                        runOnUiThread(() -> {
                            SAL.print("Resetting...");
                            parent.setClickable(true);
                            parent.findViewById(R.id.pb_status).setVisibility(View.INVISIBLE);
                        });
                    }

                    //If the object is fresh enough
                    if (i < devices.size() && devices.get(i).getFreshness() < 2500) {

                        nTotalDevices++;

                        TextView tv = parent.findViewById(R.id.tv_uid);

                        if(!needReset && tv.getText().toString().contains("|")) {
                            continue;
                        }

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
