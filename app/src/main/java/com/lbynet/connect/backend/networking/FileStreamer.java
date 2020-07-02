package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Timer;
import com.lbynet.connect.backend.frames.ParallelTask;

import java.net.Socket;

public class FileStreamer extends ParallelTask {

    final public static int RW_BUFFER_SIZE = 4096;

    public String TAG = this.getClass().getSimpleName();

    public Socket socket_;
    public long totalBytesRead = 0,
            lastBytesRead = 0,
            lastSpeed = 0;
    public int port_;
    public long fileSize_;

    //For showing speed
    Timer timer = new Timer(this.getClass().getSimpleName());

    public enum NetStatus {
        IDLE,
        WORKING,
        SUCCESS,
        BAD_FILE_INPUT,
        BAD_OUTPUT,
        BAD_NETWORK,
        BAD_GENERAL
    }

    NetStatus netStatus = NetStatus.IDLE;

    public NetStatus getNetStatus() {
        return netStatus;
    }

    public boolean isGood() {
        return netStatus == NetStatus.SUCCESS;
    }

    /**
     * Get filesize in bytes.
     * @return filesize
     */
    public long getFileSize() {
        return fileSize_;
    }

    public double getProgress() {
        if (netStatus != NetStatus.WORKING && netStatus != NetStatus.IDLE) {
            return 1;
        }
        else {

            long bottom = (fileSize_ == 0) ? 1 : fileSize_;
            double top = totalBytesRead;
            double result = top / bottom;

            //SAL.print("Top:" + top + "\tBottom:" + bottom);

            return result;
        }
    }

    public long getNumBytesRead() {

        //SendStreamer is not working
        if (netStatus != NetStatus.WORKING && netStatus != NetStatus.IDLE) {
            return fileSize_;
        }
        //SendStreamer is working
        else {
            return totalBytesRead;
        }
    }

    /**
     * Calculate transfer speed in kilobytes per second
     * @return
     */
    public long getAverageSpeedInKbps() {

        if(timer.getElaspedTimeInMs() < 1000 && getProgress() > 0.10) {
            return lastSpeed;
        }

        float speedRate = ((float)(totalBytesRead - lastBytesRead)) / 1024 / timer.getElaspedTimeInMs() * 1000;

        timer.start();
        lastBytesRead = totalBytesRead;

        lastSpeed = ((long) speedRate + lastSpeed) / 2;

        return lastSpeed;
    }

    public boolean isBad() {
        return !(netStatus == NetStatus.IDLE || netStatus == NetStatus.WORKING || netStatus == NetStatus.SUCCESS);
    }

}
