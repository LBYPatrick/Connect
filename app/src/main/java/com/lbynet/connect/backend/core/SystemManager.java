package com.lbynet.connect.backend.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.StrictMode;

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

                            if(!Pairing.isStarted()) {
                                Pairing.start();
                            }
                            else {
                                Pairing.onRecover();
                            }

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

    public static void overrideFileSafety() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
        }
    }

    public static void setPowerSavingMode(boolean isTrue) {
        DataPool.isPowerSavingMode = isTrue;
    }

    public static void setInvisibleMode(boolean isTrue) {
        DataPool.isInvisibleMode = isTrue;
    }

}
