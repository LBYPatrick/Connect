package com.lbynet.connect.frontend;

import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.lbynet.connect.R;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.frames.FileInfo;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.networking.FileSender;
import com.lbynet.connect.backend.networking.Pairing;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TargetLoader extends ParallelTask {

    final public String TAG = this.getClass().getSimpleName();
    ArrayList<FrameLayout> deviceHolders = new ArrayList<>();
    ArrayList<Pairing.Device> devices = new ArrayList<>();
    ArrayList<Uri> uris_;

    FrameLayout rootView_;
    AppCompatActivity activity_;
    boolean isPaused_ = false;
    boolean isForceUpdateNeeded_ = false;

    public TargetLoader(FrameLayout rootView, AppCompatActivity activity) {
        rootView_ = rootView;
        activity_ = activity;
    }

    public void requestForceUpdate() {
        isForceUpdateNeeded_ = true;
    }

    public void setUris(ArrayList<Uri> uris) {
        uris_ = uris;
    }

    public void setPause(boolean value) {
        isPaused_ = value;
    }

    public void inflateList() throws Exception{

        AtomicBoolean isListReady = new AtomicBoolean(false);

        activity_.runOnUiThread( () -> {

            for (int i = 0; i < DataPool.NUM_TARGET_PLACEHOLDERS; ++i) {

                CardView v = (CardView) activity_.getLayoutInflater().inflate(R.layout.target_button, null);
                v.setOnClickListener(this::onTargetSelected);
                v.setVisibility(View.GONE);

                v.findViewById(R.id.btn_cancel).setVisibility(View.GONE);
                v.findViewById(R.id.tv_speed).setVisibility(View.GONE);

                ((LinearLayout) rootView_.findViewById(R.id.ll_select)).addView(v);
                deviceHolders.add(v);
            }

            isListReady.set(true);
        });

        while(!isListReady.get()) {
            Thread.sleep(50);
        }

    }

    boolean onTargetSelected(View v) {

        try {

            TextView uidView = v.findViewById(R.id.tv_uid),
                     speedView = v.findViewById(R.id.tv_speed);
            String targetUid = uidView.getText().toString();
            String targetIp = "";
            ProgressBar pb = v.findViewById(R.id.pb_status);
            String trimmedUid;

            if (!v.isClickable()) {
                return true;
            }

            if(targetUid.length() > 15) {
                trimmedUid = targetUid.substring(0,16) + "...";
            }
            else {
                trimmedUid = targetUid;
            }

            v.setClickable(false);
            Utils.showView(speedView,100);

            Utils.playTickVibration(activity_);

            targetIp = ((TextView) v.findViewById(R.id.hidden_ip)).getText().toString();

            if (targetIp.length() == 0) {
                SAL.print(SAL.MsgType.ERROR, "onTargetSelected", "Failed to find target IP in paired device list.");
                return true;
            }

            ArrayList<FileInfo> infos = new ArrayList<>();
            ArrayList<InputStream> streams = new ArrayList<>();

            for(Uri i : uris_) {
                infos.add(Utils.getFileInfo((Uri) i, activity_.getContentResolver()));
                streams.add(activity_.getContentResolver().openInputStream((Uri)i));
            }

            if (infos.size() == 0) {
                SAL.print(SAL.MsgType.ERROR, "onTargetSelected", "None of the files are valid");
                return true;
            }

            FileSender sender = new FileSender(targetIp, infos, streams);
            sender.start();

            //Progress Update
            new Thread(() -> {
                boolean isSuccessful = false;

                try {

                    while (true) {

                        FileSender.NetStatus status = sender.getNetStatus();

                        //In Progress
                        if (status == FileSender.NetStatus.TRANSFERRING || status == FileSender.NetStatus.DONE) {
                            //TODO: Do something to notify user
                            if (pb.getVisibility() == View.INVISIBLE) {
                                activity_.runOnUiThread(() -> {
                                    Utils.showView(pb, 200);
                                });
                            }

                            activity_.runOnUiThread(() -> {

                                String text = "";

                                double rawSpeed = sender.getSpeedInKilobytesPerSec();

                                if(rawSpeed < 1024) {
                                    text += Utils.numToString(rawSpeed,0) + " KB/s";
                                }
                                //MBps
                                else {
                                    text += Utils.numToString(rawSpeed / 1024,2) + " MB/s";
                                }

                                pb.setProgress((int) (sender.getPercentDone() * 100), true);
                                speedView.setText(text);
                            });

                            if (status == FileSender.NetStatus.DONE) {
                                SAL.print(TAG,"File transfer complete.");
                                Utils.playDoubleClickAnimation(activity_);
                                isSuccessful = true;
                                break;
                            }
                        }

                        //Failure
                        else if (status != FileSender.NetStatus.IDLE) {
                            //TODO: Do something to notify user
                            SAL.print(TAG,"File transfer failed.");
                            break;
                        }

                        //Don't update UI too fast
                        Thread.sleep(30);
                    }

                    //After file transfer, hide speed view

                    Utils.hideView(activity_,speedView,true,100);

                    activity_.runOnUiThread( () -> {
                        speedView.setText(activity_.getString(R.string.sendto_pending));
                    });

                    //Request a force update upon completion
                    isForceUpdateNeeded_ = true;

                } catch (Exception e) {
                    SAL.print(e);
                }


                final boolean isGood = isSuccessful;

                activity_.runOnUiThread( () -> {

                    Snackbar.make(activity_.findViewById(R.id.screen),
                                    String.format(
                                            isGood? activity_.getString(R.string.sendto_snackbar_success_text)
                                                    : activity_.getString(R.string.sendto_snackbar_fail_text),
                                            targetUid
                                    ), BaseTransientBottomBar.LENGTH_LONG)
                            .show();

                    v.setClickable(true);
                });

            }).start();

        } catch (Exception e) {
            SAL.print(e);
        }

        return true;
    }


    @Override
    public void run() throws Exception {

        LinearLayout waitingPrompt = rootView_.findViewById(R.id.ll_waiting_prompt);

        inflateList();

        while (true) {

            if(isPaused_) {
                Utils.sleepFor(500);
                continue;
            }

            Utils.sleepFor(200);

            boolean isChanged = Pairing.getFilteredDevices(devices,1000);

            int nTotalDevices = 0;

            //Accept force update request ONCE
            if(isForceUpdateNeeded_) {
                Pairing.requestForceUpdate();
                isForceUpdateNeeded_ = false;
            }
            else if(!isChanged) {
                continue;
            }

            for(Pairing.Device i : devices) {

                if(nTotalDevices < DataPool.NUM_TARGET_PLACEHOLDERS) {

                    FrameLayout parent = deviceHolders.get(nTotalDevices);
                    TextView uid = parent.findViewById(R.id.tv_uid);
                    TextView ip = parent.findViewById(R.id.hidden_ip);
                    ProgressBar pbar = parent.findViewById(R.id.pb_status);


                    activity_.runOnUiThread(() -> {
                        uid.setText(i.deviceName);
                        ip.setText(i.ip);
                    });

                    Utils.showView(activity_,parent,100);

                    nTotalDevices += 1;
                }

            }

            for(int i = nTotalDevices; i < DataPool.NUM_TARGET_PLACEHOLDERS; ++i) {

                FrameLayout parent = deviceHolders.get(i);
                TextView speedView = parent.findViewById(R.id.tv_speed);

                //Busy targets -- skip them
                if(speedView.getVisibility() == View.VISIBLE) {
                    nTotalDevices += 1;
                    continue;
                }

                else if(parent.getAlpha() != 0) {
                    Utils.hideView(activity_,parent,true,100);
                }
            }

            SAL.print("Active Devices: " + nTotalDevices);

            if(nTotalDevices == 0) {
                Utils.showView(activity_,waitingPrompt,200);
            }
            else {
                Utils.hideView(activity_,waitingPrompt,false,200);
            }
        }
    }
}