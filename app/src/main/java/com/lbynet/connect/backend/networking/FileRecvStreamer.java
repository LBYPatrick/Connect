package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileRecvStreamer extends FileStreamer {

    final public static int RW_BUFFER_SIZE = 8192;

    private String filename_,
            targetDirectory_;
    private Socket socket_;
    private int port_;

    public FileRecvStreamer(String filename,String targetDirectory, int port) {
        filename_ = filename;
        targetDirectory_ = targetDirectory;
        port_ = port;
    }

    @Override
    void run() {
        try {

            socket_ = new ServerSocket(port_).accept();

            status = Status.WORKING;

            InputStream in = socket_.getInputStream();
            File file = new File(targetDirectory_ + "/" + filename_);
            byte [] buffer = new byte[8192];

            FileOutputStream out = new FileOutputStream(file);

            SAL.print("File " + filename_ + " receiving...");

            while(!socket_.isClosed()) {

                int bytesRead = in.read(buffer);

                if(bytesRead != -1) {
                    out.write(Utils.getTrimedData(buffer, bytesRead));
                }
                else {
                    status = Status.SUCCESS;
                    out.close();
                    in.close();
                    SAL.print("File " + filename_ + " received.");
                    return;
                }
            }

            status = Status.BAD_NETWORK;

        } catch(Exception e) {
            status = Status.BAD_GENERAL;
            e.printStackTrace();
        }
    }
}

