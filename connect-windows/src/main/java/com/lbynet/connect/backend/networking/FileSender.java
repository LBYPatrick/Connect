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

            SAL.print(SAL.MsgType.ERROR,"FileSender", "Failed to initialize FileSender because of malformed parameter");
            netStatus = NetStatus.INIT_FAIL;
            return;
        }

        try {

            //Build file info in JSON
            JSONObject json = new JSONObject();

            for(FileInfo file : fileInfos_) {
                json.put(file.name, file.size);
            }


            //Send file info to target
            Socket socket = new Socket(ip_, Utils.getTargetPort(ip_));

            IO.sendDataToRemote(socket,json.toString());

            //Retrive file transferring port from target
            String receivedData = IO.getDataFromRemote(socket, 5000);

            SAL.print(receivedData);

            if (receivedData != null) {
                SAL.print(SAL.MsgType.VERBOSE,"FileSender", "File Transfer Ports: " + receivedData);
            }

            netStatus = NetStatus.TRANSFERRING;

            //Parse Response (Which contains an array of ports)
            JSONArray ports = new JSONArray(receivedData);

            //Send Actual Data
            for(int i = 0; i < ports.length(); ++i) {

                FileSendStreamer fss = new FileSendStreamer(fileStreams_.get(i),fileInfos_.get(i).size,ip_,ports.getInt(i));
                queue.add(fss);
                fss.start();
            }

            numFiles = queue.size();

            //Wait till everything is done
            while(percentDone < 1) {

                double tempPercent = 0;
                long tempSpeed = 0;

                //Iterate through every task
                for(int i = 0; i < queue.size(); ++i) {

                    tempPercent += (queue.get(i).getProgress() / numFiles);
                    tempSpeed += queue.get(i).getAverageSpeedInKbps();

                }

                percentDone = tempPercent;
                speedInKilobytesPerSec = tempSpeed;

                Thread.sleep(100);
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



    public NetStatus getNetStatus() {
        return netStatus;
    }

}
