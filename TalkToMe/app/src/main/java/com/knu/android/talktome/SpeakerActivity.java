package com.knu.android.talktome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SpeakerActivity extends AppCompatActivity {

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedEditor;

    private EditText editSpeaker;

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
    }

    public void btnSpeakerSetting(View v){
        String speaker = editSpeaker.getText().toString();
        if(speaker.isEmpty()) {
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
}
