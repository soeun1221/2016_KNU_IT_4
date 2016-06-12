package com.knu.android.talktome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.knu.android.talktome.utils.ExtAudioRecorder;

import java.io.DataOutputStream;
import java.io.IOException;

import fr.lium.spkDiarization.system.Diarization;


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
        String[] diarizationParams = {"--fInputMask=%s.wav", "--sOutputMask=%s.seg", "--doCEClustering", "base"};
        Diarization.main(diarizationParams);
        Toast.makeText(SpeakerActivity.this, "Finish", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        recorder.release();
        super.onDestroy();
    }

    public synchronized void execCommand(String command) {
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes(command + "\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
