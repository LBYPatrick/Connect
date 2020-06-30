package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.core.DataPool;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Timer;
import com.lbynet.connect.backend.Utils;
import com.lbynet.connect.backend.frames.NetCallback;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;

//This is a singleton, so do not create instances of this class
public class Pairing {

    public static class Device {
        public String ip, deviceName, uid;
        private Timer t;
        private boolean isDead = false;

        public Device() {
            t = new Timer();
            t.start();
        }

        public Device(String ip, String deviceName, String uid) {
            this();
            this.ip = ip;
            this.deviceName = deviceName;
            this.uid = uid;
        }

        public void kill() {
            isDead = true;
        }

        public boolean isDead() {
            return isDead;
        }

        public long getFreshness() {

            return t.getElaspedTimeInMs();
        }

        public void refresh() {
            t.start();
        }
    }

    //Eager Initialization
    private static Pairing instance = new Pairing();

    private static InetAddress GROUP_ADDR;
    private static byte[] msg_;
    private static String selfUid_,
                          subnet_,
                          selfIp_,
                          selfName_;
    private static ArrayList<Device> pairedDevices_ = new ArrayList<>(),
                                     filteredDevices_ = new ArrayList<>();
    private static MulticastSocket socket_;

    private static boolean isStarted = false,
            isBusy = false,
            isInvisible_ = false,
            isListChanged = true;

    private static Thread listenThread,
            sendThread;
    private static Runnable sendTask,
            listenTask;
    private static NetCallback statusCallback_ = new NetCallback();

    final private static String MCAST_ADDR = "233.233.233.233";
    final private static int MCAST_PORT = 2333,
                             BUFFER_LENGTH = 128;
    final public static String TAG = Pairing.class.getSimpleName();


