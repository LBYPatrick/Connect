package com.lbynet.connect.backend.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.frames.NetCallback;
import com.lbynet.connect.backend.networking.FileReceiver;
import com.lbynet.connect.backend.networking.Pairing;
import com.lbynet.connect.frontend.Visualizer;

public class SystemManager {

    final public static String TAG = SystemManager.class.getSimpleName();
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

        try {
            Pairing.start();
        } catch (Exception e) {
            SAL.print(e);
        }

        //Wi-Fi State
        ((ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .registerNetworkCallback(request,new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        SAL.print(SAL.MsgType.VERBOSE,TAG,"Wi-Fi Connected");
                        super.onAvailable(network);
                        DataPool.isWifiConnected = true;
                        if(DataPool.isLauncherActvitiy) Visualizer.updateFsnStatusOnLauncher((AppCompatActivity) context);
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        SAL.print(SAL.MsgType.VERBOSE,TAG,"Wi-Fi Lost");
                        DataPool.isWifiConnected = false;
                        if(DataPool.isLauncherActvitiy) Visualizer.updateFsnStatusOnLauncher((AppCompatActivity) context);
                    }
                });

        //Pairing
        Pairing.setStatusCallback(new NetCallback() {
            @Override
            public void onConnect() {
                SAL.print("Pairing Possible");
                new Thread( () -> {
                    if(DataPool.isLauncherActvitiy) Visualizer.updateFsnStatusOnLauncher(DataPool.activity);
                }).start();
            }

            @Override
            public void onLost() {
                SAL.print("Pairing bad");

                new Thread( () -> {
                    if(DataPool.isLauncherActvitiy)Visualizer.updateFsnStatusOnLauncher(DataPool.activity);
                }).start();
            }
        });
    }

    public static void overrideFileSafety() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
        }
    }

}
