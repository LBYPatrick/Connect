package com.lbynet.connect.backend.networking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lbynet.connect.backend.frames.FileInfo;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class FileSender extends ParallelTask {

    final public static String TAG = FileSender.class.getSimpleName();
    private String ip_ = "";
    private ArrayList<FileInfo> fileInfos_;
    private ArrayList<InputStream> fileStreams_;
    private NetStatus netStatus = NetStatus.IDLE;
    private ArrayList<FileSendStreamer> queue = new ArrayList<>();
    private double percentDone = 0;
    private long speedInKilobytesPerSec = 0;
    private int numFiles = 0;

    public FileSender(String ip, ArrayList<FileInfo> fileInfos, ArrayList<InputStream> fileStreams) {
        ip_ = ip;
        fileInfos_ = fileInfos;
        fileStreams_ = fileStreams;
    }

    public enum NetStatus {
        IDLE,
        INIT_FAIL,
        TRANSFERRING,
        HANDSHAKE_REJECTED,
        HANDSHAKE_TIMEOUT,
        INTERRUPTED,
        DONE
    }


    public void run() {

        if (ip_.length() == 0 || fileInfos_.size() == 0) {

            SAL.print(SAL.MsgType.ERROR, "FileSender", "Failed to initialize FileSender because of malformed parameter");
            netStatus = NetStatus.INIT_FAIL;
            return;
        }

        try {

            //Build file info in JSON
            JsonObject json = new JsonObject();
            JsonArray files = new JsonArray(),
                      sizes = new JsonArray();

            json.addProperty("device", SAL.getDeviceName());
            json.addProperty("method", "plain");


            for (FileInfo file : fileInfos_) {
                files.add(file.name);
                sizes.add(file.size);
            }

            json.add("files",files);
            json.add("sizes",sizes);

            SAL.print(SAL.MsgType.VERBOSE,TAG,json.toString());

            //Send file info to target
            Socket socket = new Socket(ip_, Utils.getTargetPort(ip_));

            IO.sendDataToRemote(socket, json.toString());

            //Retrive file transferring port from target
            String receivedData = IO.getDataFromRemote(socket, 5000);

            if (receivedData != null) {
                SAL.print(SAL.MsgType.VERBOSE, TAG, "File Transfer Ports: " + receivedData);
            }
            else {
                netStatus = NetStatus.HANDSHAKE_TIMEOUT;
                return;
            }

            netStatus = NetStatus.TRANSFERRING;

            socket.close();

            //Parse Response (Which contains an array of ports)
            JsonArray ports = JsonParser.parseString(receivedData).getAsJsonArray();

            long totalSize = 0;

            //Start file receive streamers
            for (int i = 0; i < ports.size(); ++i) {

                FileSendStreamer fss = new FileSendStreamer(fileStreams_.get(i), fileInfos_.get(i).size, ip_, ports.get(i).getAsInt());
                totalSize += fileInfos_.get(i).size;
                queue.add(fss);
                fss.start();
            }

            //numFiles = queue.size();

            //Wait till everything is done
            while (true) {

                double finishedBytes = 0;
                long tempSpeed = 0;

                //Iterate through every task
                for (int i = 0; i < queue.size(); ++i) {

                    FileSendStreamer task = queue.get(i);

                    finishedBytes += task.getNumBytesSent();
                    tempSpeed += task.getAverageSpeedInKbps();

                }

                percentDone = finishedBytes / totalSize;
                speedInKilobytesPerSec = tempSpeed;

                if (percentDone >= 1) {
                    break;
                }

                Utils.sleepFor(10);
            }

            netStatus = NetStatus.DONE;

        } catch (Exception e) {
            SAL.print(e);

            percentDone = 1;

            if (e instanceof SecurityException) {
                netStatus = NetStatus.INIT_FAIL;
            }
            else if (e instanceof UnknownHostException || e instanceof IOException) {
                netStatus = NetStatus.HANDSHAKE_TIMEOUT;
            }
            else {
                netStatus = NetStatus.INTERRUPTED;
            }

            SAL.print(TAG,"Failed to transfer files, reason: " + netStatus.toString());

            return;
        }
        return;
    }

    public double getPercentDone() {

        return percentDone;
    }

    public long getSpeedInKilobytesPerSec() {

        return speedInKilobytesPerSec;
    }

    public int[] getNumGoodsAndBads() {

        int good = 0, bad = 0;

        if (queue.size() == 0) {
            return new int[]{0, 0};
        }

        for (FileSendStreamer fss : queue) {

            FileStreamer.NetStatus status = fss.getNetStatus();

            if (status != FileStreamer.NetStatus.SUCCESS) {
                good += 1;
            } else {
                bad += 1;
            }
        }

        return new int[]{good, bad};
    }

    public NetStatus getNetStatus() {
        return netStatus;
    }

}
