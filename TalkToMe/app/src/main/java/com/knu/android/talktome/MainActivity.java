package com.knu.android.talktome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.knu.android.talktome.utils.AudioWriterPCM;
import com.naver.speech.clientapi.SpeechConfig;

import java.io.IOException;
import java.lang.ref.WeakReference;


public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    private static final String CLIENT_ID = "YCbcJhHY4rl1k4y1_AS5"; // "내 애플리케이션"에서 Client ID를 확인해서 이곳에 적어주세요.
    private static final SpeechConfig SPEECH_CONFIG = SpeechConfig.OPENAPI_KR; // or SpeechConfig.OPENAPI_EN

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;


    private Button btnStart;
    private TextView txtResult;
    private String mResult;

    private AudioWriterPCM writer;

    private boolean isRunning;
    private boolean listening;

    private SharedPreferences sharedPref;

    private ListView listView;
    public static MessageAdapter messageAdapter;

    private Client client;

    private long backPressedTime = 0;

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
                if (listening) {
                    click();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getSharedPreferences("TalkToMe", Activity.MODE_PRIVATE);

        txtResult = (TextView) findViewById(R.id.txt_result);
        btnStart = (Button) findViewById(R.id.btn_start);
        listView = (ListView) findViewById(R.id.messageListView);

        messageAdapter = new MessageAdapter(this, R.layout.message_item);
        listView.setAdapter(messageAdapter);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        // When message is added, it makes listview to scroll last message
        messageAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                Log.d(TAG, "onChanged: " + (messageAdapter.getCount() - 1));
                listView.setSelection(messageAdapter.getCount() - 1);
            }
        });

        listening = false;

        handler = new RecognitionHandler(this);

        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID, SPEECH_CONFIG);

        client = new Client(sharedPref.getString("speaker", ""), messageAdapter);
        client.execute();
    }

    public void btnStart(View v) {
        listening = !listening;
        click();
    }

    public void click() {
        Log.d(TAG, "click: " + isRunning);
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
        if (message.isEmpty()) return;
        client.send(message);
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
//        naverRecognizer.getSpeechRecognizer().stopImmediately();
//        naverRecognizer.getSpeechRecognizer().release();
//
//        isRunning = false;
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

    public MainActivity() {
        super();
    }

    @Override
    protected void onDestroy() {

        client.cancel(true);
        Log.d(TAG, "onDestroy :" + client.getStatus());

        naverRecognizer.getSpeechRecognizer().stopImmediately();
        naverRecognizer.getSpeechRecognizer().release();
        isRunning = false;

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;
        if (intervalTime >= 0 && intervalTime <= 2000) {
            try {
                sendTalk("bye");
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.onBackPressed();
        } else {
            backPressedTime = tempTime;
            Toast.makeText(this, "'뒤로'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }
}