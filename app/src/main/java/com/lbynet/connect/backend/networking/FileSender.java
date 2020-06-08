package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.frames.FileInfo;
import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FileSender extends ParallelTask {

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
            JSONObject json = new JSONObject();

            for (FileInfo file : fileInfos_) {
                json.put(file.name, file.size);
            }


            //Send file info to target
            Socket socket = new Socket(ip_, Utils.getTargetPort(ip_));

            IO.sendDataToRemote(socket, json.toString());

            //Retrive file transferring port from target
            String receivedData = IO.getDataFromRemote(socket, 5000);

            if (receivedData != null) {
                SAL.print(receivedData);
                SAL.print(SAL.MsgType.VERBOSE, "FileSender", "File Transfer Ports: " + receivedData);
            }
            else {
                netStatus = NetStatus.HANDSHAKE_TIMEOUT;
                return;
            }

            netStatus = NetStatus.TRANSFERRING;

            Utils.sleepFor(200);

            //Parse Response (Which contains an array of ports)
            JSONArray ports = new JSONArray(receivedData);

            //Start file receive streamers
            for (int i = 0; i < ports.length(); ++i) {

                FileSendStreamer fss = new FileSendStreamer(fileStreams_.get(i), fileInfos_.get(i).size, ip_, ports.getInt(i));
                queue.add(fss);
                fss.start();
            }

            numFiles = queue.size();

            //Wait till everything is done
            while (true) {

                double tempPercent = 0;
                long tempSpeed = 0;

                //Iterate through every task
                for (int i = 0; i < queue.size(); ++i) {

                    tempPercent += (queue.get(i).getProgress());
                    tempSpeed += queue.get(i).getAverageSpeedInKbps();

                }

                percentDone = tempPercent / numFiles;
                speedInKilobytesPerSec = tempSpeed;

                SAL.print("Speed: " + tempSpeed + "\tPercent done: " + percentDone);

                if (percentDone >= 1) {
                    SAL.print("Getting out...");
                    break;
                }

                Thread.sleep(300);
            }

        } catch (Exception e) {
            SAL.print(e);

            if (e instanceof SecurityException) {
                netStatus = NetStatus.INIT_FAIL;
            } else if (e instanceof UnknownHostException || e instanceof IOException) {
                netStatus = NetStatus.HANDSHAKE_TIMEOUT;
            } else {
                netStatus = NetStatus.INTERRUPTED;
            }

            SAL.print("Failed to transfer files, reason: " + netStatus.toString());

            return;
        }

        SAL.print("All Done");
        netStatus = NetStatus.DONE;
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
