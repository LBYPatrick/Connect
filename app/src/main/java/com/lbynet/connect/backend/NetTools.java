package com.lbynet.connect.backend;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

public class NetTools {

    public class ARPRecord {
        public String ip = "", mac = "";

        public ARPRecord() { };

    }

    public static ArrayList<String> getAllDeviceIPs() {

        String gateway = getSubnetAddr();
        ArrayList<String> reachable = new ArrayList<>();

        boolean [] status = new boolean[255];

        Arrays.fill(status,false);

        for(int i = 0; i < 255; ++i) {

            //Java is stupid
            final int n = i;

            new Thread(() -> {

                String addr = gateway + "." + (n + 1);

                try {
                    if(InetAddress.getByName(addr).isReachable(200)) {
                        reachable.add(addr);
                    }
                } catch (Exception e) {
                    //Shhhh
                } finally {
                    status[n] = true;
                }
            }).start();
        }

        while(true) {

            boolean isDone = true;

            for(int i = 0; i < 255; ++i) {
                if(status[i] == false) {
                    isDone = false;
                    break;
                }
            }

            if(isDone) break;
        }

        return reachable;
    }

    public static String getSubnetAddr() {
        int raw = ((WifiManager)Core.activity.getSystemService(Context.WIFI_SERVICE)).getDhcpInfo().gateway;

        return String.format(
                "%d.%d.%d",
                (raw & 0xff),
                (raw >> 8 & 0xff),
                (raw >> 16 & 0xff));
    }
}
