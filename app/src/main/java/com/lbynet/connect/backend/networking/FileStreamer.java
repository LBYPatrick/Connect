package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.frames.ParallelTask;

public abstract class FileStreamer extends ParallelTask {

    final public static int RW_BUFFER_SIZE = 8192;

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

    public boolean isBad() {
        return !(netStatus == NetStatus.IDLE || netStatus == NetStatus.WORKING || netStatus == NetStatus.SUCCESS);
    }

}
