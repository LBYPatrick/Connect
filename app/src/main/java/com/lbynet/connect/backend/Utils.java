package com.lbynet.connect.backend;

import java.util.Random;

public class Utils {

    final private static String LETTER_INDEX = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static Utils instance = new Utils();

    private static Random randomizer;

    private Utils() {
        randomizer = new Random();
    }

    public static String getRandomString(int size) {
        String out = "";

        for(int i = 0; i < size; ++i) {
            out += LETTER_INDEX.charAt(randomizer.nextInt(LETTER_INDEX.length()));
        }

        return out;
    }

    public static byte [] getTrimedData(byte[] rawBuffer, int offset, int length) {
        byte [] r = new byte [length];

        for(int i = 0; i < length; ++i) {
            r[i] = rawBuffer[offset + i];
        }

        return r;
    }
}
