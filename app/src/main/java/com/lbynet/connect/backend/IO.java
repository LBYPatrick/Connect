package com.lbynet.connect.backend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class IO {

    public static ArrayList<String> readFile(String filePath) {
        ArrayList<String> buffer = new ArrayList<>();

        readFile(filePath,buffer);

        return buffer;

    }

    public static boolean readFile(String filePath, ArrayList<String> buffer) {

        BufferedReader br;

        try {
            br = new BufferedReader(new FileReader(filePath));

            buffer.clear();

            String line;
            while((line = br.readLine()) != null) {
                buffer.add(line);
            }

            br.close();

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
