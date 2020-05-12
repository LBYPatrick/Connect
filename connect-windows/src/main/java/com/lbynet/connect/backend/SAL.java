package com.lbynet.connect.backend;

import java.net.InetAddress;

//This is a stub class that simluates whatever that is happening on Android devices
public class SAL {

    public enum MsgType {
        ERROR,
        DEBUG,
        VERBOSE,
        INFO,
        WARN,
        ASSERT
    }

    public static void print(String msg) {
        print(MsgType.VERBOSE,"DefaultTag",msg);
    }

    public static void print(MsgType type, String tag, String msg) {
        String s = "";

        switch(type) {
            case ERROR:
                s += "E/";
                break;
            case VERBOSE:
                s += "V/";
                break;
            case DEBUG:
                s += "D/";
                break;
            case INFO:
                s += "I/";
                break;
            case WARN:
                s += "W/";
                break;
        }

        s += tag + ": " + msg;

        System.out.println(s);
    }

    public static String getDeviceName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void print(Exception e) {
        print(e.getClass().toString() + ": "  + e.getStackTrace().toString());
    }
}
