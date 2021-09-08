/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import static android.speech.tts.TextToSpeech.ERROR;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends TextToSpeechActivity implements OnImageAvailableListener, SensorEventListener {
        private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = false;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Detector detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

// new
  private double oldSqrSize = 0;
  private double ratio = 0;
  private double tempRatio = 0;
  private String oldTitle;

  //현재 걸음 수
  private int mSteps = 0;
  //리스너가 등록되고 난 후의 step count
  private int mCounterSteps = 0;

  private TextToSpeech textToSpeech;

  //센서 연결을 위한 변수
  private SensorManager sensorManager;
  private Sensor stepCountSensor;
  Context context;


  private int oldStep = 0;
  private int curStep = 0;
  private int oneStep = 60; // 보폭
  private int movedStep = 0;

  private int distanceFromBollard = 0;

  private PoI poi;


  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(null);

    context = this;

    // check onestep valid
    setOneStep();

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

    //센서 연결[걸음수 센서를 이용한 흔듬 감지]
    sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

    if(stepCountSensor == null){
      Toast.makeText(this,"No Step Detect Sensor",Toast.LENGTH_SHORT).show();
    }

    poi = new PoI(getApplicationContext());
    poi.init();

  }
  public void onStart() {
    super.onStart();
    if(stepCountSensor !=null){
      //센서의 속도 설정
      sensorManager.registerListener(this, stepCountSensor,SensorManager.SENSOR_DELAY_GAME);
    }

    poi.readLocation();

  }
  public void onStop(){
    super.onStop();
    if(sensorManager!=null){
      sensorManager.unregisterListener(this);
    }

    poi.startCalc();  // 80 전방위 범위 계산 및 3미터 삼각 계산

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
  // end new

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              this,
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing Detector!");
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            LOGGER.i("Running detection on image " + currTimestamp);

            final long startTime = SystemClock.uptimeMillis();
            final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);


            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            }

            final List<Detector.Recognition> mappedRecognitions =
                new ArrayList<Detector.Recognition>();

            for (final Detector.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
              }
            }

            // from here, start new code

            if (oldStep == 0) {
              oldStep = mSteps;
              curStep = mSteps;
            }

            // 이전/현재 비율 구하기
            // oldType 없을 시 제일 큰 물체 oldType 으로 지정
            double ratio = sizeCompare(mappedRecognitions);

            Toast myToast6 = Toast.makeText(getApplicationContext(),
                            String.format("RATIO: %f", ratio), Toast.LENGTH_SHORT);
                    myToast6.show();
            if(ratio != 0){
              // update curStep
              curStep = mSteps;

              // 현재 보수 받아오기
//              if (oldStep != curStep)
//                movedStep = curStep - oldStep;
              movedStep = 2;
              double movedDistance = 0;
              movedDistance = oneStep * movedStep * 0.01;

              // update oldStep
              oldStep = curStep;

//                if (movedDistance != 0) {
//                    Toast myToast = Toast.makeText(this.getApplicationContext(),
//                            String.format("Step: %d  moved : %f", movedStep, movedDistance), Toast.LENGTH_SHORT);
//                    myToast.show();
//                }

              // 물체로부터의 거리 계산
              double distanceFrom = 0;
              // 멀어질 경우 : 3/5 비교 후, => 보폭 * 5/2 => 보폭 * pow((1 - 3/5), -1)    (넘겨줄 때 음수 처리)
              // 가까워질 경우 : 3/5 비교 후, => 보폭 * 3/2 => 보폭 * 3/5 * pow((1 - 3/5), -1)
              // 가까워질 때
              if(ratio > 0)
                distanceFrom = movedDistance * ratio * Math.pow((1 - ratio), -1);
                // 멀어질 때
              else if (ratio < 0){
                ratio = -1 * ratio;
                distanceFrom = movedDistance * Math.pow((1 - ratio), -1);
              }

              if(movedStep != 0) {
                Toast myToast3 = Toast.makeText(getApplicationContext(),
                        String.format("moved : %f, ratio : %f, distanceFrom : %f", movedDistance, ratio, distanceFrom), Toast.LENGTH_SHORT);
                myToast3.show();

                distanceFromBollard = (int)distanceFrom + 1;    // 올림

              }
//                else{
//                    Toast myToast4 = Toast.makeText(this.getApplicationContext(),"NO", Toast.LENGTH_SHORT);
//                    myToast4.show();
//                }
              movedStep = 0;
            }

            if(distanceFromBollard != 0 && distanceFromBollard < 7) {
              textToSpeech.speak(String.format("볼라드 %d미터", distanceFromBollard), TextToSpeech.QUEUE_ADD, null, null);
//            textToSpeech.speak(String.format("볼라드 %d미터", 3), TextToSpeech.QUEUE_ADD, null, null);

              if(distanceFromBollard>=0){
                try {
                  System.out.println("Enter Insert");
                  poi.insert();
                  System.out.println("End Insert");
                }
                catch (Exception e){}
              }
            distanceFromBollard = 0;
            }

            // new code end

            // Request Render
            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

          }
        });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(
        () -> {
          try {
            detector.setUseNNAPI(isChecked);
          } catch (UnsupportedOperationException e) {
            LOGGER.e(e, "Failed to set \"Use NNAPI\".");
            runOnUiThread(
                () -> {
                  Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
          }
        });
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){

      //stepcountsenersor는 앱이 꺼지더라도 초기화 되지않는다. 그러므로 우리는 초기값을 가지고 있어야한다.
      if (mCounterSteps < 1) {
        // initial value
        mCounterSteps = (int) event.values[0];
      }
      //리셋 안된 값 + 현재값 - 리셋 안된 값
      mSteps = (int) event.values[0] - mCounterSteps;
//            mwalknum.setText(Integer.toString(mSteps));
      Log.i("log: ", "New step detected by STEP_COUNTER sensor. Total step count: " + mSteps );
    }

  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

  public void setOneStep(){
    //보폭 크기 설정
  //  File file = new File(getExternalFilesDir(null), "step_length.txt");
    SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
    SharedPreferences.Editor editor = pref.edit();


//    // TestCode to make stride false
//    editor.putBoolean("oneStep_set", false);
//    editor.apply();
//    // End TestCode


    // if not set
    if( !pref.getBoolean("oneStep_set", false)) {
      // After Context Switching => 보폭 설정 화면으로 이동
      // Move to Stride Main
      Intent intent = new Intent(getApplicationContext(), StrideMainActivity.class);
      startActivity(intent);

      finish();
//      //Test Code
//      editor.putBoolean("oneStep_set", true);
//      editor.putInt("step_length", 60);
//      editor.apply();
    }

    oneStep = pref.getInt("step_length", 100);
    System.out.println("oneStep = " + oneStep);

  }
  public double sizeCompare(List<Detector.Recognition> recognitions) {

    Vector v = new Vector();

    if (recognitions != null) {
      // 사이즈 모두 계산, sorting 까지
      for (Detector.Recognition r : recognitions) {
        SizeNType temp = new SizeNType();
        temp.size = Math.sqrt(r.getLocation().width() * r.getLocation().height()); // 상크기 루트 씌운거
        temp.type = r.getTitle();
        // 같은 class 일 경우만 선정
        if (oldTitle != null) {
          if (temp.type.equals(oldTitle))
            v.add(temp);
        } else{ // 첫 시행이라 oldTitle이 null 일 때
          oldTitle = temp.type;
        }
      }
//            Collections.sort(v, new Comparator() {
//                @Override
//                public int compare(Object arg0, Object arg1) {
//                    return ((SizeNType) arg0).size > ((SizeNType) arg1).size ? 0 : 1;
//                }
//            });
    }
    // 임시로 저장할 curr_squareSize, oldSquareSize에 저장할 목적
    double temp_squareSize = 0;
//        Log.i(LOGGING_TAG, String.format("Old : %f", oldSqrSize));
    for (int i = 0; i < v.size(); i++) {
      SizeNType curr = (SizeNType) v.get(i);
      double curr_squareSize = curr.size;

      // 아직 한번도 갱신이 안되었을 경우
      if (oldSqrSize == 0) {
        oldSqrSize = curr_squareSize;
        oldTitle = curr.type;
        ratio = 0;
        Log.i("TEST", "NO...");
        return ratio;
      }

      Log.i("TEST", "YES!");

      // ratio가 1에 제일 가까운(큰) 이유는 비율 차이가 가장 안나는 것을  선정
      // 멀어질 경우 : 3/5 비교 후, => 보폭 * 5/2 => 보폭 * pow((1 - 3/5), -1)    (넘겨줄 때 음수 처리)
      // 가까워질 경우 : 3/5 비교 후, => 보폭 * 3/2 => 보폭 * 3/5 * pow((1 - 3/5), -1)
      // 비율
      if(oldSqrSize < curr_squareSize)
        tempRatio = oldSqrSize * Math.pow(curr_squareSize, -1);
      else if(oldSqrSize > curr_squareSize)
        tempRatio = -1 * curr_squareSize * Math.pow(oldSqrSize, -1);

      Log.i("TEST", String.format("tempRatio : %f ratio : %f", tempRatio, ratio));
      // 비교
      // tempRatio가 더 큰 경우 => 갱신
      if(i == 0 || Math.abs(ratio) < Math.abs(tempRatio)) {
        Log.i("TEST", String.format("Yes"));
        ratio = tempRatio;
        temp_squareSize = curr_squareSize;
      }
    } // end for
    oldSqrSize = temp_squareSize;
    return ratio;
  }
}
