package com.lbynet.connect.backend;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class Core {

    public static AppCompatActivity activity;

    public static void print(String msg) {
        Log.d("Error", msg);
    }

    public static void printException(Exception e) {
        Log.d(e.getClass().toString(), e.getStackTrace().toString());
    }
}
