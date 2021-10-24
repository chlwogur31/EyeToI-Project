package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {
    private static final String PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String PERMISSION_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSIONS_REQUEST = 1;
    private TextToSpeech textToSpeech;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_menu);
        AppGlobal.getConfig().setTts();
        textToSpeech = AppGlobal.getConfig().getTts();

        ImageButton detect_mode = (ImageButton) findViewById(R.id.detection);
        ImageButton poi_mode = (ImageButton) findViewById(R.id.poi_mode);
        detect_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, DetectorActivity.class);
                startActivity(intent);
            }
        });

        poi_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermission()) {
                    Intent intent2 = new Intent(MenuActivity.this, POIReadActivity.class);
                    startActivity(intent2);
                }
                else {
                    textToSpeech.speak(String.format("위치 권한이 허용되지 않았습니다. 위치 권한을 허용하고 다시 시도해주십시요"), TextToSpeech.QUEUE_ADD, null, null);
                }
            }
        });

        if (!hasPermission())
            requestPermission();
    }

    private boolean hasPermission() {
        return (checkSelfPermission(PERMISSION_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && checkSelfPermission(PERMISSION_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        requestPermissions(new String[]{PERMISSION_FINE_LOCATION, PERMISSION_COARSE_LOCATION}, PERMISSIONS_REQUEST);

    }
}