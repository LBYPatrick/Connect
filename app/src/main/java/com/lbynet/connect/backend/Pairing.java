package com.lbynet.connect.backend;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;

//This is a singleton, so do not create instances of this class
public class Pairing {

    public static class Device  {
        public String ip, name;
        public Device() {}

    }

    private static InetAddress GROUP_ADDR;
    private static byte[] msg = ((String) "S").getBytes();

    final private static String MCAST_ADDR = "233.233.233.233";
    final private static int    MCAST_PORT = 2333,
                                BUFFER_LENGTH = 1000;


    private static ArrayList<Device> pairedDevices_ = new ArrayList<>();
    private static MulticastSocket socket_;
    private static boolean isStarted = false;
    private static Thread listenThread, sendThread;

    //Eager Initialization
    private static Pairing instance = new Pairing();

    private Pairing() {
        try {
            GROUP_ADDR = InetAddress.getByName(MCAST_ADDR);
            socket_ = new MulticastSocket(MCAST_PORT);

        } catch (Exception e) {
            Core.printException(e);
        }

    }

    /**
     *
     * @throws Exception
     */
    public static void start() throws Exception {

        if(isStarted) {
            stop();
            pairedDevices_.clear();
        }

        socket_.joinGroup(GROUP_ADDR);

        isStarted = true;

        sendThread = new Thread(() -> {
            try {
                while (isStarted) {
                    socket_.send(new DatagramPacket(msg, msg.length, GROUP_ADDR, MCAST_PORT));
                    Thread.sleep(1000);
                }
            } catch(Exception e) {
                Core.printException(e);
            }
        });

        listenThread = new Thread(() -> {
            try {
                while (isStarted) {

                    DatagramPacket dp = new DatagramPacket(new byte[BUFFER_LENGTH], BUFFER_LENGTH);
                    socket_.receive(dp);

                    if(dp.getLength() > 0) {
                        Device d = new Device();
                        d.ip = dp.getAddress().getHostAddress();
                        d.name = dp.getAddress().getHostName();

                        boolean isExistingDevice = false;

                        for(Device i : pairedDevices_) {
                            if(i.ip == d.ip) {
                                isExistingDevice = true;
                                break;
                            }
                        }

                        if(!isExistingDevice) {
                            pairedDevices_.add(d);
                        }
                    }
                }
            } catch (Exception e) {
                Core.printException(e);
            }
        });

        listenThread.start();
        sendThread.start();
    }

    public static ArrayList<Device> getPairedDevices() {
        return pairedDevices_;
    }

    public static void stop() throws Exception{
        isStarted = false;
        listenThread.interrupt();
        sendThread.interrupt();

        //Block until the thread is dead -- should take no time
        while(!listenThread.isInterrupted() || !sendThread.isInterrupted()) {};

        socket_.leaveGroup(GROUP_ADDR);
    }


    //Brute-force pinging -- doesn't work under some networks
    public static ArrayList<String> getAllDeviceIPs() {

        String gateway = getSubnetAddr();
        ArrayList<String> reachable = new ArrayList<>();

        boolean[] status = new boolean[255];

        Arrays.fill(status, false);

        for (int i = 0; i < 255; ++i) {

            //Java is stupid
            final int n = i;

            new Thread(() -> {

                String addr = gateway + "." + (n + 1);

                try {
                    if (InetAddress.getByName(addr).isReachable(200)) {
                        reachable.add(addr);
                    }
                } catch (Exception e) {
                    //Shhhh
                } finally {
                    status[n] = true;
                }
            }).start();
        }

        while (true) {

            boolean isDone = true;

            for (int i = 0; i < 255; ++i) {
                if (status[i] == false) {
                    isDone = false;
                    break;
                }
            }

            if (isDone) break;
        }

        return reachable;
    }

    public static String getSubnetAddr() {
        int raw = ((WifiManager) Core.activity.getSystemService(Context.WIFI_SERVICE)).getDhcpInfo().gateway;

        return String.format(
                "%d.%d.%d",
                (raw & 0xff),
                (raw >> 8 & 0xff),
                (raw >> 16 & 0xff));
    }
}
