package com.knu.android.talktome.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.knu.android.talktome.MainActivity;
import com.knu.android.talktome.communication.Communication;
import com.knu.android.talktome.communication.CommunicationManager;

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
    byte[] b;

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

            RandomAccessFile f = new RandomAccessFile(filename, "r");
            b = new byte[(int) f.length()];
            f.read(b);
            Log.d(TAG, "write: " + b.length);

            String length = new String("" + b.length);
            //length전송
            new Communication(length.getBytes(), new CommunicationManager() {
                @Override
                public void AfterCommunication(byte[] getInObject) {
                    try {
                        //2개가 메시지
                        byte[] msgBytes = new byte[2];
                        for (int i = 0; i < 2; i++)
                            msgBytes[i] = getInObject[i];
                        Log.d(TAG, "AfterCommunication: "+new String(getInObject));
                        handler.sendMessage(Message.obtain(handler, Integer.parseInt(new String(msgBytes)),
                                getInObject));
                    } catch (NullPointerException e) {
                        handler.sendEmptyMessage(-1);
                    }
                }
            });
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
            speechFile.write(buffer.array());
        } catch (IOException e) {
            System.err.println("Can't write file : " + filename);
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
//                    Log.d(TAG, "handleMessage: " + b.length);
                    new Communication(b);
                    break;
                case 2:
                    Log.d(TAG, "handleMessage: for Result:" + msg.obj);
                    byte[] lenBytes = new byte[3];
                    for (int i = 0; i < 3; i++)
                        lenBytes[i] = ((byte[]) msg.obj)[i + 2];
                    int len = Integer.parseInt(new String(lenBytes));

                    byte[] result = new byte[len];
                    for (int i = 0; i < len; i++)
                        result[i] = ((byte[]) msg.obj)[i + 5];
                    //카운트 다운이 끝났음을 알림
                    Intent intent = new Intent(MainActivity.ACTION_IDENTIFY_SPEAKER);
                    intent.putExtra("speaker", new String(result));
                    context.sendBroadcast(intent);

                    break;
                default:
                    Log.d(TAG, "handleMessage: " + "default");
            }
        }
    };
}
