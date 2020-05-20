package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.*;

import java.io.*;
import java.net.Socket;

public class FileSendStreamer extends FileStreamer {

    private String path_, ip_;
    private int port_;
    private long numCyclesRead_ = 0;
    private long fileSize_ = 0;
    private InputStream in_;
    private OutputStream out_;

    //For showing speed
    Timer timer = new Timer("FileSendStreamer");
    private long lastCycleCount = 0;

    public FileSendStreamer(String path, String ip, int port) {
        path_ = path;
        port_ = port;
        ip_ = ip;
    }

    public FileSendStreamer(InputStream input, long fileSize, String ip, int port) {
        in_ = input;
        fileSize_ = fileSize;
        ip_ = ip;
        port_ = port;
    }

    @Override
    public void run() {
        try {

            netStatus = NetStatus.WORKING;

            Socket socket_ = new Socket(ip_, port_);
            byte[] buffer = new byte[RW_BUFFER_SIZE];

            SAL.print("Send Port Open");
            out_ = socket_.getOutputStream();

            //Legacy by-path mode
            if (path_ != null) {

                File file = new File(path_);

                if (file.length() == 0L) {
                    netStatus = NetStatus.BAD_FILE_INPUT;
                    return;
                } else {
                    fileSize_ = file.length();
                }

                in_ = new FileInputStream(file);
            }

            timer.start();

            boolean isSuccess = false;

            //Start Looping and send data
            while (!socket_.isClosed()) {

                int bytesRead = in_.read(buffer);

                numCyclesRead_ += 1;

                if(bytesRead == -1) {
                    isSuccess = true;
                    break;
                }
                else if(bytesRead < RW_BUFFER_SIZE) {
                    out_.write(Utils.getTrimedData(buffer, bytesRead));
                    isSuccess = true;
                    break;
                }
                else {
                    out_.write(Utils.getTrimedData(buffer, bytesRead));
                }
            }

            out_.close();
            in_.close();

            if(isSuccess) {
                netStatus = NetStatus.SUCCESS;
                SAL.print(SAL.MsgType.VERBOSE,"FileSendStreamer","File sent.");
                return;
            }
            else {
                netStatus = NetStatus.BAD_NETWORK;
            }

        } catch (Exception e) {
            netStatus = NetStatus.BAD_GENERAL;
            SAL.print(e);
        }
    }

    public double getProgress() {
        if (netStatus != NetStatus.WORKING && netStatus != NetStatus.IDLE) {
            return 1;
        } else {
            double bottom = (fileSize_ == 0) ? 1 : fileSize_;
            double top = RW_BUFFER_SIZE * numCyclesRead_;
            double result = top / bottom;

            return result > 1 ? 0.99 : result;
        }
    }

    /**
     * Calculate transfer speed in kilobytes per second
     * @return
     */
    public long getAverageSpeedInKbps() {

        //If file transfer has just started/ended
        if(numCyclesRead_ == lastCycleCount) {
            return 0;
        }

        float dataTransferred = ((float)(numCyclesRead_ - lastCycleCount)) * RW_BUFFER_SIZE / 1024;

        return (long)(dataTransferred / timer.getElaspedTimeInMs() * 1000);

    }
}



