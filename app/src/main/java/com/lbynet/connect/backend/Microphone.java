package com.lbynet.connect.backend;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Microphone {

    final public static int SOURCE = MediaRecorder.AudioSource.MIC,
                     SAMPLE_RATE = 44100,
                     CHANNEL = AudioFormat.CHANNEL_IN_MONO,
                     RAW_ENCODING = AudioFormat.ENCODING_PCM_16BIT,
                     BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,CHANNEL,RAW_ENCODING);


    //static MediaRecorder mr = new MediaRecorder();
    static AudioRecord ar;
    static boolean isTargetConfigured_ = false;
    static boolean isBusy = false;
    //static MediaCodec codec;
    static Thread mainThread_;
    static InetAddress targetIp_;
    static int port_;


    static {
        ar = new AudioRecord(SOURCE,
                SAMPLE_RATE,
                CHANNEL,
                RAW_ENCODING,
                BUFFER_SIZE);

    }

    public static void initialize(String targetIp, int port) {
        try {
            targetIp_ = InetAddress.getByName(targetIp);
            port_ = port;
        } catch (Exception e) {
            SAL.print(e);
        }
    }

    public static void start() {

        if(targetIp_ == null || isBusy) {
            return;
        }

        isBusy = true;

        mainThread_ = new Thread(() -> {
            try {

                ar.startRecording();

                byte [] buffer = new byte[BUFFER_SIZE];
                int dataLength = 0;
                DatagramSocket socket = new DatagramSocket();

                while(isBusy) {

                    dataLength = ar.read(buffer,0,BUFFER_SIZE);

                    SAL.print(Integer.toString(dataLength));

                    if(dataLength != 0) {
                        socket.send(new DatagramPacket(buffer,dataLength,targetIp_,port_));
                    }
                }

                socket.close();

            }
            catch (Exception e) {
                SAL.print(e);
            }
        });

        mainThread_.start();
    }

    public static void pause() {
       //TODO: Implement this
    }

    public static void resume() {
        //TODO: Implement this
    }

    public static void stop() {
        isBusy = false;
    }

}
