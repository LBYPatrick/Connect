package com.lbynet;


import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.aac.gain.GainControl;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class AudioStreamer {

    //static Socket socket_;

    static int listenPort_ = 0;
    static Thread mainThread;
    static boolean isBusy = false;


    static SourceDataLine speaker;

    public static void init(int listenPort) throws Exception {

        listenPort_ = listenPort;

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

                ((FloatControl) speaker.getControl(FloatControl.Type.MASTER_GAIN)).setValue(100.0f);
                speaker.start();
                speaker.open(new AudioFormat(44100,
                        16,
                        1,
                        true,
                        false));

                byte [] buffer = new byte[3584];

                DatagramPacket packetBuffer = new DatagramPacket(buffer,3584);

                int length = 0;

                DatagramSocket socket = new DatagramSocket(listenPort_);

                /*
                ADTSDemultiplexer demuxer = new ADTSDemultiplexer(socket_.getInputStream());
                Decoder decoder = new Decoder(demuxer.getDecoderSpecificInfo());
                byte[] frame;
                SampleBuffer decodeBuffer = new SampleBuffer();
                 */

                while (isBusy) {

                    socket.receive(packetBuffer);

                    length = packetBuffer.getLength();

                    if(length != 0) {

                        SAL.print(Integer.toString(length));
                        speaker.write(packetBuffer.getData(),0,length);
                        speaker.drain();
                    }

                    /*
                    if ((frame = demuxer.readNextFrame()) != null) {

                        decoder.decodeFrame(frame, decodeBuffer);

                        byte[] data = decodeBuffer.getData();

                        System.out.println(data);

                        speaker.write(data,0,data.length);

                    }
                    */

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
