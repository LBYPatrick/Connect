package com.lbynet.connect.backend;

import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

/*
SAL stands for "Software Abstract Layer", a class that mainly addresses differences in operating systems.
 */
public class SAL {

    public static AppCompatActivity activity;

    public enum MsgType {
        ERROR,
        DEBUG,
        VERBOSE,
        INFO,
        WARN,
    }

    public static void print(String msg) {
        print(MsgType.VERBOSE,"DefaultTag",msg);
    }

    public static void print(MsgType type, String tag, String msg) {

        switch(type) {
            case ERROR:
                Log.e(tag,msg);
                break;
            case VERBOSE:
                Log.v(tag,msg);
                break;
            case DEBUG:
                Log.d(tag,msg);
                break;
            case INFO:
                Log.i(tag,msg);
                break;
            case WARN:
                Log.w(tag,msg);
                break;
        }
    }

    public static String getDeviceName() {
        return Build.BRAND +" " + Build.MODEL + " " + Build.ID;
    }

    public static void print(Exception e) {
        Log.d(e.getClass().toString(), e.getStackTrace().toString());
    }
}
