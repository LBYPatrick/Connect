package com.lbynet.connect.backend;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.lbynet.connect.backend.networking.Pairing;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class IO {

    final public static int RECV_BUFFER_SIZE = 8192;

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
            SAL.print(e);
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

    public static boolean sendDataToRemote(Socket s, String msg) {

        if(s.isClosed()) {
            return false;
        }

        try {
            s.getOutputStream().write(msg.getBytes(StandardCharsets.UTF_8));
            s.shutdownOutput();
        } catch (Exception e) {
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

                if(bytesRead == -1) {
                    return data;
                }
                else {
                    data += new String(Utils.getTrimedData(buffer,bytesRead));
                }

                if(data.length() == 0 && timer.getElaspedTimeInMs() >= timeoutInMs) {
                        return null;
                }
            }

        } catch (Exception e) {
            SAL.print(e);
        }

        return null;
    }

    //Brute-force pinging -- doesn't work under some networks
    public static ArrayList<String> getAllDeviceIPs() {

        String gateway = Pairing.getSubnetAddr();

        ArrayList<String> reachable = new ArrayList<>();

        boolean[] status = new boolean[255];

        Arrays.fill(status, false);

        for (int i = 0; i < 255; ++i) {

            //Java is stupid
            final int n = i;

            new Thread(() -> {

                String addr = gateway + "." + (n + 1);

                try {
                    if (InetAddress.getByName(addr).isReachable(200)) {
                        reachable.add(addr);
                    }
                } catch (Exception e) {
                    //Shhhh
                } finally {
                    status[n] = true;
                }
            }).start();
        }

        while (true) {

            boolean isDone = true;

            for (int i = 0; i < 255; ++i) {
                if (status[i] == false) {
                    isDone = false;
                    break;
                }
            }

            if (isDone) break;
        }

        return reachable;
    }


}
