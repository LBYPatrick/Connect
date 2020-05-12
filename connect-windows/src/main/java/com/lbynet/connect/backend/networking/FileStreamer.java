package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.frames.ParallelTask;

public abstract class FileStreamer extends ParallelTask {

    enum NetStatus {
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

}
