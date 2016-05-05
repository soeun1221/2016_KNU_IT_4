package com.knu.android.talktome.communication;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

//통신을 위한 스레드
public class Communication extends Thread {

    private final String TAG = "Communication";

    public static Socket mySocket;

//    private static final String IP = "192.168.10.3";
    private static final String IP = "54.152.85.123";
    private static final int PORT = 56788;
    public static final int CONNECT_TIMEOUTRATE = 2500;

    private OutputStream output;
    private InputStream input;

    private byte[] inObject;
    private byte[] outObject;

    private DataOutputStream writer;

    private CommunicationManager manager;

    public Communication(byte[] object) {
        this.outObject = object;
        this.inObject = new byte[1024];
        start();
    }

    public Communication(byte[] object, CommunicationManager communicationManager) {
        this.outObject = object;
        this.inObject = new byte[1024];
        this.manager = communicationManager;
        start();
    }

    public void output() {
        //서버로 OBJECT 객체를 전송한다
        Log.i(TAG, "[Communy] Waiting output");
        try {
            output = mySocket.getOutputStream();
            writer = new DataOutputStream(output);
            Log.d(TAG, "output: "+outObject.length);
            writer.write(outObject);
            writer.flush();
            Log.d(TAG, "output: " + outObject.length);

            input();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void input() {
        //서버에서 OBJECT 객체를 읽어온다
        Log.i(TAG, "[Communy] Waiting input");
        try {
            input = mySocket.getInputStream();
            if(input == null){
                Log.d(TAG, "input: NULL");
            } else if(inObject == null){
                Log.d(TAG, "inObject: NULL");
            }
            new DataInputStream(input).read(inObject);
            Log.d(TAG, "input: " + inObject.toString());
            if (inObject == null)
                Log.d(TAG, "[Communy] Input complete : null");
            else
                Log.d(TAG, "[Communy] Input complete : " + inObject.toString());
            disConnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disConnect() {
        //연결해제
        try {
            output.close();
            input.close();
            mySocket.close();
            Log.d(TAG, "[Communy] Socket disconnected");
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
                Log.i(TAG, "[Communy] Attempt to connect");
                try {
                    mySocket=new Socket();
                    mySocket.connect(new InetSocketAddress(IP, PORT),CONNECT_TIMEOUTRATE);
                    //소켓 생성 후 CONSTANT 클래스에 있는 주소로 연결, timeout 시간이 지나면 fail
                    Log.d(TAG, "[Communy] Socket is connected");
                    output();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Connecting Failed");
                }
            }

        });
        //스레드 돌려서 서버에 접속하고 종료한다.
        run.start();
        try {
            run.join();//run이 끝나야 AfterCommunication 실행
            if(manager != null)
                manager.AfterCommunication(inObject);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
