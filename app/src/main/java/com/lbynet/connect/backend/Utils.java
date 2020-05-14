package com.lbynet.connect.backend;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.WallpaperManager;
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
import android.provider.Settings;
import android.view.View;

import org.json.JSONObject;

import java.io.File;
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

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**`
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
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

    public static String getOutputPath() {
        return "/storage/emulated/0/Download/";
    }
}
