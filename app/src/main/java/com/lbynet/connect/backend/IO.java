package com.lbynet.connect.backend;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;

public class IO {

    public static ArrayList<String> readFile(String filePath) {
        ArrayList<String> buffer = new ArrayList<>();

        readFile(filePath,buffer);

        return buffer;

    }

    public static byte [] readFileAtOnce(String path) {

        try {

            File file = new File(path);
            FileInputStream stream = new FileInputStream(file);

            byte [] buffer = new byte[(int)file.length()];

            Log.v("readFileAtOnce","Reading File " + path +" with size " + Long.toString(file.length()) + "Bytes");

            stream.read(buffer,0,buffer.length);
            stream.close();

            return buffer;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean readFile(String filePath, ArrayList<String> buffer) {

        BufferedReader br;

        try {
            br = new BufferedReader(new FileReader(filePath));

            buffer.clear();

            String line;
            while((line = br.readLine()) != null) {
                buffer.add(line);
            }

            br.close();

        } catch(Exception e) {
            SAL.print(e);
            return false;
        }

        return true;
    }
}
