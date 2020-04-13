package com.lbynet;


import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.DataInputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class AACStreamer {

    static Socket socket_;
    static Thread mainThread;
    static boolean isBusy = false;


    static SourceDataLine speaker;

    public static void init(Socket socket) throws Exception {

        socket_ = socket;

        speaker = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class,
                                                                         new AudioFormat(44100,
                                                                                 16,
                                                                                 1,
                                                                                 true,
                                                                                 false)));


    }

    public static void start() {
        isBusy = true;

        SAL.print("STARTING");

        mainThread = new Thread(() -> {

            try {

                ADTSDemultiplexer demuxer = new ADTSDemultiplexer(socket_.getInputStream());
                Decoder decoder = new Decoder(demuxer.getDecoderSpecificInfo());
                byte[] frame;
                SampleBuffer decodeBuffer = new SampleBuffer();

                while (isBusy) {

                    if ((frame = demuxer.readNextFrame()) != null) {

                        decoder.decodeFrame(frame, decodeBuffer);

                        byte[] data = decodeBuffer.getData();

                        System.out.println(data);

                        speaker.write(data,0,data.length);

                    }
                }

                speaker.close();

            } catch (Exception e) {
                SAL.printException(e);
            }

        });

        mainThread.start();
    }

    public void stop() {

        if(!isBusy) return;

        isBusy = false;
    }


}
