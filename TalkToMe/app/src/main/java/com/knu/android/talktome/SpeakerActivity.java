package com.knu.android.talktome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.knu.android.talktome.instance.Constant;
import com.knu.android.talktome.network.Communication;
import com.knu.android.talktome.network.CommunicationManager;
import com.knu.android.talktome.utils.ExtAudioRecorder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import data.OBJECT;


public class SpeakerActivity extends AppCompatActivity {

    private static final String TAG = "SpeakerActivity";

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedEditor;

    private EditText editSpeaker;

    private ExtAudioRecorder recorder;

    private Button btnRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker);

        // TalkToMe란 이름으로 shared preference 생성
        sharedPref = getSharedPreferences("TalkToMe", Activity.MODE_PRIVATE);
        // 에디터객체
        sharedEditor = sharedPref.edit();

        editSpeaker = (EditText) findViewById(R.id.speakerEditText);
        editSpeaker.setText(sharedPref.getString("speaker", ""));

        btnRecording = (Button) findViewById(R.id.buttonRecording);

        recorder = ExtAudioRecorder.getInstanse(false);
        recorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest/base.wav");
    }

    public void btnSpeakerSetting(View v) {
        String speaker = editSpeaker.getText().toString();
        if (speaker.isEmpty()) {
            Toast.makeText(SpeakerActivity.this, "화자를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!sharedPref.getString("speaker", "").equals(speaker)) {
            //SharedPrefernce에 새로 저장. speaker 키로 값 저장, 나중에 같은 키로 불러올 수 있다
            sharedEditor.putString("speaker", speaker);
            sharedEditor.commit();
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void btnRecording(View v) {
        Log.d(TAG, "btnRecording: " + recorder.getState());
        if (recorder.getState() == ExtAudioRecorder.State.RECORDING) {
            recorder.stop();
            recorder.release();
            btnRecording.setText("Start");
        } else {
            recorder.prepare();
            recorder.start();
            btnRecording.setText("Stop");
        }
    }

    public void btnBuildGmm(View v) {
        RandomAccessFile file = null;
        byte[] wavefile = null;
        try {
            file = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest/base.wav", "r");
            wavefile = new byte[(int)file.length()];
            file.read(wavefile);

            new Communication(new OBJECT(Constant.SEND_BASE_WAVE, wavefile), new CommunicationManager() {
                @Override
                public void AfterCommunication(OBJECT getInObject) {
                    try {
                        handler.sendMessage(Message.obtain(handler, getInObject.getMessage(),
                                getInObject));
                    } catch (NullPointerException e) {
                        handler.sendEmptyMessage(-1);
                    }
                }
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        recorder.release();
        super.onDestroy();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.SEND_BASE_WAVE:
                    Toast.makeText(SpeakerActivity.this, "gmm 을 생성하였습니다.", Toast.LENGTH_SHORT).show();
                    break;
                case -1:
                    Toast.makeText(SpeakerActivity.this, "서버통신에 실패하였습니다", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
