package com.lbynet.connect;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.lbynet.connect.backend.Core;
import com.lbynet.connect.backend.Pairing;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Core.activity = this;

        new Thread( () -> {
            try {
            while (true) {
                String temp = "";

                for (Pairing.Device device : Pairing.getPairedDevices()) {
                    temp += device.ip + "\t" + device.name + "\n";
                }

                final String out = temp;


                runOnUiThread( () -> {
                    ((TextView) findViewById(R.id.main_content_box)).setText(out);
                });

                Thread.sleep(200);
            }
            } catch (Exception e) {
                Core.printException(e);
            }
        }).start();

    }

}
