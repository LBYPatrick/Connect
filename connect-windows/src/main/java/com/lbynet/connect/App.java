package com.lbynet.connect;

import com.lbynet.connect.backend.*;
import com.lbynet.connect.backend.networking.*;
import com.lbynet.connect.backend.networking.Pairing;

public class App {
    public static void main(String[] args) throws Exception {

        try {
            Pairing.start();
            FileReceiver.start();

            while(true) {
                Thread.sleep(10000);
            }

        } catch (Exception e) {
            SAL.print(e);
        }


    }
}