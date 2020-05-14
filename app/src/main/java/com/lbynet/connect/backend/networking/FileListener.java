package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import org.json.JSONArray;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FileListener {

    static Socket s_;
    static ServerSocket ss_;
    static boolean isBusy_ = false;
    static Thread t_;

    public static void start() {

        if(isBusy_) {
            return;
        }

        t_ = new Thread( () -> {

            try {
                String selfIp = Pairing.getSelfAddress();

                while(selfIp == null) {
                    selfIp = Pairing.getSelfAddress();
                    Thread.sleep(500);
                }

                SAL.print("IP: " + selfIp);

                ss_ = new ServerSocket();

                ss_.setReuseAddress(true);
                ss_.bind(new InetSocketAddress(Utils.getTargetPort(selfIp)));

                SAL.print(SAL.MsgType.VERBOSE,"FileListener","Initiated with ip " + selfIp + " and port " + Utils.getTargetPort(selfIp));

                isBusy_ = true;

                while(isBusy_) {

                    if(DataPool.isInvisibleMode) {
                        Thread.sleep(5000);
                        continue;
                    }

                    SAL.print("Listening...");

                    String data = "";

                    s_ = ss_.accept();

                    InputStream input = s_.getInputStream();
                    OutputStream output = s_.getOutputStream();

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

                        new FileRecvStreamer(filename, Utils.getOutputPath(), port).start();

                        portList.put(port);
                    }

                    output.write((portList.toString() + "<EOF>").getBytes(StandardCharsets.UTF_8));

                    s_.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        });

        t_.start();

    }

    public static void stop() {
        isBusy_ = false;
        t_.interrupt();
    }
}
