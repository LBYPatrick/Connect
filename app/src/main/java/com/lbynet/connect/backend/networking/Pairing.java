package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Timer;
import com.lbynet.connect.backend.Utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;

//This is a singleton, so do not create instances of this class
public class Pairing {

    public static class Device {
        public String ip, uid;
        private Timer t;

        public Device() {
            t = new Timer();
            t.start();
        }

        public Device(String ip, String uid) {
            this();
            this.ip = ip;
            this.uid = uid;
        }

        public long getFreshness() {
            return t.getElaspedTimeInMs();
        }

        public void refresh() {
            t.start();
        }

    }

    private static InetAddress GROUP_ADDR;
    private static String selfUid_;
    private static byte[] msg_;

    final private static String MCAST_ADDR = "233.233.233.233";
    final private static int MCAST_PORT = 2333,
            BUFFER_LENGTH = 16384;

    private static String subnet_,
            selfIp_,
            selfName_;

    private static ArrayList<Device> pairedDevices_ = new ArrayList<>();
    private static MulticastSocket socket_;
    private static boolean isStarted = false;
    private static boolean isBusy = false;
    private static Thread listenThread, sendThread;

    //Eager Initialization
    private static Pairing instance = new Pairing();

    private Pairing() {
        try {
            GROUP_ADDR = InetAddress.getByName(MCAST_ADDR);
            socket_ = new MulticastSocket(MCAST_PORT);

            //TODO: Change this when you are porting this to other platforms
            selfUid_ = SAL.getDeviceName();
            msg_ = selfUid_.getBytes();

        } catch (Exception e) {
            SAL.print(e);
        }

    }

    /**
     * @throws Exception
     */
    public static void start() throws Exception {

        if (isStarted) {
            stop();
            pairedDevices_.clear();
        }

        socket_.joinGroup(GROUP_ADDR);

        isStarted = true;

        sendThread = new Thread(() -> {
            try {
                while (isStarted) {
                    socket_.send(new DatagramPacket(msg_, msg_.length, GROUP_ADDR, MCAST_PORT));
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                SAL.print(e);
            }
        });

        listenThread = new Thread(() -> {
            try {

                while (isStarted) {

                    DatagramPacket dp = new DatagramPacket(new byte[BUFFER_LENGTH], BUFFER_LENGTH);
                    socket_.receive(dp);

                    //Package Received
                    if (dp.getLength() > 0) {
                        Device d = new Device();
                        d.ip = dp.getAddress().getHostAddress();
                        d.uid = new String(Utils.getTrimedData(dp.getData(), dp.getOffset(), dp.getLength()));

                        if (d.uid.compareTo(selfUid_) != 0) {

                            boolean isExistingDevice = false;

                            for (Device i : pairedDevices_) {
                                if (i.uid.equals(d.uid)) {
                                    isExistingDevice = true;
                                    i.ip = d.ip;
                                    i.refresh();
                                    break;
                                }
                            }

                            if (!isExistingDevice) {
                                SAL.print("Device added: " + d.uid + "@" + d.ip);
                                //pairedDevices_.add(d);

                                boolean isDeviceAdded = false;
                                //Put the device in the right spot of the queue
                                for(int i = 1; i < pairedDevices_.size(); ++i) {
                                    if(d.uid.compareTo(pairedDevices_.get(i).uid) <= 0) {
                                        pairedDevices_.add(i - 1, d);
                                        isDeviceAdded = true;
                                        break;
                                    }
                                }

                                //If the device belongs to the bottom of the queue...
                                if(!isDeviceAdded) {
                                    pairedDevices_.add(d);
                                }


                            }
                        } else if (selfIp_ == null) {
                            selfIp_ = d.ip;
                        }
                    }
                }
            } catch (Exception e) {
                SAL.print(e);
            }
        });

        listenThread.start();
        sendThread.start();
    }

    public static ArrayList<Device> getPairedDevices() {

        return pairedDevices_;
    }

    public static void stop() throws Exception {
        isStarted = false;
        listenThread.interrupt();
        sendThread.interrupt();

        //Block until the thread is dead -- should take no time
        while (!listenThread.isInterrupted() || !sendThread.isInterrupted()) {
        }

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

    public static String getSelfName() {

        if (selfName_ == null) {
            return "";
        } else {
            return selfName_;
        }
    }

    public static String getSelfAddress() {
        if (selfIp_ == null) {
            return "";
        } else return selfIp_;
    }

    public static String getSubnetAddr() {

        if (subnet_ != null) {
            return subnet_;
        } else if (getSelfAddress().length() == 0) {
            return "";
        } else {
            subnet_ = getSelfAddress().substring(0, getSelfAddress().lastIndexOf("."));
            return subnet_;
        }

    }

    public static String getSelfUid() {
        return selfUid_;
    }
}
