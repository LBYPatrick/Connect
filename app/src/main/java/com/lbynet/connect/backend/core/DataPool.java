package com.lbynet.connect.backend.core;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

public class DataPool {

    final public static int NUM_TARGET_PLACEHOLDERS = 10;

    public static boolean isPowerSavingMode = false;
    public static boolean isInvisibleMode = false;

    public static ArrayList<Uri> uris;

    public static Blurry.BitmapComposer background;

    public static Context context;

}
