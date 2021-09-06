package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.speech.tts.TextToSpeech.ERROR;

public class WalkingActivity extends AppCompatActivity {

    private TextToSpeech tts; // TTS 변수 선언

    private TextView text;
    private TextView text1;
    private TextView text2;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking);

        text1 = (TextView) findViewById(R.id.textView1);
        text2 = (TextView) findViewById(R.id.textView2);
        editText = (EditText) findViewById(R.id.editText);

        // Get intent data, update stride
        Intent intent = getIntent(); //이 액티비티를 부른 인텐트를 받는다.
        int stride = intent.getIntExtra("stride",0); //측정된 보폭 수 받아옴
        editText.setText(String.valueOf(stride));

        Button startBtn = findViewById(R.id.FinButton);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();

                TextView strideView = findViewById(R.id.editText);
                int measuredStride = Integer.parseInt(strideView.getText().toString());

//                System.out.println(measuredStride);

                editor.putBoolean("oneStep_set", true);
                editor.putInt("step_length", measuredStride);
                editor.apply();

                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                startActivity(intent);

                finish();
            }
        });

        alert();

        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);

                    tts.speak(text1.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak(editText.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                    tts.speak("센티미터", TextToSpeech.QUEUE_ADD, null, null);
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

// 알람 소리 파일 교체할 것
    public void alert(){
        MediaPlayer player;
        player = MediaPlayer.create(getApplicationContext(), R.raw.alarm1);
        player.start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        player.stop();
    }
}