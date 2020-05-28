package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.IO;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.frames.FileReceiveListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FileReceiver {

    static Socket s_;
    static ServerSocket ss_;
    static boolean isBusy_ = false;
    static Thread t_;
    static FileReceiveListener listener_;

    public static void setOnReceiveListener(FileReceiveListener listener) {
        listener_ = listener;
    }

    public static void start() {

        if(isBusy_) {
            return;
        }

        isBusy_ = true;

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

                while(isBusy_) {

                    String data = "";
                    String senderName = "";
                    String senderIp = "";

                    if(DataPool.isInvisibleMode) {
                        Thread.sleep(5000);
                        continue;
                    }

                    SAL.print("Listening...");

                    s_ = ss_.accept();

                    SAL.print("Incoming Connection...");

                    data = IO.getDataFromRemote(s_,500);

                    //Get sender name
                    ArrayList<Pairing.Device> devices = Pairing.getPairedDevices();
                    senderIp = s_.getInetAddress().toString().substring(1);

                    for(Pairing.Device d : devices) {
                        if(d.ip.equals(senderIp)) {
                            senderName = d.uid;
                            SAL.print("Address from Pairing: " + d.ip);
                            break;
                        }
                    }

                    SAL.print("Address from socket: " + senderIp);
                    SAL.print("SenderName: " + senderName);

                    JSONObject receivedData  = new JSONObject(data);
                    JSONArray portList = new JSONArray();

                    SAL.print(data);

                    ArrayList<String> fileList = new ArrayList<>();
                    receivedData.keys().forEachRemaining(fileList::add);
                    ArrayList<FileRecvStreamer> streams = new ArrayList<>();

                    for (String file : fileList) {

                        int port = Utils.getTransferPort();

                        SAL.print(SAL.MsgType.VERBOSE,"FileListener", "Filename: " + file + "\t" + "Port: " + port);

                        FileRecvStreamer stream = new FileRecvStreamer(file, Utils.getOutputPath(), port,receivedData.getLong(file));
                        streams.add(stream);
                        stream.start();

                        portList.put(port);
                    }

                    //Call Listener
                    if(listener_ != null) {
                        listener_.onFileReceive(senderName,streams);
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
