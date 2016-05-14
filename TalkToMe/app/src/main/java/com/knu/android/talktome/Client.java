package com.knu.android.talktome;

import android.os.AsyncTask;
import android.util.Log;

import com.knu.android.talktome.instance.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by star on 2016. 5. 13..
 */
public class Client extends AsyncTask<Void, MessageItem, Void> {
    private static final String TAG = "Client";

    private Socket socket;
    private String speaker;
    private MessageAdapter messageAdapter;

    private boolean isRunning = true;

    public Client(String speaker, MessageAdapter messageAdapter) {
        this.speaker = speaker;
        this.messageAdapter = messageAdapter;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            if (socket == null) {
                socket = new Socket();// 소켓생성하고 초기값으로
                socket.connect(new InetSocketAddress(Constant.IP, Constant.PORT), Constant.CONNECT_TIMEOUTRATE);
                Log.d(TAG, "onCreate: Socket is Connected");
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                //사용자가 입력한 데이터를 JVM이 소켓의 바이트 스트림 단위로 서버에 보내기 위해서 PrintWriter를사용
                writer.println(speaker);//사용자 이름을 서버로 출력
                writer.flush();//만약에 메모리 버퍼에 남아있는게 있으면 한번에 비워라
            }
            receive();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Socket Connecting failed!");
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(MessageItem... values) {
        MainActivity.messageAdapter.add(values[0]);
        MainActivity.messageAdapter.notifyDataSetChanged();
        super.onProgressUpdate(values);
    }

    public void send(String message) {
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.println(message);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receive() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // 서버가 보내온 소켓의 바이트 스트림을 다시 char타입의 InputSTreamReader 그것을라인단위로 읽기 위해서
            // BufferedReader로 감싼다

            while (isRunning) {
                String str = reader.readLine();// str변수에 라인단위로 저장
                Log.d(TAG, "receive: "+str);

                if(isCancelled()){
                    Log.d(TAG, "receive: Cancel");
                    break;
                }
                if (str == null) {// null이면
                    Log.d(TAG, "receive: out");
                    break;// 종료
                }

                publishProgress(new MessageItem(str));
            }

            Log.d(TAG, "receive: finish");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "onCancelled: call cancelled");
        isRunning = false;
        super.onCancelled();
    }
}