    /**
     * Initialize Runnable objects for listen and send threads
     */
    private Pairing() {

        try {
            GROUP_ADDR = InetAddress.getByName(MCAST_ADDR);
            selfUid_ = Utils.getRandomString(4);

        } catch (Exception e) {
            SAL.print(e);
        }

        sendTask = () -> {
            while (!isStarted) {
                Utils.sleepFor(200);
            }

            while (true) {

                //Pause sending if the app is hibernated (this variable is manually set in code)
                if(DataPool.isAppHiberated) {
                    Utils.sleepFor(10);
                    continue;
                }

                if (!isInvisible_) {
                    try {
                        socket_.send(new DatagramPacket(msg_, msg_.length, GROUP_ADDR, MCAST_PORT));
                    } catch (Exception e) {

                        if (e instanceof IOException) {
                            //SAL.print(SAL.MsgType.VERBOSE,"Pairing","Send thread hibernating...");
                            Utils.sleepFor(1000);
                        } else {
                            SAL.print(e);
                        }
                    }
                }
                Utils.sleepFor(300);
            }
        };

        listenTask = () -> {

            while (!isStarted) {
                Utils.sleepFor(200);
            }

            while (true) {

                //Pause receiving if the app is hibernated (this variable is manually set in code)
                if(DataPool.isAppHiberated) {
                    Utils.sleepFor(10);
                    continue;
                }


                boolean isPackageGood = true;
                DatagramPacket dp = new DatagramPacket(new byte[BUFFER_LENGTH], BUFFER_LENGTH);

                try {
                    socket_.receive(dp);
                } catch (Exception e) {
                    SAL.print(e);
                    isPackageGood = false;
                }

                if (!isPackageGood) continue;

                //Package Received
                if (dp.getLength() > 0) {

                    String data = new String(Utils.getTrimedData(dp.getData(), dp.getOffset(), dp.getLength()));

                    //Read basic information
                    Device d = new Device();
                    d.ip = dp.getAddress().getHostAddress();
                    d.uid = data.substring(data.indexOf('\n') + 1);
                    d.deviceName = data.substring(0, data.indexOf('\n'));

                    //If device name is too long, process it
                    if (d.deviceName.length() > 15) {
                        d.deviceName = d.deviceName.substring(0, 16) + "...";
                    }

                    if (!d.uid.equals(selfUid_)) {

                        boolean isExistingDevice = false;

                        //Match device by UID
                        for (Device i : pairedDevices_) {
                            if (i.uid.equals(d.uid)) {
                                isExistingDevice = true;
                                i.ip = d.ip;

                                //If this device "aged" before and "revived" now, notify getFilteredDevices()
                                if(i.getFreshness() > 2500) {
                                    isListChanged = true;
                                }

                                i.refresh();
                                break;
                            }
                        }

                        if (isExistingDevice) {
                            /**
                             * If the device happens to be the same one but changed UID
                             * Which means that the app has been restarted
                             */

                            for (Device i : pairedDevices_) {
                                if (d.ip.equals(i.ip) && !i.uid.equals(d.uid)) {

                                    SAL.print(TAG,d.deviceName + " rejoined.");

                                    i.kill();
                                    pairedDevices_.remove(i);
                                    isListChanged = true;
                                    break;
                                }
                            }
                        }

                        if (!isExistingDevice) {

                            SAL.print("Device added: \n"
                                    + "\tName: " + d.deviceName + "\n"
                                    + "\tUID: " + d.uid + "\n"
                                    + "\tIP Address: " + d.ip + "\n");

                            pairedDevices_.add(d);
                            /*
                            boolean isDeviceAdded = false;
                            //Put the device in the right spot of the queue
                            for (int i = 1; i < pairedDevices_.size(); ++i) {
                                if (d.uid.compareTo(pairedDevices_.get(i).uid) <= 0) {
                                    pairedDevices_.add(i - 1, d);
                                    isDeviceAdded = true;
                                    break;
                                }
                            }

                            //If the device belongs to the bottom of the queue...
                            if (!isDeviceAdded) {
                                pairedDevices_.add(d);
                            }
                             */

                            isListChanged = true;
                        }
                    } else if (selfIp_ == null) {

                        DataPool.isPairingReady = true;
                        selfIp_ = d.ip;
                        selfName_ = d.deviceName;

                        DataPool.isPairingReady = true;
                        statusCallback_.onConnect();
                    }
                    Utils.sleepFor(10);
                }
            }
        };

    }

    /**
     * Start the pairing service, device list can be obtained by getPairedDevices()
     * @throws Exception
     */
    public static void start() throws Exception {

        if (isStarted) {
            return;
        }

        msg_ = (SAL.getDeviceName() + "\n" + selfUid_).getBytes();

        SAL.print(SAL.MsgType.VERBOSE,TAG,"Started with name " + SAL.getDeviceName() + " and uid " + selfUid_);

        joinGroup();

        isStarted = true;

        listenThread = new Thread(listenTask);
        sendThread = new Thread(sendTask);

        listenThread.start();
        sendThread.start();
    }

    /**
     * Manually stop the pairing service.
     */
    public static void stop() {

        isStarted = false;

        try {

            //Interrupt listen and send threads
            listenThread.interrupt();
            sendThread.interrupt();

            //Reset variables
            selfIp_ = null;
            selfName_ = null;
            subnet_ = null;

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                SAL.print("Pairing Interrupted");
            } else {
                SAL.print(e);
            }
        }
        pairedDevices_.clear();

