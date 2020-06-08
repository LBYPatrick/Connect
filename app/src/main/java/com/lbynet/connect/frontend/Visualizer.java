package com.lbynet.connect.frontend;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.lbynet.connect.R;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.core.FileManager;
import com.lbynet.connect.backend.networking.FileRecvStreamer;
import com.lbynet.connect.backend.networking.FileStreamer;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Visualizer {

    static boolean isChannelCreated = false;

    public static void overrideSecurity() {
        try {
            StrictMode.class.getMethod("disableDeathOnFileUriExposure").invoke(null);
        } catch (Exception e) {
            SAL.print(e);
        }
    }

    public static void showReceiveProgress(Context context, String senderName, ArrayList<FileRecvStreamer> streams) {


        new Thread(() -> {

            double percentDone = 0;
            int numFiles = streams.size();
            double speedInKilobytesPerSec = 0;

            //Create channel (When needed)
            if (!isChannelCreated) {

                CharSequence name = context.getString(R.string.notif_recv_channel_name);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                String description = context.getString(R.string.notif_recv_channel_description);

                NotificationChannel channel = new NotificationChannel("connect_receive", name, importance);
                channel.setDescription(description);

                ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(channel);
                isChannelCreated = true;
            }


            String title = String.format(context.getString(R.string.notif_recv_working_title), numFiles);
            String subtitle = String.format(context.getString(R.string.notif_recv_working_subtitle), senderName);

            //Build notification and setup notification manager
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "connect_receive")
                    .setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_connect_logo_v3_round)
                    .setContentText(subtitle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setProgress(100, 0, false);

            int notificationId = Utils.getUniqueInt();

            //Progress update
            while (true) {

                double tempPercent = 0;
                long tempSpeed = 0;

                //Iterate through every task
                for (int i = 0; i < streams.size(); ++i) {

                    tempPercent += (streams.get(i).getProgress());
                    tempSpeed += streams.get(i).getAverageSpeedInKbps();

                }

                percentDone = tempPercent / numFiles;
                speedInKilobytesPerSec = tempSpeed;

                String info;

                if (speedInKilobytesPerSec < 1024) {
                    info = Utils.numToString(speedInKilobytesPerSec, 0) + " KB/s";
                }
                //MBps
                else {
                    info = Utils.numToString(speedInKilobytesPerSec / 1024, 2) + " MB/s";
                }

                if (percentDone >= 1) {
                    percentDone = 1;
                    info = context.getString(R.string.notif_recv_finished_percent);
                }

                builder.setSubText(info);
                builder.setProgress(100, (int) (percentDone * 100), false);

                manager.notify(notificationId, builder.build());

                if (percentDone >= 1) {
                    break;
                }

                Utils.sleepFor(200);
            }

            Utils.sleepFor(300);

            int goods = 0, bads = 0;
            ArrayList<String> fileNames = new ArrayList<>();

            for (FileRecvStreamer i : streams) {

                //Pause if the streamer is not actually done yet
                while(i.getNetStatus() == FileStreamer.NetStatus.WORKING) {
                    SAL.print("Blocking notification...");
                    Utils.sleepFor(300);
                }

                if (i.getNetStatus() != FileStreamer.NetStatus.SUCCESS) { bads += 1; }

                else { goods += 1; fileNames.add(i.getFilename()); }
            }

            SAL.print("Cancelling notification...");
            //Cancel the progress notification
            manager.cancel(notificationId);

            //Construct a new notification prompting the user to check the files out

            //Construct title and subtitle
            title = bads == 0 ?
                    (String.format(context.getString(R.string.notif_recv_finished_title), numFiles)) //All success
                    : (String.format(context.getString(R.string.notif_recv_finished_partial_success), goods, bads)); //Partial success

            subtitle = String.format(context.getString(R.string.notif_recv_finished_subtitle));

            //TODO: Construct intent

            Intent intent = new Intent("CHECK_RECEIVED_ITEMS");
            intent.putStringArrayListExtra("received_files",fileNames);

            PendingIntent pIntent = PendingIntent.getActivities(context,0,new Intent[]{intent},PendingIntent.FLAG_ONE_SHOT);

            //Construct notification
            builder = new NotificationCompat.Builder(context, "connect_receive")
                    .setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_connect_logo_v3_round)
                    .setContentText(subtitle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pIntent)
                    .setAutoCancel(true);

            manager.notify(Utils.getUniqueInt(), builder.build());

        }).start();
    }

    public static void updateFsnStatusOnLauncher(AppCompatActivity activity, boolean isGood) {
        CardView cv = activity.findViewById(R.id.cv_fsn_status);
        TextView tv = activity.findViewById(R.id.tv_fsn_status);

        activity.runOnUiThread(() -> {
            Utils.hideView(cv, false, 0);
            tv.setText(activity.getString(isGood ? R.string.launcher_fsn_good : R.string.launcher_fsn_bad));
            cv.setCardBackgroundColor(activity.getColor(isGood ? R.color.positive_75 : R.color.negative_75));
            Utils.showView(cv, 100);
        });
    }
}
