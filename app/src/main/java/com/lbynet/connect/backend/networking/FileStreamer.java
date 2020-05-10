package com.lbynet.connect.backend.networking;

public abstract class FileStreamer {

    enum Status {
        IDLE,
        WORKING,
        SUCCESS,
        BAD_FILE_INPUT,
        BAD_OUTPUT,
        BAD_NETWORK,
        BAD_GENERAL
    }

    Status status = Status.IDLE;

    public void start() { new Thread(() -> run()).start(); }

    abstract void run();

    public Status getStatus() {
        return status;
    }

}
