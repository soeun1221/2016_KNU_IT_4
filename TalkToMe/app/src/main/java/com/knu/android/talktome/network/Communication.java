package com.knu.android.talktome.network;

import android.util.Log;

import com.knu.android.talktome.instance.Constant;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import data.OBJECT;


public class Communication extends Thread {

    private final String TAG = "Communication";

    public static Socket mySocket;
    private OutputStream output;
    private InputStream input;

    private OBJECT inObject;
    private OBJECT outObject;

    private CommunicationManager manager;

    public Communication(OBJECT object, CommunicationManager communicationManager) {
        this.outObject = object;
        this.manager = communicationManager;
        start();
    }

    public void output() {
        Log.i(TAG, "[Network] Waiting output");
        try {
            output = mySocket.getOutputStream();
            new ObjectOutputStream(output).writeObject(outObject);
            Log.d(TAG, "[Network] Output complete : " + outObject.getMessage());
            input();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void input() {
        Log.i(TAG, "[Network] Waiting input");
        try {
            input = mySocket.getInputStream();
            inObject = (OBJECT) new ObjectInputStream(input).readObject();
            if (inObject == null)
                Log.d(TAG, "[Network] Input complete : null");
            else
                Log.d(TAG, "[Network] Input complete : " + inObject.getMessage());
            disConnect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void disConnect() {

        try {
            output.close();
            input.close();
            mySocket.close();
            Log.d(TAG, "[Network] Socket disconnected");
        } catch (IOException e) {
            Log.e(TAG, "disConnect() : IO");
        } catch (NullPointerException e) {
            Log.e(TAG, "disConnect() : nullPointer");
        }
    }


    @Override
    public void run() {

        Thread run = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "[Network] Attempt to connect");
                try {
                    mySocket = new Socket();
                    mySocket.connect(new InetSocketAddress(Constant.IP, Constant.PORT), Constant.CONNECT_TIMEOUTRATE);
                    Log.d(TAG, "[Network] Socket is connected");
                    output();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Connecting Failed");
                }
            }

        });

        run.start();
        try {
            run.join();//run이 끝나야 AfterCommunication 실행
            manager.AfterCommunication(inObject);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
