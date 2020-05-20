package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.IO;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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

                    SAL.print("Incoming Connection...");

                    data = IO.getDataFromRemote(s_,500);

                    JSONObject receivedData  = new JSONObject(data);
                    JSONArray portList = new JSONArray();

                    SAL.print(data);

                    ArrayList<String> fileList = new ArrayList<>();
                    receivedData.keys().forEachRemaining(fileList::add);

                    for (String file : fileList) {

                        int port = Utils.getTransferPort();

                        SAL.print(SAL.MsgType.VERBOSE,"FileListener", "Filename: " + file + "\t" + "Port: " + port);

                        new FileRecvStreamer(file, Utils.getOutputPath(), port).start();

                        portList.put(port);
                    }

                    IO.sendDataToRemote(s_,portList.toString());

                    s_.close();
                }

            } catch (Exception e) {
                SAL.print(e);
            }


        });

        t_.start();

    }

    public static void stop() {
        isBusy_ = false;
        t_.interrupt();
    }
}
