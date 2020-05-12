package com.lbynet.connect.backend;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class IO {

    final public static int RECV_BUFFER_SIZE = 2048;

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

            SAL.print(SAL.MsgType.VERBOSE,"readFileAtOnce","Reading File " + path +" with size " + file.length() + "Bytes");

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

    public static String getDataFromRemote(Socket s, long timeoutInMs) {
        try {

            if(s.isClosed()) {
                return null;
            }

            InputStream stream = s.getInputStream();
            String data = "";
            Timer timer = new Timer("Socket Timer");
            byte [] buffer = new byte[RECV_BUFFER_SIZE];

            timer.start();

            while(true) {
                int bytesRead = stream.read(buffer);

                switch (bytesRead){
                    case 0:
                        break;
                    default:
                        data += new String(Utils.getTrimedData(buffer,bytesRead));
                        break;
                }

                if(data.lastIndexOf("<EOF>") != -1) {
                    return data.substring(0,data.length()-5);
                }
                else if(data.length() == 0 && timer.getElaspedTimeInMs() >= timeoutInMs) {
                    return null;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
