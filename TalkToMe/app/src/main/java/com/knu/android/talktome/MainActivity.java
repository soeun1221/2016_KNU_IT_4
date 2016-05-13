package com.knu.android.talktome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.knu.android.talktome.instance.Constant;
import com.knu.android.talktome.utils.AudioWriterPCM;
import com.naver.speech.clientapi.SpeechConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;


public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    private static final String CLIENT_ID = "YCbcJhHY4rl1k4y1_AS5"; // "내 애플리케이션"에서 Client ID를 확인해서 이곳에 적어주세요.
    private static final SpeechConfig SPEECH_CONFIG = SpeechConfig.OPENAPI_KR; // or SpeechConfig.OPENAPI_EN

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    private TextView txtResult;
    private Button btnStart;
    private String mResult;

    private AudioWriterPCM writer;

    private boolean isRunning;
    private boolean listening;

    private SharedPreferences sharedPref;

    private Socket socket;

    // Handle speech recognition Messages.
    private void handleMessage(Message msg) throws IOException {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                txtResult.setText("Connected");
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest", this);
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                Log.i(TAG, "handleMessage: audioRecording");

                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                txtResult.setText(mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                String[] results = (String[]) msg.obj;
                mResult = results[0];
                txtResult.setText(mResult);
                sendTalk(mResult);
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                txtResult.setText(mResult);
                btnStart.setText(R.string.str_start);
                isRunning = false;
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }
                Log.d(TAG, "handleMessage: clientInactive");
                btnStart.setText(R.string.str_start);
                isRunning = false;
                if(listening)
                    click();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResult = (TextView) findViewById(R.id.txt_result);
        btnStart = (Button) findViewById(R.id.btn_start);
        sharedPref = getSharedPreferences("TalkToMe", Activity.MODE_PRIVATE);

        listening = false;

        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID, SPEECH_CONFIG);

        try {
            socket = new Socket(Constant.IP, Constant.PORT);// 소켓생성하고 초기값으로
            // IP주소와 포트번호
            Thread thread1 = new SenderThread(socket, sharedPref.getString("speaker", ""));// 센드 스레드 생성하고 초기값으로
            // 공유데이터인 소켓과 사용자
            // 이름을 줌
            Thread thread2 = new ReceiverThread(socket);// 리시브 스레드 생성하고 초기값으로 공유
            // 데이터인 소켓을 줌
            thread1.start();// 스레드 시작
            thread2.start();// 스레드 시작
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void btnStart(View v) {
        listening = !listening;
        click();
    }

    public void click() {
        Log.d(TAG, "click: "+isRunning);
        if (!isRunning) {
            // Start button is pushed when SpeechRecognizer's state is inactive.
            // Run SpeechRecongizer by calling recognize().
            mResult = "";
            txtResult.setText("Connecting...");
            btnStart.setText(R.string.str_stop);
            isRunning = true;

            naverRecognizer.recognize();
        } else {
            // This flow is occurred by pushing start button again
            // when SpeechRecognizer is running.
            // Because it means that a user wants to cancel speech
            // recognition commonly, so call stop().

            naverRecognizer.getSpeechRecognizer().stop();
            btnStart.setText(R.string.str_start);
        }
    }

    public void sendTalk(String message) throws IOException {
        if(message.isEmpty()) return;

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(message);
        writer.flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // initialize() must be called on resume time.
        naverRecognizer.getSpeechRecognizer().initialize();

        mResult = "";
        txtResult.setText("");
        btnStart.setText(R.string.str_start);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // release() must be called on pause time.
        naverRecognizer.getSpeechRecognizer().stopImmediately();
        naverRecognizer.getSpeechRecognizer().release();

        isRunning = false;
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        RecognitionHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                try {
                    activity.handleMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}