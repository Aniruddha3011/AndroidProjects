package com.example.emergencysos.Screens;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.R;

public class IncomingCallActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private static final int AUTO_END_DELAY = 30000; // 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        TextView callerNameText = findViewById(R.id.callerNameText);
        TextView callerNumberText = findViewById(R.id.callerNumberText);
        ImageView answerBtn = findViewById(R.id.answerBtn);
        ImageButton declineBtn = findViewById(R.id.declineBtn);

        String name = getIntent().getStringExtra("caller_name");
        String number = getIntent().getStringExtra("caller_number");

        if (name == null) name = "Mom";
        if (number == null) number = "+91 8208331164";

        callerNameText.setText(name);
        callerNumberText.setText(number);

        // Ringtone
        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // Answer
        String finalName = name;
        String finalNumber = number;
        answerBtn.setOnClickListener(v -> {
            stopAndReleaseMediaPlayer();
            Intent intent = new Intent(IncomingCallActivity.this, OngoingActivity.class);
            intent.putExtra("caller_name", finalName);
            intent.putExtra("caller_number", finalNumber);
            startActivity(intent);
            finish();
        });

        // Decline
        declineBtn.setOnClickListener(v -> {
            stopAndReleaseMediaPlayer();
            finish();
        });

        // Auto-decline
        new Handler().postDelayed(() -> {
            stopAndReleaseMediaPlayer();
            finish();
        }, AUTO_END_DELAY);
    }

    private void stopAndReleaseMediaPlayer() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        stopAndReleaseMediaPlayer();
        super.onDestroy();
    }
}
