package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class StrideMainActivity extends AppCompatActivity {

    private TextToSpeech tts; // TTS 변수 선언

    private TextView text;
    private TextView text1;
    private TextView text2;
    private TextView text3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stride_main);

        text = (TextView) findViewById(R.id.textView);
        text1 = (TextView) findViewById(R.id.textView1);
        text2 = (TextView) findViewById(R.id.textView2);
        text3 = (TextView) findViewById(R.id.textView3);

        Button startBtn = findViewById(R.id.StartBtn);
        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(getApplicationContext(), StepActivity.class);
                startActivity(intent);

                finish();
            }
        });

        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);

                    tts.speak(text.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak(text1.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak(text2.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak(text3.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTS 객체가 남아있다면 실행을 중지하고 메모리에서 제거한다.
        if(tts != null){
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}