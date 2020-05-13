package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.*;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FileSender extends ParallelTask {

    private String ip_ = "";
    private String[] filePaths_;
    private NetStatus netStatus = NetStatus.IDLE;
    private ArrayList<FileSendStreamer> queue = new ArrayList<>();
    private int numDone = 0;
    private int numFiles = 0;

    public FileSender(String ip, String... filePaths) {
        ip_ = ip;
        filePaths_ = filePaths;
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

        if (ip_.length() == 0 || filePaths_.length == 0) {

            SAL.print(SAL.MsgType.ERROR,"FileSender", "Failed to initialize FileSender because of malformed parameter");
            netStatus = NetStatus.INIT_FAIL;
            return;
        }

        try {

            //Build file info in JSON
            JSONArray fileList = new JSONArray();

            for(String path : filePaths_) {
                fileList.put(Utils.getFilename(path));
            }

            //Send file info to target
            Socket socket = new Socket(ip_, Utils.getTargetPort(ip_));
            InputStream input = socket.getInputStream();

            socket.getOutputStream().write((fileList.toString() + "<EOF>").getBytes(StandardCharsets.UTF_8));

            //Retrive file transferring port from target
            String receivedData = IO.getDataFromRemote(socket, 5000);

            if (receivedData != null) {
                SAL.print(SAL.MsgType.VERBOSE,"FileSender", "File Transfer Ports: " + receivedData);
            }

            netStatus = NetStatus.TRANSFERRING;

            //Parse Response (Which contains an array of ports)
            JSONArray ports = new JSONArray(receivedData);

            //Send Actual Data
            for(int i = 0; i < filePaths_.length; ++i) {
                FileSendStreamer fss = new FileSendStreamer(filePaths_[i],ip_,ports.getInt(i));
                queue.add(fss);
                fss.start();
            }

            numFiles = queue.size();
            ArrayList<Integer> skipList = new ArrayList<>();

            //Wait till everything is done
            while(numDone < queue.size()) {
                for(int i = 0; i < queue.size(); ++i) {
                    boolean isChecked = false;

                    for(int n : skipList) {
                        if (i == n) {
                            isChecked = true;
                            break;
                        }
                    }
                    if(isChecked) continue;

                    if(queue.get(i).isBad() || queue.get(i).isGood()) {
                        skipList.add(i);
                        numDone += 1;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

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

        netStatus = NetStatus.DONE;
        return;
    }

    public double getPercentDone() {

        double numerator = numDone;
        double denominator = (numFiles == 0)? 1 : numFiles;

        SAL.print(numerator + "/" + denominator +"=" + numerator/denominator);

        return numerator / denominator;
    }

    public NetStatus getNetStatus() {
        return netStatus;
    }

}
