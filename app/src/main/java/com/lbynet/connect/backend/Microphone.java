package com.lbynet.connect.backend;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class Microphone {

    static MediaRecorder mr = new MediaRecorder();
    static boolean isTargetConfigured_ = false;
    static boolean isBusy = false;

    static {

        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
        mr.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //mr.setOutputFile();

        //mr.setAudioSamplingRate(16000);
    }

    public static void setOutput (FileDescriptor fd) {
        mr.setOutputFile(fd);
        isTargetConfigured_ = true;
    }

    public static void setOutput(String path) {
        mr.setOutputFile(path);
        isTargetConfigured_ = true;
    }

    public static void setOutput (Socket socket) throws Exception {

        mr.setOutputFile(ParcelFileDescriptor.fromSocket(socket).getFileDescriptor());
        isTargetConfigured_ = true;
    }

    public static boolean start() {
        try {
            if(!isTargetConfigured_) {
                setOutput("mic_output.mco");

            }
            SAL.print("Preparing");
            mr.prepare();
            SAL.print("Starting");
            mr.start();
            SAL.print("Start Successful");
        } catch (Exception e) {
            SAL.printException(e);
            return false;
        }
        return true;
    }

    public static void pause() {
        mr.pause();
    }

    public static void resume() {
        mr.resume();
    }

    public static void stop() {
        mr.stop();
    }

}
