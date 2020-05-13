package com.lbynet.connect.backend.frames;

import com.lbynet.connect.backend.SAL;

public abstract class ParallelTask {

    public enum ThreadStatus {
        IDLE,
        WORKING,
        GOOD,
        BAD
    }

    private ThreadStatus threadStatus = ThreadStatus.IDLE;

    final public void start() {

        try {
            preRun();
        } catch (Exception e) {
            e.printStackTrace();
            threadStatus = ThreadStatus.BAD;
        }

        if(threadStatus == ThreadStatus.BAD) {
            return;
        }

        new Thread( () -> {

            try {
                threadStatus = ThreadStatus.WORKING;
                run();
                threadStatus = ThreadStatus.GOOD;

            } catch (Exception e) {
                SAL.print(e);
                threadStatus = ThreadStatus.BAD;
            }

        }).start();
    }

    final public ThreadStatus getThreadStatus() {
        return threadStatus;
    }

    public abstract void run() throws Exception;

    public void preRun() throws Exception { }

}
