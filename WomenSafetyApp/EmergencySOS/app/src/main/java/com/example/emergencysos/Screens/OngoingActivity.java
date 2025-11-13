package com.example.emergencysos.Screens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.R;
import com.example.emergencysos.databinding.ActivityOngoingBinding;

public class OngoingActivity extends AppCompatActivity {

    ActivityOngoingBinding binding;

    TextView callerNameText, callTimerText;
    MediaPlayer mediaPlayer;
    String voiceLang;

    private Handler timerHandler = new Handler();
    private long startTime = 0L;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityOngoingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        callerNameText = findViewById(R.id.callerNameText);
        callTimerText = findViewById(R.id.callTimer);



        String callerName = getIntent().getStringExtra("caller_name");
        voiceLang = getIntent().getStringExtra("voice");


        callerNameText.setText(callerName);
        binding.endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(OngoingActivity.this, HomePageActivity.class);
                startActivity(intent);
            }
        });



        playVoiceAfterPickup();
        startTimer();
    }

    private void playVoiceAfterPickup() {
        int resId = 0;

        if (voiceLang != null) {
            switch (voiceLang.toLowerCase()) {
                case "english":
                    resId = R.raw.english;
                    break;
                case "hindi":
//                    resId = R.raw.hindi;
                    break;
                case "marathi":
//                    resId = R.raw.marathi;
                    break;
                default:
                    resId = R.raw.english;
                    break;
            }
        } else {
            resId = R.raw.english;
        }

        mediaPlayer = MediaPlayer.create(this, resId);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.stop();
                mp.release();
            });
            mediaPlayer.start();
        }
    }

    private void startTimer() {
        startTime = SystemClock.uptimeMillis();
        timerHandler.postDelayed(updateTimerThread, 0);
    }

    private final Runnable updateTimerThread = new Runnable() {
        public void run() {
            long timeInMillis = SystemClock.uptimeMillis() - startTime;

            int secs = (int) (timeInMillis / 1000);
            int mins = secs / 60;
            secs = secs % 60;

            callTimerText.setText(String.format("%02d:%02d", mins, secs));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        timerHandler.removeCallbacks(updateTimerThread);
    }
}
