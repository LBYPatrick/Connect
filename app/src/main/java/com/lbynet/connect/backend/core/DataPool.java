package com.lbynet.connect.backend.core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

public class DataPool {

    final public static int NUM_TARGET_PLACEHOLDERS = 10;

    public static boolean isPairingReady = false,
                          isWifiConnected = false,

                          isPowerSavingMode = false,
                          isInvisibleMode = false;

    public static Bitmap wallpaper;

    public static AppCompatActivity activity;

    public static String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

}
