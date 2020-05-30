package com.lbynet.connect.backend.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

public class DataPool {

    public enum WifiStatus {
        CONNECTED,
        IDLE,
        DISABLED
    }

    final public static int NUM_TARGET_PLACEHOLDERS = 10;

    public static boolean isLauncherActivity = false;
    public static boolean isPowerSavingMode = false;
    public static boolean isInvisibleMode = false;
    public static WifiStatus wifiStatus;

    public static ArrayList<Uri> uris;

    public static Bitmap wallpaper;

    public static Activity activity;

}
