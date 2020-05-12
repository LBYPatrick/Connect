package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.frames.ParallelTask;
import com.lbynet.connect.backend.*;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class FileSender extends ParallelTask {

    private String ip_ = "";
    private String[] filePaths_;
    private NetStatus netStatus = NetStatus.IDLE;

    public FileSender(String ip, String... filePaths) {
        ip_ = ip;
        filePaths_ = filePaths;
    }

    public enum NetStatus {
        IDLE,
        INIT_FAIL,
        HANDSHAKE_SUCCESS,
        HANDSHAKE_REJECTED,
        HANDSHAKE_TIMEOUT,
        TRANSFERRING,
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
            Socket socket = new Socket(ip_, 35678);
            InputStream input = socket.getInputStream();

            socket.getOutputStream().write((fileList.toString() + "<EOF>").getBytes(StandardCharsets.UTF_8));

            //Retrive file transferring port from target
            String receivedData = IO.getDataFromRemote(socket, 5000);

            if (receivedData != null) {
                SAL.print(SAL.MsgType.VERBOSE,"FileSender", "File Transfer Ports: " + receivedData);
            }

            netStatus = NetStatus.HANDSHAKE_SUCCESS;

            //Parse Response (Which contains an array of ports)
            JSONArray ports = new JSONArray(receivedData);

            //Send Actual Data
            for(int i = 0; i < filePaths_.length; ++i) {
                new FileSendStreamer(filePaths_[i],ip_,ports.getInt(i)).start();
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

    public NetStatus getNetStatus() {
        return netStatus;
    }

}
