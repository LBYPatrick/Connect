package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.*;

import java.io.*;
import java.net.Socket;

public class FileSendStreamer extends FileStreamer {

    private String path_, ip_;
    private int port_;
    private long totalBytesRead = 0,
            lastBytesRead = 0,
            lastSpeed = 0;
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

            netStatus = NetStatus.WORKING;

            //Start Looping and send data
            while (!socket_.isClosed()) {

                int bytesRead = in_.read(buffer);

                //Connection closed prematurely
                if(bytesRead == -1 && totalBytesRead < fileSize_) {
                    break;
                }
                else if(bytesRead == -1 && totalBytesRead == fileSize_) {
                    isSuccess = true;
                    break;
                }

                else if(bytesRead < RW_BUFFER_SIZE) {
                    totalBytesRead += bytesRead;
                    out_.write(Utils.getTrimedData(buffer, bytesRead));
                    isSuccess = true;
                    break;
                }
                else {
                    totalBytesRead += bytesRead;
                    out_.write(Utils.getTrimedData(buffer, bytesRead));
                }
            }

            socket_.shutdownOutput();
            socket_.shutdownInput();

            out_.close();
            in_.close();

            if(isSuccess) {
                netStatus = NetStatus.SUCCESS;
                SAL.print(SAL.MsgType.VERBOSE,"FileSendStreamer","File sent. Progress: " + getProgress());
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
        }
        else if(netStatus == NetStatus.SUCCESS) {
            return 1;
        }
        else {

            long bottom = (fileSize_ == 0) ? 1 : fileSize_;
            double top = totalBytesRead;
            double result = top / bottom;

            SAL.print("Top:" + top + "\tBottom:" + bottom);

            return result;
        }
    }

    /**
     * Calculate transfer speed in kilobytes per second
     * @return
     */
    public long getAverageSpeedInKbps() {

        if(timer.getElaspedTimeInMs() < 300 && getProgress() > 0.10) {
            return lastSpeed;
        }

        float speedRate = ((float)(totalBytesRead - lastBytesRead)) / 1024 / timer.getElaspedTimeInMs() * 1000;

        timer.start();
        lastBytesRead = totalBytesRead;

        lastSpeed = ((long) speedRate + lastSpeed) / 2;

        return lastSpeed;
    }
}



