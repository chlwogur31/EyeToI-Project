package org.tensorflow.lite.examples.detection;

import android.app.Application;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class AppGlobal extends Application {
    private double longitude;
    private double latitude;
    private int angle;
    private TextToSpeech tts;

    private static AppGlobal instance;

    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static AppGlobal getConfig() {
        return instance;
    }


    public void setLongitude(double longitude){
        this.longitude = longitude;
    }
    public void setLatitude(double latitude){
        this.latitude = latitude;
    }
    public void setAngle(int angle){
        this.angle = angle;
    }
    public void setTts() {
        this.tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR)
                    tts.setLanguage(Locale.KOREAN);
            }
        });
    }

    public double getLongitude(){
        return longitude;
    }
    public double getLatitude(){
        return latitude;
    }
    public int getAngle(){
        return angle;
    }
    public TextToSpeech getTts() {return tts;}

}
