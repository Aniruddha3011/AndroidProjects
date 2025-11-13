package com.example.emergencysos.Screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.R;

public class FakeCallActivity extends AppCompatActivity {

    EditText editTextCallerName, editTextDelay;
    Spinner spinnerRingtone, spinnerFakeVoice;
    Button buttonInitiateFakeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        editTextCallerName = findViewById(R.id.editTextCallerName);
        editTextDelay = findViewById(R.id.editTextDelay);
        spinnerRingtone = findViewById(R.id.spinnerRingtone);
        spinnerFakeVoice = findViewById(R.id.spinnerFakeVoice);
        buttonInitiateFakeCall = findViewById(R.id.buttonInitiateFakeCall);


        String[] ringtoneOptions = {"Default Ringtone", "Ringtone 1", "Ringtone 2"};
        String[] fakeVoiceOptions = {"No Voice", "Voice 1", "Voice 2"};

        spinnerRingtone.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ringtoneOptions));
        spinnerFakeVoice.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fakeVoiceOptions));

        buttonInitiateFakeCall.setOnClickListener(v -> {
            String callerName = editTextCallerName.getText().toString().trim();
            String delayStr = editTextDelay.getText().toString().trim();
            String selectedRingtone = spinnerRingtone.getSelectedItem().toString();
            String selectedVoice = spinnerFakeVoice.getSelectedItem().toString();

            if (callerName.isEmpty() || delayStr.isEmpty()) {
                Toast.makeText(this, "Please enter caller name and delay.", Toast.LENGTH_SHORT).show();
                return;
            }

            int delayMillis = Integer.parseInt(delayStr) * 1000;

            new Handler().postDelayed(() -> {
                Intent intent = new Intent(FakeCallActivity.this, IncomingCallActivity.class);
                intent.putExtra("caller_name", callerName);
                intent.putExtra("ringtone", selectedRingtone);
                intent.putExtra("voice", selectedVoice);
                startActivity(intent);
            }, delayMillis);
        });
    }
}
