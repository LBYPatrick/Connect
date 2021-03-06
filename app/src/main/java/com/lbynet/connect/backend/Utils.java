package com.lbynet.connect.backend;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.frames.FileInfo;

import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

import jp.wasabeef.blurry.Blurry;

public class Utils {

    final private static String LETTER_INDEX = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static Utils instance = new Utils();
    private static int numPortsRequested = 0;
    private static int numIntRequested = 0;

    private static Random randomizer;

    private Utils() {
        randomizer = new Random();
    }

    public static int getUniqueInt() {
        numIntRequested += 1;
        return 114513 + numIntRequested;
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

    public static boolean isDarkMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static FileInfo getFileInfo(Uri uri, ContentResolver contentResolver) {

        Cursor query = contentResolver.query(uri,null,null,null,null,null);

        query.moveToFirst();

        return new FileInfo(query.getString(query.getColumnIndex(OpenableColumns.DISPLAY_NAME)),
                            Long.parseLong(query.getString(query.getColumnIndex(OpenableColumns.SIZE))));

    }


    public static Locale getLocale() {
        return Resources.getSystem().getConfiguration().getLocales().get(0);
    }

    public static boolean isChinese() {

        Locale language = getLocale();

        SAL.print("Locale:" + language.toLanguageTag());

        return language.toLanguageTag().equals("zh-CN") || language.toLanguageTag().equals("zh-Hans-CN") || language.toLanguageTag().equals("zh-TW");
    }

    public static Bitmap getWallpaper(Context context) {

        if(DataPool.wallpaper != null) {
            return DataPool.wallpaper;
        }

        Bitmap wp = ((BitmapDrawable)(WallpaperManager.getInstance(context).getDrawable())).getBitmap();

        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point displayInfo = new Point();

        display.getRealSize(displayInfo);

        final double wpRatio = (double)wp.getHeight() / wp.getWidth();
        final double dpRatio = (double)displayInfo.y / (double)displayInfo.x;

        //Wallpaper being taller than screen
        if(wpRatio > dpRatio) {
            final int height = (int)(wp.getWidth() * dpRatio);
            DataPool.wallpaper = Bitmap.createBitmap(wp,0,0,wp.getWidth(),height);
        }
        //Wallpaper shorter than screen
        else if(wpRatio < dpRatio) {
            final int width = (int)(wp.getHeight() / dpRatio);
            DataPool.wallpaper = Bitmap.createBitmap(wp,0,0,width,wp.getHeight());
        }
        else {
            DataPool.wallpaper = wp;
        }

        return DataPool.wallpaper;
    }

    public static void hideView(AppCompatActivity activity, View v, boolean isGone, int durationInMs) {
        activity.runOnUiThread( () -> {
            hideView(v,isGone,durationInMs);
        });
    }

    public static void hideView(View v, boolean isGone, int durationInMs) {

        if(v.getVisibility() == View.GONE) {
            return;
        }

        if(durationInMs == 0) {
            v.setAlpha(0);
            v.setVisibility(isGone? View.GONE : View.INVISIBLE);
        }

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

    public static void showView(AppCompatActivity activity,View v, int durationInMs) {
        activity.runOnUiThread( () -> {
            showView(v,durationInMs);
        });
    }

    public static void showView(View v, int durationInMs) {

        if(v.getVisibility() == View.VISIBLE) {
            return;
        }

        if(durationInMs == 0) {
            v.setAlpha(1);
            v.setVisibility(View.VISIBLE);
        }

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

    public static void sleepFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            //Shhhh
        }
    }

    public static String numToString(double in, int digits) {
        NumberFormat fmt = NumberFormat.getInstance();


        fmt.setMinimumFractionDigits(digits);
        fmt.setMaximumFractionDigits(digits);

        return fmt.format(in).toString();

    }

    public static void playTickVibration(Context context) {
        Vibrator v =  (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if(v != null) {
                v.vibrate(VibrationEffect.createOneShot(10,VibrationEffect.DEFAULT_AMPLITUDE));
        }
        else {
            SAL.print("Failed to vibrate because no vibrator is available.");
        }
    }

    public static void playDoubleClickAnimation(Context context) {
        Vibrator v =  (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if(v != null) {
            new Thread( () -> {
                v.vibrate(VibrationEffect.createOneShot(100,200));
                sleepFor(200);
                v.vibrate(VibrationEffect.createOneShot(100,200));
            }).start();
        }
        else {
            SAL.print("Failed to vibrate because no vibrator is available.");
        }
    }

    public static void printToast(Activity activity, String msg, boolean isLongTime) {
        activity.runOnUiThread( () -> {
            Toast.makeText(activity, msg, isLongTime ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        });
    }

    public static String getOutputPath() {

        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Connect";
    }

    public static boolean isPermissionGranted(Context context) {
        boolean isGood = true;

        for (String p : DataPool.permissions) {
            if (context.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                isGood = false;
                break;
            }
        }

        return isGood;
    }
}
