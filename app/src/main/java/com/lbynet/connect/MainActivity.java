package com.lbynet.connect;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;

import com.lbynet.connect.backend.Core;
import com.lbynet.connect.backend.NetTools;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Core.activity = this;

        String temp = "";

        for(String line : NetTools.getAllDeviceIPs()) {
            temp += line + "\n";
        }


        ((TextView)findViewById(R.id.main_content_box)).setText(temp);
    }

}
