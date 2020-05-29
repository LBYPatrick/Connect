package com.lbynet.connect.frontend;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.lbynet.connect.R;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.networking.FileRecvStreamer;

import java.util.ArrayList;

public class Visualizer {

    static boolean isChannelCreated = false;

    public static void showReceiveProgress(Context context, String senderName, ArrayList<FileRecvStreamer> streams) {


        new Thread(()->{


            double percentDone = 0;
            int numFiles = streams.size();
            double speedInKilobytesPerSec = 0;

            //Create channel (When needed)
            if(!isChannelCreated) {

                CharSequence name = context.getString(R.string.notif_recv_channel_name);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                String description = context.getString(R.string.notif_recv_channel_description);

                NotificationChannel channel = new NotificationChannel("connect_receive", name, importance);
                channel.setDescription(description);

                ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(channel);
                isChannelCreated = true;
            }


            String title = String.format(context.getString(R.string.notif_recv_working_title),numFiles);
            String subtitle = String.format(context.getString(R.string.notif_recv_working_subtitle),senderName);

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
            while(true) {

                double tempPercent = 0;
                long tempSpeed = 0;

                //Iterate through every task
                for(int i = 0; i < streams.size(); ++i) {

                    tempPercent += (streams.get(i).getProgress());
                    tempSpeed += streams.get(i).getAverageSpeedInKbps();

                }

                percentDone = tempPercent / numFiles;
                speedInKilobytesPerSec = tempSpeed;

                String info;

                if(speedInKilobytesPerSec < 1024) {
                    info = Utils.numToString(speedInKilobytesPerSec,0) + " KB/s";
                }
                //MBps
                else {
                    info = Utils.numToString(speedInKilobytesPerSec / 1024,2) + " MB/s";
                }

                if(percentDone >= 1) {
                    percentDone = 1;
                    info = context.getString(R.string.notif_recv_finished_percent);
                }

                builder.setSubText(info);
                builder.setProgress(100,(int)(percentDone * 100),false);

                manager.notify(notificationId,builder.build());

                if(percentDone >= 1) {
                    break;
                }

                Utils.sleepFor(200);
            }

            Utils.sleepFor(300);
            //Finish data transfer (cancel the notification)
            manager.cancel(notificationId);

            title = String.format(context.getString(R.string.notif_recv_finished_title),numFiles);
            subtitle = String.format(context.getString(R.string.notif_recv_finished_subtitle));

            //Construct a new notification prompting the user to check the files out
            builder = new NotificationCompat.Builder(context, "connect_receive")
                    .setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_connect_logo_v3_round)
                    .setContentText(subtitle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            manager.notify(Utils.getUniqueInt(),builder.build());

        }).start();
    }

}
