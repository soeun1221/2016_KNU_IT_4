package com.knu.android.talktome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.knu.android.talktome.instance.Constant;
import com.knu.android.talktome.utils.ExtAudioRecorder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import data.OBJECT;

public class BuildGMMActivity extends AppCompatActivity {

    private static final String TAG = "BuildGMMActivity";

    private ExtAudioRecorder recorder;

    private Button btnRecording;
    private TextView textViewRecordState;
    private static TextView textViewGmmState;
    private Client client;

    private SharedPreferences sharedPref;

    private long backPressedTime = 0;

    public static boolean isBaseGmmSet = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_gmm);

        sharedPref = getSharedPreferences("TalkToMe", Activity.MODE_PRIVATE);

        btnRecording = (Button) findViewById(R.id.btnRecording);
        textViewRecordState = (TextView) findViewById(R.id.textViewRecordState);
        textViewGmmState = (TextView) findViewById(R.id.textViewGmmState);

        recorder = ExtAudioRecorder.getInstanse(false);
        recorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/TalkToMe/base.wav");
        client = Client.getInstance(sharedPref.getString("speaker", ""));
        client.setContext(this);
        client.execute();
    }

    public void btnRecording(View v) {
        Log.d(TAG, "btnRecording: " + recorder.getState());
        if (recorder.getState() == ExtAudioRecorder.State.RECORDING) {
            recorder.stop();
            recorder.release();
            btnRecording.setText("Start");
            textViewRecordState.setText("Stopped");
        } else {
            recorder.prepare();
            recorder.start();
            btnRecording.setText("Stop");
            textViewRecordState.setText("Recording");
        }
    }

    @Override
    protected void onDestroy() {
        if (recorder != null)
            recorder.release();
        super.onDestroy();
    }

    public void btnBuildGmm(View v) {

        Toast.makeText(BuildGMMActivity.this, "GMM을 생성하는 중입니다 완료를 알릴때까지 기다려주세요", Toast.LENGTH_LONG).show();
        setGMMState("setting...");
        RandomAccessFile file = null;
        byte[] wavefile = null;
        try {
            file = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/TalkToMe/base.wav", "r");
            wavefile = new byte[(int) file.length()];
            file.read(wavefile);
            client.send(new OBJECT(Constant.SEND_BASE_WAVE, wavefile));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnStartListening(View v) {
        if (isBaseGmmSet) {
            Intent intent = new Intent(BuildGMMActivity.this, MainActivity.class);
            startActivity(intent);
            return;
        }
        Toast.makeText(BuildGMMActivity.this, "Base GMM이 아직 설정되지 않았습니다", Toast.LENGTH_SHORT).show();
    }

    public static void setGMMState(String state) {
        textViewGmmState.setText(state);
    }


    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;
        if (intervalTime >= 0 && intervalTime <= 2000) {
            super.onBackPressed();
        } else {
            backPressedTime = tempTime;
            Toast.makeText(this, "'뒤로'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
