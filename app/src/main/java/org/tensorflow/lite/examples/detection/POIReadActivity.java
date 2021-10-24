package org.tensorflow.lite.examples.detection;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class POIReadActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private PoI poi;
//    private static final String PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
//    private static final String PERMISSION_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
//    private static final int PERMISSIONS_REQUEST = 1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_poiread);

        // TTS settings
        AppGlobal.getConfig().setTts();
        textToSpeech = AppGlobal.getConfig().getTts();


        // TTS settings
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status != ERROR)
                    textToSpeech.setLanguage(Locale.KOREAN);
            }
        });

//        if (!hasPermission())
//            requestPermission();

    }
    public void onStart() {
        super.onStart();
    }

    public void onResume() {
        super.onResume();
//    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);    //리스너 object 등록
        poi = new PoI(getApplicationContext());
        poi.init();

        poi.readLocation();
        poi.startCalc(); // 80 전방위 범위 계산 및 3미터 삼각 계산
    }

    public void onStop(){
        super.onStop();
    }

    public void onPause(){
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        // TTS 객체가 남아있다면 실행을 중지하고 메모리에서 제거한다.
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
//    private boolean hasPermission() {
//        return (checkSelfPermission(PERMISSION_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//                && checkSelfPermission(PERMISSION_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private void requestPermission() {
//
//        requestPermissions(new String[] {PERMISSION_FINE_LOCATION,PERMISSION_COARSE_LOCATION}, PERMISSIONS_REQUEST);
//
//    }

}
