package com.lbynet.connect;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Pairing;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SAL.activity = this;

        try {
            Pairing.start();
        } catch (Exception e) {
            SAL.printException(e);
        }

        new Thread( () -> {
            try {

            TextView view = findViewById(R.id.main_content_box);

            while (true) {
                String temp = "";

                for (Pairing.Device device : Pairing.getPairedDevices()) {
                    temp += device.ip + "\t" + device.name + "\n";
                }

                final String out = temp;

                if(out.compareTo(view.getText().toString()) != 0) {
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

    }

}
