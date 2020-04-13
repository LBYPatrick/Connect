package app;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;

public class App {
    public static void main(String[] args) throws Exception {

        Socket socket = new Socket(InetAddress.getByName("127.0.0.1"),233);

        while(true) {
        }
    }
}