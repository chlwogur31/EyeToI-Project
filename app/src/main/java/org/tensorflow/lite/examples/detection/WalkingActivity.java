package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class WalkingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking);

        Button startBtn = findViewById(R.id.FinButton);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();

              //  String value = binding.editText.getText().toString();
                // Error HERE
                TextView strideView = findViewById(R.id.editTextTextPersonName);
                int measuredStride = Integer.parseInt(strideView.getText().toString());

                System.out.println(measuredStride);

                editor.putBoolean("oneStep_set", true);
                editor.putInt("step_length", measuredStride);
                editor.apply();

                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                startActivity(intent);

                finish();
            }
        });
    }
}