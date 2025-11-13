package com.example.emergencysos.Screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.emergencysos.Models.EmergencyContact;
import com.example.emergencysos.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShortcutActivity extends AppCompatActivity {

    private static final int REQUEST_CALL_PERMISSION = 100;
    private static final int REQUEST_SMS_PERMISSION = 101;
    private static final int REQUEST_WOMAN_SAFETY_CALL_PERMISSION = 102;
    private static final int REQUEST_AMBULANCE_CALL_PERMISSION = 103; // 🚑

    private List<EmergencyContact> contactList;
    private SharedPreferences prefs;

    private MediaPlayer mediaPlayer;
    private boolean isSirenPlaying = false;

    private final String WOMAN_SAFETY_NUMBER = "7276143214";
    private final String AMBULANCE_NUMBER = "7666468380";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shortcut);

        Button callBtn = findViewById(R.id.btn_call);
        Button smsBtn = findViewById(R.id.btn_msg);
        Button sirenBtn = findViewById(R.id.btn_siren);


        // Loading contacts
        prefs = getSharedPreferences("emergency_contacts", MODE_PRIVATE);
        loadContactListFromPrefsSet();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String firstName = prefs.getString("firstName", "");
        String lastName = prefs.getString("lastName", "");
        String fullName = firstName + " " + lastName + "!";
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        TextView textView = findViewById(R.id.user_name);
        textView.setText(fullName);

        TextView home= findViewById(R.id.home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ShortcutActivity.this, HomePageActivity.class);
                startActivity(intent);
            }
        });

        //Call Emergency Contact
        callBtn.setOnClickListener(v -> {
            if (contactList.isEmpty()) {
                Toast.makeText(this, "No emergency contacts saved", Toast.LENGTH_SHORT).show();
                return;
            }
            String number = contactList.get(0).getPhone();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
            } else {
                callPhoneNumber(number);
            }
        });

        // Send Emergency SMS
        smsBtn.setOnClickListener(v -> {
            if (contactList.size() < 2) {
                Toast.makeText(this, "Less than 2 emergency contacts saved", Toast.LENGTH_SHORT).show();
                return;
            }
            String number = contactList.get(1).getPhone();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
            } else {
                sendEmergencySms(number);
            }
        });

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        TextView textView1=findViewById(R.id.national_number);
        textView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ShortcutActivity.this, NationalNumber.class);
                startActivity(intent);
            }
        });


        //Play and Stop Siren
        sirenBtn.setOnClickListener(v -> {
            if (isSirenPlaying) {
                stopSiren();
                sirenBtn.setText("🚨 Play Siren");
            } else {
                startSiren();
                sirenBtn.setText("⏹️ Stop Siren");
            }
        });


    }

    // Load contacts using the same method as add_emergency_contact activity
    private void loadContactListFromPrefsSet() {
        contactList = new ArrayList<>();
        Set<String> savedSet = prefs.getStringSet("contacts", new HashSet<>());
        for (String entry : savedSet) {
            String[] parts = entry.split(",");
            if (parts.length == 3) {
                contactList.add(new EmergencyContact(parts[0], parts[1], parts[2]));
            }
        }
    }

    private void callPhoneNumber(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    private void sendEmergencySms(String phoneNumber) {
        try {
            String message = "🚨 This is an emergency! Please contact me immediately.";
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Emergency SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startSiren() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.siren);
            mediaPlayer.setLooping(true);
        }
        mediaPlayer.start();
        isSirenPlaying = true;
    }

    private void stopSiren() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isSirenPlaying = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSiren();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!contactList.isEmpty()) {
                    callPhoneNumber(contactList.get(0).getPhone());
                }
            } else {
                Toast.makeText(this, "CALL_PHONE permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (contactList.size() >= 2) {
                    sendEmergencySms(contactList.get(1).getPhone());
                }
            } else {
                Toast.makeText(this, "SEND_SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_WOMAN_SAFETY_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callPhoneNumber(WOMAN_SAFETY_NUMBER);
            } else {
                Toast.makeText(this, "CALL_PHONE permission denied for woman safety", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_AMBULANCE_CALL_PERMISSION) { // 🚑
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callPhoneNumber(AMBULANCE_NUMBER);
            } else {
                Toast.makeText(this, "CALL_PHONE permission denied for ambulance", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
