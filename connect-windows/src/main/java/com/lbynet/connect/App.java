package com.lbynet.connect;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.lbynet.connect.backend.*;
import com.lbynet.connect.backend.networking.*;
import com.lbynet.connect.backend.networking.Pairing;
import org.json.*;

public class App {
    public static void main(String[] args) throws Exception {

        Socket s;
        ServerSocket ss = new ServerSocket();

        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(35678));

        SAL.print("Device ID: " + Pairing.getSelfUid());

        SAL.print("Target Port: " + Utils.getTargetPort(InetAddress.getLocalHost().getHostAddress()));

        Pairing.start();


        while (true) {

            SAL.print("Listening...");

            String data = "";

            s = ss.accept();

            InputStream input = s.getInputStream();
            OutputStream output = s.getOutputStream();

            SAL.print("Incoming Connection...");

            byte[] buffer = new byte[1024];

            while (true) {

                int numRead = input.read(buffer);
                byte[] temp = new byte[numRead];

                for (int i = 0; i < numRead; ++i) {
                    temp[i] = buffer[i];
                }

                data += new String(temp, StandardCharsets.UTF_8);


                if (data.contains("<EOF>")) {

                    data = data.substring(0, data.length() - 5);

                    break;
                }
            }

            JSONArray fileList = new JSONArray(data);
            JSONArray portList = new JSONArray();

            for (int i = 0; i < fileList.length(); ++i) {

                String filename = fileList.getString(i);
                int port = Utils.getTransferPort();

                new FileRecvStreamer(filename, "C:/Users/lbypa/Desktop/Connect", port).start();

                portList.put(port);
            }

            output.write((portList.toString() + "<EOF>").getBytes(StandardCharsets.UTF_8));

            s.close();

            //Thread.sleep(5000);
        }
    }
}