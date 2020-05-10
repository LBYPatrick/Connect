package com.lbynet;

//This is a stub class that simluates whatever that is happening on Android devices
public class SAL {

    public static void print(String msg) {
        System.out.println(msg);
    }

    public static void printVerbose(String tag, String msg) {
        System.out.println( "v/" + tag + ": " + msg);
    }

    public static void print(Exception e) {
        print(e.getClass().toString() + ": "  + e.getStackTrace().toString());
    }
}
