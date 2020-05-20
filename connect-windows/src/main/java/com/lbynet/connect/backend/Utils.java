package com.lbynet.connect.backend;


import com.lbynet.connect.backend.frames.FileInfo;

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
        return "C:/users/lbypa/Desktop/Connect";
    }
}
