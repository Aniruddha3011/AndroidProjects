package com.example.emergencysos.Screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.emergencysos.R;
import com.example.emergencysos.databinding.ActivityMainBinding;
import com.google.android.gms.location.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    Button sosButton;
    TextView location, phone, keypad;
    FusedLocationProviderClient fusedLocationProviderClient;
    final int REQUEST_CODE = 101;

    MediaRecorder recorder;
    String audioFilePath;
    Handler locationHandler = new Handler();
    int locationUpdateCount = 0;
    final int MAX_UPDATES = 10;

    LocationCallback locationCallback;
    List<String> emergencyContactList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        sosButton = findViewById(R.id.sosButton);
        location = findViewById(R.id.location);
        phone = findViewById(R.id.phone);
        keypad = findViewById(R.id.keypad);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        loadEmergencyContact();

        sosButton.setOnClickListener(v -> {
            startLiveLocationUpdates();
            startAudioRecording();
        });

        location.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, GoogleMapActivity.class);
            startActivity(intent);
        });

        phone.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, FakeCallActivity.class);
            startActivity(intent);
        });

        keypad.setOnClickListener(view -> {
            String number = emergencyContactList.isEmpty() ? "+911234567890" : emergencyContactList.get(0);
            EmergencyKeypadDialog.show(MainActivity.this, number);
        });

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CALL_PHONE
        }, REQUEST_CODE);
    }

    private void loadEmergencyContact() {
        SharedPreferences prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);
        String savedPhone = prefs.getString("phone", null);
        if (savedPhone != null && !savedPhone.isEmpty()) {
            emergencyContactList.clear(); // clear old
            emergencyContactList.add(savedPhone);
        } else {
            // fallback numbers
            emergencyContactList.add("+918208331164");
            emergencyContactList.add("+917248934207");
        }
    }

    private void startLiveLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(30000);
        locationRequest.setFastestInterval(15000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationUpdateCount >= MAX_UPDATES) {
                    stopLiveLocationUpdates();
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    String message = "Live Location: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                    SmsManager smsManager = SmsManager.getDefault();
                    for (String number : emergencyContactList) {
                        smsManager.sendTextMessage(number, null, message, null, null);
                    }
                    Toast.makeText(MainActivity.this, "Live location SMS sent", Toast.LENGTH_SHORT).show();
                    locationUpdateCount++;
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, locationHandler.getLooper());
    }

    private void stopLiveLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Toast.makeText(this, "Stopped live location updates", Toast.LENGTH_SHORT).show();
    }

    private void startAudioRecording() {
        try {
            audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath()
                    + "/sos_audio_" + System.currentTimeMillis() + ".m4a";

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(audioFilePath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.prepare();
            recorder.start();

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(this::stopAudioRecording, 10000);

        } catch (IOException e) {
            Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudioRecording() {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;

                Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show();
                uploadAudioToFirebase();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAudioToFirebase() {
        File audioFile = new File(audioFilePath);
        if (!audioFile.exists() || audioFile.length() < 1024) {
            Toast.makeText(this, "Audio file missing or invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = Uri.fromFile(audioFile);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference audioRef = storageRef.child("sos_recordings/" + fileUri.getLastPathSegment());

        audioRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> audioRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            sendAudioLinkSMS(uri.toString());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendAudioLinkSMS(String downloadUrl) {
        String message = "Emergency Audio Recording: " + downloadUrl;
        SmsManager smsManager = SmsManager.getDefault();
        for (String number : emergencyContactList) {
            smsManager.sendTextMessage(number, null, message, null, null);
        }
        Toast.makeText(this, "Audio URL sent via SMS", Toast.LENGTH_SHORT).show();
    }
}
