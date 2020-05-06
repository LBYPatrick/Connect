package app;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;

public class App {
    public static void main(String[] args) throws Exception {

        Pairing.start();

        Thread.sleep(500);

        for(String i : Pairing.getAllDeviceIPs()) {
            SAL.print(i);
        }

        while(true){

        }
    }
}