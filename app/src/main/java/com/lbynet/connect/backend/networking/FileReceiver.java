package com.lbynet.connect.backend.networking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lbynet.connect.backend.IO;
import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.frames.FileReceiveListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class FileReceiver {

    static String TAG = FileReceiver.class.getSimpleName();

    static Socket s_;
    static ServerSocket ss_;
    static boolean isStarted_ = false;
    static boolean isBusy_ = false;
    static Thread t_;
    static Runnable mainTask;
    static FileReceiveListener listener_;
    static private FileReceiver instance = new FileReceiver();

    public static void setOnReceiveListener(FileReceiveListener listener) {
        listener_ = listener;
    }

    private FileReceiver() {

        try {
            //ss_ = new ServerSocket();
            //ss_.setReuseAddress(true);
        } catch (Exception e) {
            if(e instanceof SocketException || e instanceof IOException) {
                SAL.print(SAL.MsgType.ERROR,TAG,"Exception threw during initialization");
                SAL.print(e);
            }
        }

        mainTask = () -> {

            try {
                String selfIp = null;

                //Fetch IP address from Pairing
                while(selfIp == null) {
                    selfIp = Pairing.getSelfAddress();
                    Utils.sleepFor(50);
                }

                SAL.print(SAL.MsgType.VERBOSE,TAG,"IP: " + selfIp);

                //Bind every time
                if(ss_ != null && ss_.isBound()) { ss_.close(); }

                ss_ = new ServerSocket();
                ss_.setReuseAddress(true);
                ss_.bind(new InetSocketAddress(Utils.getTargetPort(selfIp)));
                SAL.print(SAL.MsgType.VERBOSE,TAG,"Initiated with ip " + selfIp + " and port " + Utils.getTargetPort(selfIp));

                while(true) {

                    String data = "";
                    String senderName = "";

                    if(DataPool.isInvisibleMode) {
                        Thread.sleep(5000);
                        continue;
                    }

                    SAL.print(SAL.MsgType.VERBOSE,TAG,"Listening");

                    s_ = ss_.accept();

                    isBusy_ = true;

                    SAL.print(SAL.MsgType.VERBOSE,TAG,"Incoming Connection");

                    data = IO.getDataFromRemote(s_,500);

                    ArrayList<FileRecvStreamer> streams = new ArrayList<>();


                    JsonObject received = JsonParser.parseString(data).getAsJsonObject();
                    JsonArray files = received.getAsJsonArray("files");
                    JsonArray sizes = received.getAsJsonArray("sizes");

                    senderName = received.get("device").getAsString();

                    JsonArray portList = new JsonArray();

                    for (int i = 0; i < files.size(); ++i) {

                        int port = Utils.getTransferPort();

                        SAL.print(SAL.MsgType.VERBOSE,"FileListener", "Filename: "
                                + files.get(i).getAsString()
                                + "\n\t"
                                + "Port: " + port
                                + "\n\t"
                                + "Size: " + sizes.get(i).getAsLong() + " bytes");

                        FileRecvStreamer stream = new FileRecvStreamer(files.get(i).getAsString(), Utils.getOutputPath(), port,sizes.get(i).getAsLong());
                        streams.add(stream);
                        stream.start();

                        portList.add(port);
                    }

                    IO.sendDataToRemote(s_,portList.toString());

                    //Call Listener
                    //which fires up the notification defined in Visualizer.showReceiveProgress()
                    //Registered in LauncherActivity and SendActivity
                    if(listener_ != null) {
                        listener_.onFileReceive(senderName,streams);
                    }

                    s_.close();
                    isBusy_ = false;
                }

            } catch (Exception e) {
                if(!(e instanceof InterruptedException)) {
                    SAL.print(e);
                }
            }

        };
    }

    /**
     * Starts the listening thread (Requires Pairing to be running beforehand)
     */
    public static void start() {

        if(isStarted_) {
            return;
        }

        isStarted_ = true;

        try {

            t_ = new Thread(mainTask);
            t_.start();

        } catch (Exception e) {
            SAL.print(e);
        }

        SAL.print(SAL.MsgType.VERBOSE,TAG,"FileReceiver started.");

    }

    public static boolean isBusy() {
        return isBusy_;
    }

    public static void restart() {
        if(isStarted_) {
            stop();
        }
        start();
    }

    /**
     * Schedule a restart (if a handshaking session is active, restart after the session is done)
     */
    public static void restartLater() {
        new Thread (() -> {

            while(isBusy_) { Utils.sleepFor(50); }

            restart();

        }).start();
    }

    /**
     * Stops the listening thread (Would do nothing if the thread is not started)
     */
    public static void stop() {

        if(!isStarted_) {
            return;
        }

        isStarted_ = false;
        t_.interrupt();
        SAL.print(SAL.MsgType.VERBOSE,TAG,"FileReceiver stopped.");
    }
}
