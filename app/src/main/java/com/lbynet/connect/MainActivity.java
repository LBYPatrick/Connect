package com.lbynet.connect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.lbynet.connect.backend.Microphone;
import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Pairing;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity {

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);


            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            SAL.activity = this;

            /*
            Pairing.start();

            new Thread(() -> {
                try {

                    TextView view = findViewById(R.id.main_content_box);

                    while (true) {
                        String temp = "";

                        for (Pairing.Device device : Pairing.getPairedDevices()) {
                            temp += device.ip + "\t" + device.name + "\n";
                        }

                        final String out = temp;

                        if (out.compareTo(view.getText().toString()) != 0) {
                            runOnUiThread(() -> {
                                view.setText(out);
                            });
                        }

                        Thread.sleep(15);
                    }
                } catch (Exception e) {
                    SAL.printException(e);
                }
            }).start();

            */

            new Thread(() -> {
                try {
                    SAL.print("Starting Microphone");

                    //Socket s = new Socket(InetAddress.getByName("192.168.1.182"), 233);

                    //Microphone.setOutput(s);
                    Microphone.start();
                } catch(Exception e) {
                    SAL.printException(e);
                }
            }).start();

        } catch (Exception e) {
            SAL.printException(e);
        }

    }

}
