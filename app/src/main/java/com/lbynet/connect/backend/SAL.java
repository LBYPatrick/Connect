package com.lbynet.connect.backend;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.core.DataPool;

import java.io.File;
import java.net.InetAddress;

/*
SAL stands for "Software Abstract Layer", a class that mainly addresses differences in operating systems.
 */
public class SAL {

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

        msg = "[CONNECT]" + msg;

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

    public static void print(String tag, String msg) {
        print(MsgType.VERBOSE,tag,msg);
    }

    public static String getDeviceName() {

        String name = Settings.Secure.getString(DataPool.activity.getContentResolver(),"bluetooth_name");

        if(name == null) {
            name = Build.BRAND +" " + Build.MODEL + " " + Build.ID;
        }

        return name;
    }

    public static void print(Exception e) {

        String msg = "[Connect]";

        msg += "Exception: " + e.toString() + "\n"
                + "Message: " + e.getMessage() + "\n"
                + "Location: " + "\n";

        for(StackTraceElement i : e.getStackTrace()) {
            msg += "\t" + i.getClassName() + "." + i.getMethodName() + "(Line " + i.getLineNumber() + ")\n";
        }

        Log.e("Exception", msg);
    }

    public static void printUri(Uri uri, ContentResolver resolver) {

        Cursor query = resolver.query(uri,null,null,null,null,null);

        query.moveToFirst();

        SAL.print(MsgType.VERBOSE,
                "printUri",
                "Scheme: " + uri.getScheme() + "\n" +
                "Path: " + uri.getPath() + "\n" +
                "Authority: " + uri.getAuthority() + "\n" +
                "Query: " + uri.getQuery() + "\n" +
                "Type: " + resolver.getType(uri) + "\n");


        SAL.print(MsgType.VERBOSE,
                "printUri",
                "Filename (by query): " + query.getString(query.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
    }
}
