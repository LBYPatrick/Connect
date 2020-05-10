package com.lbynet.connect.backend.networking;

import android.util.Log;

import com.lbynet.connect.backend.IO;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class FileSender {

    private String ip_ = "";
    private String[] filePaths_;
    private Status status = Status.IDLE;

    public FileSender(String ip, String... filePaths) {
        ip_ = ip;
        filePaths_ = filePaths;
    }

    public enum Status {
        IDLE,
        INIT_FAIL,
        HANDSHAKE_SUCCESS,
        HANDSHAKE_REJECTED,
        HANDSHAKE_TIMEOUT,
        TRANSFERRING,
        INTERRUPTED,
        DONE
    }

    ;

    public void start() {

        new Thread(() -> doInBackground()).start();

    }

    private void doInBackground() {

        if (ip_.length() == 0 || filePaths_.length == 0) {

            Log.e("FileSender", "Failed to initialize FileSender because of malformed parameter");
            status = Status.INIT_FAIL;
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
                Log.v("FileSender", "File Transfer Ports: " + receivedData);
            }

            status = Status.HANDSHAKE_SUCCESS;

            //Parse Response (Which contains an array of ports)
            JSONArray ports = new JSONArray(receivedData);

            //Send Actual Data
            for(int i = 0; i < filePaths_.length; ++i) {
                new FileSendStreamer(filePaths_[i],ip_,ports.getInt(i)).start();
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (e instanceof SecurityException) {
                status = Status.INIT_FAIL;
            } else if (e instanceof UnknownHostException || e instanceof IOException) {
                status = Status.HANDSHAKE_TIMEOUT;
            } else {
                status = Status.INTERRUPTED;
            }

            SAL.print("Failed to transfer files, reason: " + status.toString());

            return;
        }

        status = Status.DONE;
        return;
    }

    public Status getStatus() {
        return status;
    }

}
