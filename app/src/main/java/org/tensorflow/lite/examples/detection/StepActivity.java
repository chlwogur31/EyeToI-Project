package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.speech.tts.TextToSpeech.ERROR;

public class StepActivity extends AppCompatActivity implements SensorEventListener {

    // 센서 관련 객체
    SensorManager m_sensor_manager;    // 센서 매니저 객체
    Sensor m_accelerometer;            // 가속도 센서

    // 보폭 측정기
    StrideCalculator cal;

    // 실수의 출력 자리수를 지정하는 포맷 객체
    DecimalFormat m_format;

    static int count = 1;      // timer 시간

    private TextToSpeech tts; // TTS 변수 선언
    private TextView text1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step);

        // 초기화
        init();

        text1 = (TextView) findViewById(R.id.textView1);

        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);

                    tts.speak(text1.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

//        // 일정 시간 동안 걸은 후, 다음 페이지로 값을 넘겨줌
        int timeSet = 180;   // 지정시간
        Timer timer=new Timer();
        TimerTask task=new TimerTask(){
            @Override
            public void run() {
                //TODO Auto-generated method stub
                if(count < timeSet){ //count값이 5보다 작거나 같을때까지 수행
                    count++; //실행횟수 증가
                }
                else{
                    timer.cancel(); //타이머 종료
                    System.out.println("[카운트다운 : 종료]");

                    int stride  = (int)cal.getStride();
                    System.out.println(stride);
                    Intent intent = new Intent(getApplicationContext(), WalkingActivity.class);
                    intent.putExtra("stride", stride);
                    startActivity(intent);

                    finish();
                }
            }
        };
        timer.schedule(task, 1000, 1000); //실행 Task, 1초뒤 실행, 1초마다 반복

        if(m_accelerometer !=null){
            //센서의 속도 설정
            m_sensor_manager.registerListener(this, m_accelerometer,SensorManager.SENSOR_DELAY_GAME);
        }
    }
    public void onStop(){
        super.onStop();
        if(m_sensor_manager!=null){
            m_sensor_manager.unregisterListener(this);
        }
    }

    // 초기화 메소드
    public void init()
    {

        // 포맷 객체를 생성한다.
        m_format = new DecimalFormat();
        m_format.applyLocalizedPattern("0.##");    // 소수점 두자리까지 출력될 수 있는 형식을 지정한다.

        // 센서 객체 선언
        // 시스템서비스로부터 SensorManager 객체를 얻는다.
        // SensorManager 를 이용해서 가속도 센서 객체를 얻는다.
        m_sensor_manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        m_accelerometer = m_sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Stride Calculator 객체 초기화
        cal = new StrideCalculator();
        cal.init();

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

    static int cnt = 0;
    // 센서값이 변경될 때마다 메소드 실행
    @Override
    public void onSensorChanged(SensorEvent event) {

        // 센서들이 동시에 호출되므로 synchronized로 관리
        synchronized (this) {
            switch (event.sensor.getType()) {

                // 가속 센서가 전달한 데이터인 경우
                case Sensor.TYPE_ACCELEROMETER:
                    // 계산할 가속도 값을 넘기고 PDR법 계산 진행
                    cal.setAcc(event.values[0], event.values[1], event.values[2]);
                    cal.cal_PDR();
                    break;
            }
        }
    }

    // 정확도 변경시 호출되는 메소드. 잘 사용되지 않음
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}