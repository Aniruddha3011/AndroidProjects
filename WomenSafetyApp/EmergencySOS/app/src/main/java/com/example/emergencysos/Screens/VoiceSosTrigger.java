package com.example.emergencysos.Screens;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceSosTrigger extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String result : matches) {
                        if (result.equalsIgnoreCase("sathi emergency")) {
                            triggerSOSUI();
                            triggerActions();
                            return; // ✅ stop after trigger
                        }
                    }
                }
                // ❌ Removed restart listening here
            }

            @Override public void onError(int error) {
                // ❌ Removed restart listening here
            }

            // unused
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.startListening(speechIntent); // ✅ only once
    }

    private void triggerSOSUI() {
        Toast.makeText(this, "SOS Triggered by Voice!", Toast.LENGTH_SHORT).show();
    }

    private void triggerActions() {
        String[] emergencyContacts = {"+918208331164"};
        SosUtils.triggerSOS(getApplicationContext(), emergencyContacts);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
