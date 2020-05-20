package com.lbynet.connect.backend.networking;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

public class FileRecvStreamer extends FileStreamer {

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
    public void run() {
        try {

            socket_ = new ServerSocket(port_).accept();

            netStatus = NetStatus.WORKING;

            InputStream in = socket_.getInputStream();
            File file = new File(targetDirectory_ + "/" + filename_);
            byte [] buffer = new byte[RW_BUFFER_SIZE];

            FileOutputStream out = new FileOutputStream(file);

            //SAL.print(SAL.MsgType.VERBOSE,"FileRecvStreamer","File " + filename_ + " receiving...");

            boolean isSuccess = false;

            while(!socket_.isClosed()) {

                int bytesRead = in.read(buffer);

                if(bytesRead == -1) {
                    isSuccess = true;
                    SAL.print("-1");
                    break;
                }
                else {
                    out.write(Utils.getTrimedData(buffer, bytesRead));
                }
            }

            out.close();
            in.close();

            if(isSuccess) {
                netStatus = NetStatus.SUCCESS;
                SAL.print(SAL.MsgType.VERBOSE,"FileRecvStreamer","File " + filename_ + " received.");
                return;
            }
            else {
                netStatus = NetStatus.BAD_NETWORK;
            }

        } catch(Exception e) {
            netStatus = NetStatus.BAD_GENERAL;
            SAL.print(e);
        }
    }
}

