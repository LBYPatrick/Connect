package com.lbynet.connect.backend.networking;

import com.lbynet.connect.backend.SAL;
import com.lbynet.connect.backend.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class FileSendStreamer extends FileStreamer {

    private String path_, ip_;
    private InputStream in_;
    private OutputStream out_;

    Timer timer = new Timer(this.getClass().getSimpleName());

    public FileSendStreamer(String path, String ip, int port) {
        path_ = path;
        port_ = port;
        ip_ = ip;
    }

    public FileSendStreamer(InputStream input, long fileSize, String ip, int port) {
        in_ = input;
        fileSize_ = fileSize;
        ip_ = ip;
        port_ = port;
    }

    @Override
    public void run() {
        try {

            Socket socket_ = new Socket(ip_, port_);
            byte[] buffer = new byte[RW_BUFFER_SIZE];

            SAL.print("Send Port Open");
            out_ = socket_.getOutputStream();

            //Legacy by-path mode
            if (path_ != null) {

                File file = new File(path_);

                if (file.length() == 0L) {
                    netStatus = NetStatus.BAD_FILE_INPUT;
                    return;
                } else {
                    fileSize_ = file.length();
                }

                in_ = new FileInputStream(file);
            }

            timer.start();

            boolean isSuccess = false;

            netStatus = NetStatus.WORKING;

            //Start Looping and send data
            while (!socket_.isClosed()) {

                int bytesRead = in_.read(buffer);

                boolean isDone  = false;

                if(bytesRead == -1) {
                    isDone = true;
                }
                else {
                    totalBytesRead += bytesRead;
                    out_.write(Utils.getTrimedData(buffer, bytesRead));
                }

                if(totalBytesRead == fileSize_) {
                    isSuccess = true;
                }

                if(isDone || isSuccess) {break;}
            }

            socket_.shutdownOutput();
            socket_.shutdownInput();

            out_.close();
            in_.close();

            if(isSuccess) {
                netStatus = NetStatus.SUCCESS;
                SAL.print(SAL.MsgType.VERBOSE,"FileSendStreamer","File sent. Progress: " + getProgress());
                return;
            }
            else { netStatus = NetStatus.BAD_NETWORK; }

        } catch (Exception e) {

            if(e instanceof SocketException) {
                netStatus = NetStatus.BAD_NETWORK;
            }
            else {
                netStatus = NetStatus.BAD_GENERAL;
            }
            SAL.print(e);
        }
    }

    public long getNumBytesSent() {
        return getNumBytesRead();
    }

}



