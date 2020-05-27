package com.lbynet.connect.backend.frames;

import com.lbynet.connect.backend.networking.FileRecvStreamer;

import java.util.ArrayList;

public interface FileReceiveListener{
    void onFileReceive(String senderName,ArrayList<FileRecvStreamer> streams);
}
