package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.Utils;

import java.io.*;
import java.net.Socket;

public class FileSendStreamer extends FileStreamer {

    private String path_, ip_;
    private int port_;
    private long numCyclesRead_ = 0;
    private long fileSize_ = 0;

    public FileSendStreamer(String path,String ip, int port) {
        path_ = path;
        port_ = port;
        ip_ = ip;
    }

    @Override
    public void run() {
        try {

            netStatus = NetStatus.WORKING;

            Socket socket_ = new Socket(ip_, port_);
            OutputStream out = socket_.getOutputStream();

            File file = new File(path_);
            byte [] buffer = new byte[RW_BUFFER_SIZE];

            if(file.length() == 0L) {
                netStatus = NetStatus.BAD_FILE_INPUT;
                return;
            }else {
                fileSize_ = file.length();
            }

            FileInputStream in  = new FileInputStream(file);

            while(!socket_.isClosed()) {

                int bytesRead = in.read(buffer);

                numCyclesRead_ += 1;

                if(bytesRead != -1) {
                    out.write(Utils.getTrimedData(buffer, bytesRead));
                }
                else {
                    out.close();
                    in.close();
                    netStatus = NetStatus.SUCCESS;
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

    public double getProgress() {
        if(netStatus != NetStatus.WORKING && netStatus != NetStatus.IDLE) {
            return 1;
        }
        else {
            double bottom = (fileSize_ == 0) ? 1 : fileSize_;
            double top = RW_BUFFER_SIZE * numCyclesRead_;

            double result = top / bottom;

            return result > 1 ? 0.99 : result;
        }
    }
}



