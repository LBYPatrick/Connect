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

        try {
            Pairing.start();
            FileListener.start();

            while(true) {
                Thread.sleep(10000);
            }

        } catch (Exception e) {
            SAL.print(e);
        }


    }
}