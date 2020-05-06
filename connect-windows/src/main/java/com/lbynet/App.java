package com.lbynet;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class App {
    public static void main(String[] args) throws Exception {

        SAL.print("Listening...");


        AudioStreamer.init(233);

        AudioStreamer.start();


        /*
        Pairing.start();

        Thread.sleep(500);

        SAL.print("Host: " + Pairing.getSelfName() + "@" + Pairing.getSelfAddress());


        for(String ip : Pairing.getAllDeviceIPs()) {
            SAL.print(ip);
        }

        */

        while(true) {
            
        }

    }
}