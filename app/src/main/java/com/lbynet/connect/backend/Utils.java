package com.lbynet.connect.backend;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;

import com.lbynet.connect.backend.frames.FileInfo;

import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.Random;

public class Utils {

    final private static String LETTER_INDEX = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static Utils instance = new Utils();
    private static int numPortsRequested = 0;

    private static Random randomizer;

    private Utils() {
        randomizer = new Random();
    }


    public static JSONObject getCompiledFileInfo(String... paths) throws Exception {
        JSONObject r = new JSONObject();

        for(String path : paths) {

            //Format: "<filename>" : "<file_size>"
            r.put(Utils.getFilename(path), getFileSize(path));
        }

        return r;
    }

    public static long getFileSize(String path) throws SecurityException {

        File file = new File(path);
        return file.length();

    }

    public static String getFilename(String path) {

        if(!path.contains("/")) {
            return path;
        }
        else {
            return path.substring(path.lastIndexOf("/") + 1,path.length());
        }

    }

    public static String getRandomString(int size) {
        String out = "";

        for(int i = 0; i < size; ++i) {
            out += LETTER_INDEX.charAt(randomizer.nextInt(LETTER_INDEX.length()));
        }

        return out;
    }

    public static byte [] getTrimedData(byte[] rawBuffer, int length) {
        return getTrimedData(rawBuffer,0,length);
    }

    public static byte [] getTrimedData(byte[] rawBuffer, int offset, int length) {

        if(rawBuffer.length == length) {
            return rawBuffer;
        }

        byte [] r = new byte [length];

        for(int i = 0; i < length; ++i) {
            r[i] = rawBuffer[offset + i];
        }

        return r;
    }

    public static FileInfo getFileInfo(Uri uri, ContentResolver contentResolver) {

        Cursor query = contentResolver.query(uri,null,null,null,null,null);

        query.moveToFirst();

        return new FileInfo(query.getString(query.getColumnIndex(OpenableColumns.DISPLAY_NAME)),
                            Long.parseLong(query.getString(query.getColumnIndex(OpenableColumns.SIZE))));

    }

    public static Bitmap getWallpaper(Context context) {
        return ((BitmapDrawable)(WallpaperManager.getInstance(context).getDrawable())).getBitmap();
    }

    public static void hideView(View v, boolean isGone, int durationInMs) {
        v.animate()
                .alpha(0f)
                .setDuration(durationInMs)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v.setVisibility(isGone ? View.GONE : View.INVISIBLE);
                    }
                });
    }

    public static void showView(View v, int durationInMs) {

        //Make it not "GONE" so that the view occupies the space it needs
        v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);

        v.animate()
                .alpha(1f)
                .setDuration(durationInMs)
                .setListener(null);
    }

    public static int getTargetPort(String ip) {
        int r = 0;

        for(char i : ip.toCharArray()) {
            r += (int)i;
        }

        //Bound checks
        while(r < 30000) {
            r += 1000;
        }

        while(r >= 65536) {
            r -= 10000;
        }

        return r;
    }

    public static int getTransferPort() {
        int r = 40000 + numPortsRequested;

        numPortsRequested += 1;

        return r;
    }

    public static String numToString(double in, int digits) {
        NumberFormat fmt = NumberFormat.getInstance();

        fmt.setMaximumFractionDigits(digits);

        return fmt.format(in).toString();

    }

    public static String getOutputPath() {
        return "/storage/emulated/0/Download/";
    }
}
