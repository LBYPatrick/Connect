package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import java.io.*;
import java.net.Socket;

public class FileSendStreamer extends FileStreamer {

    final public static int RW_BUFFER_SIZE = 8192;

    private String path_, ip_;
    private int port_;

    public FileSendStreamer(String path,String ip, int port) {
        path_ = path;
        port_ = port;
        ip_ = ip;
    }

    @Override
    public void run() {
        try {

            Socket socket_ = new Socket(ip_, port_);
            OutputStream out = socket_.getOutputStream();

            File file = new File(path_);
            byte [] buffer = new byte[8192];

            if(file.length() == 0L) {
                netStatus = NetStatus.BAD_FILE_INPUT;
                return;
            }

            FileInputStream in  = new FileInputStream(file);

            while(!socket_.isClosed()) {

                int bytesRead = in.read(buffer);

                if(bytesRead != -1) {
                    out.write(Utils.getTrimedData(buffer, bytesRead));
                }
                else {
                    netStatus = NetStatus.SUCCESS;
                    out.close();
                    in.close();
                    SAL.print(SAL.MsgType.VERBOSE,"FileSendStreamer","File " + Utils.getFilename(path_) + " sent.");
                    return;
                }
            }

            netStatus = NetStatus.BAD_NETWORK;

        } catch(Exception e) {
            netStatus = NetStatus.BAD_GENERAL;
            SAL.print(e);
        }
    }
}



