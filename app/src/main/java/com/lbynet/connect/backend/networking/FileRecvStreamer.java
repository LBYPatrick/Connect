package com.lbynet.connect.backend.networking;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Timer;
import com.lbynet.connect.backend.Utils;

public class FileRecvStreamer extends FileStreamer {

    private String filename_,
            targetDirectory_;
    private Socket socket_;
    private long numCyclesRead_ = 0, lastCycleCount = 0,lastSpeed = 0;
    private int port_;
    private long fileSize_;
    private Timer timer = new Timer("FileRecvStreamer");

    public FileRecvStreamer(String filename,String targetDirectory, int port,long fileSize) {
        filename_ = filename;
        targetDirectory_ = targetDirectory;
        port_ = port;
        fileSize_ = fileSize;
    }

    @Override
    public void run() {
        try {


            ServerSocket ss = new ServerSocket();

            ss.setReuseAddress(true);

            try {
                ss.bind(new InetSocketAddress(port_));
            } catch (IOException e) {
                SAL.print(e);
            }

            socket_ = ss.accept();

            netStatus = NetStatus.WORKING;

            File tempDir = new File(targetDirectory_);

            InputStream in = socket_.getInputStream();
            File file = new File(targetDirectory_ + "/" + filename_);
            byte [] buffer = new byte[RW_BUFFER_SIZE];

            FileOutputStream out = new FileOutputStream(file);

            //SAL.print(SAL.MsgType.VERBOSE,"FileRecvStreamer","File " + filename_ + " receiving...");

            boolean isSuccess = false;

            while(!socket_.isClosed()) {

                int bytesRead = in.read(buffer);

                numCyclesRead_ += 1;

                if(bytesRead == -1) {
                    isSuccess = true;
                    break;
                }
                else {
                    out.write(Utils.getTrimedData(buffer, bytesRead));
                }
            }

            Utils.sleepFor(50);

            out.close();
            in.close();

            if(isSuccess) {
                netStatus = NetStatus.SUCCESS;
                SAL.print(SAL.MsgType.VERBOSE,"FileRecvStreamer","File " + filename_ + " received.");
                return;
            }
            else {
                netStatus = NetStatus.BAD_NETWORK;
            }

        } catch(Exception e) {
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
            double bottom = (fileSize_ == 0) ? 1 : fileSize_;
            double top = RW_BUFFER_SIZE * numCyclesRead_;
            double result = top / bottom;
            return result;
        }
    }

    /**
     * Calculate transfer speed in kilobytes per second
     * @return
     */
    public long getAverageSpeedInKbps() {

        //If file transfer has just started/ended
        if(numCyclesRead_ == lastCycleCount) {
            return lastSpeed;
        }

        if(timer.getElaspedTimeInMs() < 1000) {
            return lastSpeed;
        }

        float speedRate = ((float)(numCyclesRead_ - lastCycleCount)) * RW_BUFFER_SIZE / 1024 / timer.getElaspedTimeInMs() * 1000;

        timer.start();
        lastCycleCount = numCyclesRead_;

        if(speedRate != 0) {
            lastSpeed = (long) speedRate;
        }

        return lastSpeed;

    }
}

