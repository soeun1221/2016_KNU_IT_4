package com.knu.android.talktome;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.knu.android.talktome.instance.Constant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import data.OBJECT;

/**
 * Created by star on 2016. 5. 13..
 */
public class Client extends AsyncTask<Void, MessageItem, Void> {

    private static final String TAG = "Client";

    private static Client client;

    private Socket socket;
    private String speaker;
    private MessageAdapter messageAdapter;

    private boolean isRunning = true;

    private Context context;

    private Client(String speaker) {
        this.speaker = speaker;
    }

    public static Client getInstance(String speaker) {
        if (client == null) {
            client = new Client(speaker);
        }
        return client;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setMessageAdapter(MessageAdapter messageAdapter) {
        this.messageAdapter = messageAdapter;
    }


    @Override
    protected Void doInBackground(Void... params) {
        try {
            if (socket == null) {
                socket = new Socket();// 소켓생성하고 초기값으로
                socket.connect(new InetSocketAddress(Constant.IP, Constant.PORT), Constant.CONNECT_TIMEOUTRATE);
                Log.d(TAG, "onCreate: Socket is Connected");
            }
            receive();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Socket Connecting failed!");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(MessageItem... values) {
        MainActivity.messageAdapter.add(values[0]);
        MainActivity.messageAdapter.notifyDataSetChanged();
        super.onProgressUpdate(values);
    }

    public void send(OBJECT outObject) {
        if (outObject.getMessage() == Constant.SEND_MESSAGE)
            Log.d(TAG, "send: " + outObject.getObject(1));
        OutputStream output = null;
        try {
            output = socket.getOutputStream();
            new ObjectOutputStream(output).writeObject(outObject);
            output.flush();
            Log.d(TAG, "[Network] Output complete : " + outObject.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receive() {
        try {
            Log.d(TAG, "receive: ");

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//             서버가 보내온 소켓의 바이트 스트림을 다시 char타입의 InputSTreamReader 그것을라인단위로 읽기 위해서
//             BufferedReader로 감싼다

            while (isRunning) {
                String str = reader.readLine();// str변수에 라인단위로 저장
                Log.d(TAG, "receive: " + str);

                if (str.equals("build complete")) {
                    handler.sendEmptyMessage(0);
//                    Toast.makeText(context, "완료", Toast.LENGTH_SHORT).show();
                    BuildGMMActivity.isBaseGmmSet = true;
                    continue;
                }
                if (str.equals("save")) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/TalkToMe/conversation.txt"));
                    str = reader.readLine();
                    while(!str.equals("finish")) {
                        writer.write(str);
                        str = reader.readLine();
                    }
                    writer.close();
                    continue;
                }
                if (isCancelled()) {
                    Log.d(TAG, "receive: Cancel");
                    break;
                }
                if (str.equals(speaker + "[out]")) {// null이면
                    Log.d(TAG, "receive: out");
                    break;// 종료
                }

                publishProgress(new MessageItem(str));
            }

            Log.d(TAG, "receive: finish");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "onCancelled: call cancelled");
        isRunning = false;
        super.onCancelled();
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(context, "Build GMM 완료", Toast.LENGTH_SHORT).show();
            BuildGMMActivity.setGMMState("Complete!");
            super.handleMessage(msg);
        }
    };
}
