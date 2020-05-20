package com.lbynet.connect.backend.frames;

import com.lbynet.connect.backend.SAL;

public class ParallelTask {

    public enum ThreadStatus {
        IDLE,
        WORKING,
        GOOD,
        INTERRUPTED,
        BAD
    }

    private ThreadStatus threadStatus = ThreadStatus.IDLE;

    Thread t_;

    final public void start() {

        try {
            preRun();
        } catch (Exception e) {
            SAL.print(e);
            threadStatus = ThreadStatus.BAD;
        }

        if(threadStatus == ThreadStatus.BAD) {
            return;
        }


        t_ = new Thread( () -> {

            try {
                threadStatus = ThreadStatus.WORKING;
                run();
                threadStatus = ThreadStatus.GOOD;

            } catch (Exception e) {
                SAL.print(e);
                threadStatus = ThreadStatus.BAD;
            }

        });

        t_.start();
    }

    final public ThreadStatus getThreadStatus() {
        return threadStatus;
    }

    public void run() throws Exception {

    };

    final public void stop() {
        t_.interrupt();
        threadStatus = ThreadStatus.INTERRUPTED;
    }

    public void preRun() throws Exception { }

}
