package com.example.emergencysos.Screens;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.emergencysos.Models.RecordingModel;
import com.example.emergencysos.R;
import com.example.emergencysos.Services.ShakeService;  // <-- added import
import com.example.emergencysos.databinding.ActivityHomePageBinding;
import com.google.android.gms.location.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomePageActivity extends AppCompatActivity {

    ActivityHomePageBinding binding;
    FirebaseAuth auth;
    private static final int REQ_PERMS = 101;

    FusedLocationProviderClient fusedLocationProviderClient;
    final int REQUEST_CODE = 101;
    Handler locationHandler = new Handler();

    MediaRecorder recorder;
    String audioFilePath;
    private TextView sosText, sosMessage;
    private ImageView sosIcon;

    LocationCallback locationCallback;
    List<String> emergencyContactList = new ArrayList<>();

    private TelephonyManager telephonyManager;
    private CallStateListener callStateListener;
    private boolean callAnswered = false;
    private Handler callTimeoutHandler = new Handler();
    private Runnable callTimeoutRunnable;
    private static final int CALL_ANSWER_TIMEOUT_MS = 30000; // 30 seconds timeout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        binding = ActivityHomePageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        loadEmergencyContact();

        // Request all needed permissions once
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String firstName = prefs.getString("firstName", "");
        String lastName = prefs.getString("lastName", "");
        String fullName = firstName + " " + lastName + "!";
        String imageUrl = prefs.getString("profileImageUrl", null);

        TextView userNameTextView = findViewById(R.id.user_name);
        userNameTextView.setText(fullName);

        CircleImageView profileImageView = findViewById(R.id.profileimage);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.user)
                    .into(profileImageView);
        }

        binding.safetycheckin.setOnClickListener(v -> {
            Intent intent = new Intent(this, Safety_check_in.class);
            startActivity(intent);
        });

        binding.Fakecall.setOnClickListener(v -> {
            Intent intent = new Intent(this, FakeCallActivity.class);
            startActivity(intent);
        });

        binding.AddEmergencyNo.setOnClickListener(v -> {
            Intent intent = new Intent(this, add_emergency_contact.class);
            startActivity(intent);
        });

        binding.aiTipButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatBotActivity.class);
            startActivity(intent);
        });

        binding.sosbutton.setOnClickListener(view -> {
            if (emergencyContactList.isEmpty()) {
                Toast.makeText(HomePageActivity.this, "No emergency contact number found", Toast.LENGTH_SHORT).show();
                return;
            }

            callAnswered = false;
            String callNumberToDial = emergencyContactList.get(0); // First emergency contact number

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + callNumberToDial));

            if (ActivityCompat.checkSelfPermission(HomePageActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(HomePageActivity.this, "Call permission not granted", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(HomePageActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                return;
            }
            startActivity(callIntent);

            // Setup fallback: if call not answered in 30 seconds, send SMS + start recording
            callTimeoutRunnable = () -> {
                if (!callAnswered) {
                    Toast.makeText(HomePageActivity.this, "Call not answered in time, sending SOS SMS", Toast.LENGTH_SHORT).show();
                    startLiveLocationUpdates();
                    startAudioRecording();
                }
            };
            callTimeoutHandler.postDelayed(callTimeoutRunnable, CALL_ANSWER_TIMEOUT_MS);
        });

        binding.report.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportIncidentActivity.class);
            startActivity(intent);
        });

        binding.shortcut.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShortcutActivity.class);
            startActivity(intent);
        });

        binding.profileimage.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserInfoActivity.class);
            startActivity(intent);
        });

        sosText = findViewById(R.id.sosText);
        sosMessage = findViewById(R.id.sosMessage);
        sosIcon = findViewById(R.id.sosIcon);

        // Start the Volume Button Service for hardware SOS triggers
        Intent volumeServiceIntent = new Intent(this, VolumeButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(volumeServiceIntent);
        } else {
            startService(volumeServiceIntent);
        }

//        // Start the Voice Command Service properly with correct context
//        Intent voiceServiceIntent = new Intent(this, VoiceCommandService.class);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(voiceServiceIntent);
//        } else {
//            startService(voiceServiceIntent);
//        }

        // --- **START SHAKE SERVICE HERE** ---
        Intent shakeIntent = new Intent(this, ShakeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(shakeIntent);
        } else {
            startService(shakeIntent);
        }
        // --- **END SHAKE SERVICE START** ---

        // Initialize TelephonyManager and listen to call states for Android < Q only
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        callStateListener = new CallStateListener();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        // --- ADD THIS LINE to check if activity started via shake trigger ---
        checkShakeTrigger(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkShakeTrigger(intent);
    }

    // NEW method for shake-trigger detection
    private void checkShakeTrigger(Intent intent) {
        if (intent != null && intent.getBooleanExtra("shake_triggered", false)) {
            Toast.makeText(this, "SOS triggered by shake!", Toast.LENGTH_SHORT).show();
            startLiveLocationUpdates();
            startAudioRecording();
        }
    }

    // Handle new intent when activity is already running (singleTop launch mode)


    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    callAnswered = true;
                    callTimeoutHandler.removeCallbacks(callTimeoutRunnable);
                    Toast.makeText(HomePageActivity.this, "Emergency call answered", Toast.LENGTH_SHORT).show();
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (!callAnswered) {
                        Toast.makeText(HomePageActivity.this, "Call not answered, sending SOS SMS", Toast.LENGTH_SHORT).show();
                        startLiveLocationUpdates();
                        startAudioRecording();
                    }
                    callAnswered = false;
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (telephonyManager != null && callStateListener != null) {
            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    private void loadEmergencyContact() {
        SharedPreferences prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);
        emergencyContactList.clear();

        String savedPhone = prefs.getString("phone", null);
        if (savedPhone != null && !savedPhone.isEmpty()) {
            emergencyContactList.add(savedPhone);
        }

        Set<String> savedSet = prefs.getStringSet("contacts", new HashSet<>());
        for (String entry : savedSet) {
            String[] parts = entry.split(",");
            if (parts.length == 3) {
                String phone = parts[1].trim();
                if (!emergencyContactList.contains(phone)) {
                    emergencyContactList.add(phone);
                }
            }
        }

        Log.d("SOS", "Emergency contact numbers loaded: " + emergencyContactList.size());
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
            int locationUpdateCount = 0;
            final int MAX_UPDATES = 2;

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
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
                    Toast.makeText(HomePageActivity.this, "Live location SMS sent", Toast.LENGTH_SHORT).show();
                    locationUpdateCount++;
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, locationHandler.getLooper());
    }

    private void stopLiveLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Toast.makeText(this, "Stopped live location updates", Toast.LENGTH_SHORT).show();
        }
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

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("recordings");
        String key = dbRef.push().getKey();

        RecordingModel recording = new RecordingModel(
                key,
                fileUri.getLastPathSegment(),
                fileUri.toString(),
                System.currentTimeMillis(),
                "10 sec"
        );

        dbRef.child(auth.getUid()).child(key).setValue(recording);


        audioRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> audioRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> sendAudioLinkSMS(uri.toString()))
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendAudioLinkSMS(String downloadUrl) {
        String message = "Emergency Audio Recording: " + downloadUrl;
        SmsManager smsManager = SmsManager.getDefault();
        for (String number : emergencyContactList) {
            smsManager.sendTextMessage(number, null, message, null, null);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Toast.makeText(this, "SOS triggered by volume button", Toast.LENGTH_SHORT).show();
            startLiveLocationUpdates();
            startAudioRecording();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
