package app;

import java.net.NetworkInterface;
import java.util.Collections;

public class App {
    public static void main(String[] args) throws Exception {

        Core.print(Collections.list(NetworkInterface.getByName("wlan2").getInetAddresses()).get(0).getHostAddress());
        

        Pairing.start();

        while(true) {
            
        }
    }
}