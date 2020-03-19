package com.lbynet.connect.backend;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

/*
SAL stands for "Software Abstract Layer", a class that mainly addresses differences in operating systems.
 */
public class SAL {

    public static AppCompatActivity activity;

    public static void print(String msg) { Log.d("DebugInfo", msg); }

    public static void printException(Exception e) {
        Log.d(e.getClass().toString(), e.getStackTrace().toString());
    }
}
