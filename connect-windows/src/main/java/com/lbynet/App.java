package com.lbynet;

import javafx.scene.media.MediaPlayer;

import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;

public class App {
    public static void main(String[] args) throws Exception {

        ServerSocket s = new ServerSocket(233);

        SAL.print("Listening...");

        Socket socket = s.accept();


        AACStreamer.init(socket);

        AACStreamer.start();



        Pairing.start();

        Thread.sleep(500);

        SAL.print("Host: " + Pairing.getSelfName() + "@" + Pairing.getSelfAddress());

        /*
        for(String ip : Pairing.getAllDeviceIPs()) {
            SAL.print(ip);
        }
        */


        while(true) {
            
        }
    }
}