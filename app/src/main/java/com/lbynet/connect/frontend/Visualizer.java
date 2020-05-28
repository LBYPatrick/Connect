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

                CharSequence name = "Connect Receive";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                String description = "Notifications for showing file receive status";

                if(Utils.isChinese()) {
                    description = "显示文件接收状态的通知";
                    name = "Connect 传输状态展示";
                }

                NotificationChannel channel = new NotificationChannel("connect_receive", name, importance);
                channel.setDescription(description);

                ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(channel);
                isChannelCreated = true;
            }

            String title = "Receiving " + numFiles + "file" + (numFiles > 1 ? "s" : "") + "...";
            String subtitle = "From " + senderName;

            if(Utils.isChinese()) {
                title = "正在接收" + numFiles + "个文件...";
                subtitle = "来自" + senderName;
            }

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
                    info = Utils.isChinese()? "完成" : "Done";
                }

                builder.setSubText(info);
                builder.setProgress(100,(int)(percentDone * 100),false);

                manager.notify(notificationId,builder.build());

                if(percentDone >= 1) {
                    break;
                }

                Utils.sleepFor(200);
            }

            //Finish data transfer (cancel the notification)
            manager.cancel(notificationId);

            title = numFiles + "file" + (numFiles > 1? "s" : "") + "received";
            subtitle = "Tap to check";

            if(Utils.isChinese()) {
                title = numFiles + "个文件已接收";
                subtitle = "轻触以查看";
            }

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