        //Block until the thread is dead -- should take no time
        while (!listenThread.isInterrupted() || !sendThread.isInterrupted()) {
        }
    }

    /**
     * called when recovering from network disconnection (manually called by user)
     * (Called by SystemManager in this app)
     */
    public static void onRecover() {

        selfUid_ = Utils.getRandomString(4);
        msg_ = (SAL.getDeviceName() + "\n" + selfUid_).getBytes();

        joinGroup();

    }

    /**
     * Called when LAN connection is lost (manually called by user)
     * (Called by SystemManager in this app)
     */
    public static void onLost() {

        //Reset variables
        selfIp_ = null;
        selfName_ = null;
        subnet_ = null;

        DataPool.isPairingReady = false;
        statusCallback_.onLost();
    }

    /**
     * Manually restart the pairing service. Do not call this method UNLESS stuff keeps failing/User ID is changed.
     */
    public static void restart() {

        if (isStarted) {
            stop();
        }
        try {
            start();
        } catch (Exception e) {
            SAL.print(e);
        }
    }

    /**
     *
     * @return An unfiltered list of devices (i.e. including those that are not "fresh" -- probably closed the app)
     */
    public static ArrayList<Device> getPairedDevices() {
        return pairedDevices_;
    }


    public static boolean getFilteredDevices(ArrayList<Device> buffer) {
        return getFilteredDevices(buffer,2500);
    }

    /**
     * Get a filtered list of paired devices (i.e. Alive and fresh enough)
     * @param buffer the list buffer for us to write in
     * @return Whether the list written is new
     *         (Allowing user to optimize their code as needed. For example: RecyclerView)
     */
    public static boolean getFilteredDevices(ArrayList<Device> buffer, int freshnessInMs) {
        boolean isUpdateNeeded = false;

        if(isListChanged) {

            //SAL.print(SAL.MsgType.VERBOSE,TAG,"List changed!");
            isUpdateNeeded = true;

            filteredDevices_.clear();

            for(Device i : pairedDevices_) {
                if((!i.isDead()) && i.getFreshness() < freshnessInMs) {
                    SAL.print(SAL.MsgType.VERBOSE,TAG,i.deviceName + " added.");
                    filteredDevices_.add(i);
                }
            }
        }

        //If there has been no update to the list of devices, filter out the old devices
        else {

            //SAL.print(SAL.MsgType.VERBOSE,TAG,"On else");

            for (Device i : filteredDevices_) {
                if (i.getFreshness() > freshnessInMs) {
                    SAL.print(SAL.MsgType.VERBOSE,TAG,i.deviceName + " removed because of aging.");
                    filteredDevices_.remove(i);
                    isUpdateNeeded = true;
                }
            }

        }

        //Detect difference
        if(filteredDevices_.size() != buffer.size()) {
            isUpdateNeeded = true;
        }
        else {
            for(int i = 0; i < filteredDevices_.size(); ++i) {
                if(filteredDevices_.get(i) != buffer.get(i)) {
                    isUpdateNeeded = true;
                    break;
                }
            }
        }

        if(isUpdateNeeded) {
            buffer.clear();
            buffer.addAll(filteredDevices_);
        }

        //Resets the change notification flag
        isListChanged = false;

        return isUpdateNeeded;
    }

    public static void requestForceUpdate() {
        isListChanged = true;
    }

    /**
     * Join the multicast group for send and receiving beacons.
     */
    public static void joinGroup() {
        try {
            socket_ = new MulticastSocket(MCAST_PORT);
            socket_.joinGroup(GROUP_ADDR);
        } catch (Exception e) {
            SAL.print(e);
        }
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
            return null;
        } else return selfIp_;
    }

    public static String getSubnetAddr() {

        if (subnet_ != null) {
            return subnet_;
        } else if (getSelfAddress() == null) {
            return null;
        } else {
            subnet_ = getSelfAddress().substring(0, getSelfAddress().lastIndexOf("."));
            return subnet_;
        }

    }

    public static boolean isSocketAlive() {
        return !socket_.isClosed();
    }

    public static String getSelfUid() {
        return selfUid_;
    }

    public static boolean isStarted() {
        return isStarted;
    }

    public static void setInvisible(boolean isInvisible) {
        isInvisible_ = isInvisible;
    }

    public static void setStatusCallback(NetCallback statusCallback) {
        statusCallback_ = statusCallback;
    }

}
