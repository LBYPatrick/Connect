package com.lbynet.connect.backend.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.frames.NetCallback;
import com.lbynet.connect.backend.networking.FileReceiver;
import com.lbynet.connect.backend.networking.Pairing;

public class SystemManager {

    private static SystemManager instance = new SystemManager();
    private static boolean isReceiverGood = false;

    private SystemManager () {

    }

    public static void registerReceivers (Context context) {
        if(isReceiverGood) {
            return;
        }
        isReceiverGood = true;

        NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();

        //Wi-Fi State
        ((ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .registerNetworkCallback(request,new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        DataPool.isWifiConnected = true;
                        FileReceiver.restartLater();
                        try {
                            Pairing.start();
                            Pairing.onConnect();
                        } catch (Exception e) {
                            SAL.print(e);
                        }
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        DataPool.isWifiConnected = false;
                        Pairing.onLost();
                    }
                });

        //Pairing
        Pairing.setStatusCallback(new NetCallback() {
            @Override
            public void onConnect() {
                SAL.print("Pairing Possible");
            }

            @Override
            public void onLost() {
                SAL.print("Pairing Bad");
            }
        });
    }

    public static void setPowerSavingMode(boolean isTrue) {
        DataPool.isPowerSavingMode = isTrue;
    }

    public static void setInvisibleMode(boolean isTrue) {
        DataPool.isInvisibleMode = isTrue;
    }

}
