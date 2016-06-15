package com.knu.android.talktome.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Message;
import android.util.Log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class AudioWriterPCM {

    private static String TAG = "AudioWriterPCM";

    String path;
    String filename;
    FileOutputStream speechFile;
    Context context;

    public AudioWriterPCM(String path, Context context) {
        this.path = path;
        this.context = context;
    }

    public void open(String sessionId) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        filename = directory + "/" + sessionId + ".pcm";
        try {
            speechFile = new FileOutputStream(new File(filename));
        } catch (FileNotFoundException e) {
            System.err.println("Can't open file : " + filename);
            speechFile = null;
        }
    }

    public void close() {
        if (speechFile == null)
            return;

        try {
            speechFile.close();
        } catch (IOException e) {
            System.err.println("Can't close file : " + filename);
        }
    }

    public void write(short[] data) {
        if (speechFile == null)
            return;

        ByteBuffer buffer = ByteBuffer.allocate(data.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < data.length; i++) {
            buffer.putShort(data[i]);
        }
        buffer.flip();

        try {
            Log.d(TAG, "write: "+buffer.array().length);
            speechFile.write(buffer.array());
        } catch (IOException e) {
            System.err.println("Can't write file : " + filename);
        }
    }

    public byte[] getBytes() {
        if (speechFile == null)
            return null;
        RandomAccessFile file = null;
        byte[] wavefile = null;
        try {
            file = new RandomAccessFile(filename, "r");
            wavefile = new byte[(int)file.length()];
            file.read(wavefile);
            Log.d(TAG, "getBytes: "+wavefile.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wavefile;
    }
}
