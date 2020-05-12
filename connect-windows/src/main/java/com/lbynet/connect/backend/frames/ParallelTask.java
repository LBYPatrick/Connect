package com.lbynet.connect.backend.frames;

import com.lbynet.connect.backend.*;

public abstract class ParallelTask {

    enum Status {
        IDLE,
        WORKING,
        GOOD,
        BAD
    }

    private Status status = Status.IDLE;

    final public void start() {

        try {
            preRun();
        } catch (Exception e) {
            e.printStackTrace();
            status = Status.BAD;
        }

        if(status == Status.BAD) {
            return;
        }

        new Thread( () -> {

            try {
                status = Status.WORKING;
                run();
                status = Status.GOOD;

            } catch (Exception e) {
                SAL.print(e);
                status = Status.BAD;
            }

        }).start();
    }

    final public Status getStatus() {
        return status;
    }

    public abstract void run() throws Exception;

    public void preRun() throws Exception { }

}
