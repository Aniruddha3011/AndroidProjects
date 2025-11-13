package com.example.emergencysos.Screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.emergencysos.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class Safety_check_in extends AppCompatActivity {

    private static final String PREFS = "safety_prefs";
    private static final int REQ_SMS = 101;
    private static final int REQ_LOCATION = 102;
    private static final int REQ_CALL_PHONE = 300;
    private static final int REQ_READ_PHONE_STATE = 400; // NEW constant

    TextInputEditText etInterval, etWarning, etContacts, etMessage;
    TextView tvCountdown, tvLog, tvLastLocation, tvSavedContacts;
    Button btnStart, btnSOS, btnEnableNotif, btnAllowLocation, btnTestAlert;
    Button btnQuickStart, btnQuickStop, btnQuickCheckIn, btnQuickSOS;

    SharedPreferences prefs;
    FusedLocationProviderClient fusedLocationClient;
    Location lastLocation;

    ActivityResultLauncher<String[]> permLauncher;

    private CountDownTimer checkInTimer;

    // Call-related
    private TelephonyManager telephonyManager;
    private CallStateListener callStateListener;
    private boolean callAnswered = false;
    private String currentCallNumber = null;
    private Handler callTimeoutHandler = new Handler();
    private static final int CALL_TIMEOUT_MS = 30000; // 30 seconds timeout for call answer

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_check_in);

        etInterval = findViewById(R.id.etInterval);
        etWarning = findViewById(R.id.etWarning);
        etContacts = findViewById(R.id.etContacts);
        etMessage = findViewById(R.id.etMessage);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvLog = findViewById(R.id.tvLog);
        tvLastLocation = findViewById(R.id.tvLastLocation);
        tvSavedContacts = findViewById(R.id.tvSavedContacts);

        btnStart = findViewById(R.id.btnStart);
        btnSOS = findViewById(R.id.btnSOS);
        btnEnableNotif = findViewById(R.id.btnEnableNotif1);
        btnAllowLocation = findViewById(R.id.btnAllowLocation);
        btnTestAlert = findViewById(R.id.btnTestAlert);

        btnQuickStart = findViewById(R.id.btnQuickStart);
        btnQuickStop = findViewById(R.id.btnQuickStop);
        btnQuickCheckIn = findViewById(R.id.btnQuickCheckIn);
        btnQuickSOS = findViewById(R.id.btnQuickSOS);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Init TelephonyManager and listener
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        callStateListener = new CallStateListener();

        loadSaved();

        permLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean loc = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean sms = result.getOrDefault(Manifest.permission.SEND_SMS, false);
                    Boolean call = result.getOrDefault(Manifest.permission.CALL_PHONE, false);
                    Boolean readPhone = result.getOrDefault(Manifest.permission.READ_PHONE_STATE, false);

                    if (loc) fetchLocation();
                    if (sms) Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                    if (call) Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show();
                    if (readPhone) {
                        if (telephonyManager != null && callStateListener != null) {
                            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                        }
                        Toast.makeText(this, "Read phone state permission granted", Toast.LENGTH_SHORT).show();
                    }
                });

        btnStart.setOnClickListener(v -> onStartCheckIn(false));
        btnSOS.setOnClickListener(v -> triggerSOS());

        btnEnableNotif.setOnClickListener(v -> requestNotifPermission());
        btnAllowLocation.setOnClickListener(v -> requestLocationPermission());
        btnTestAlert.setOnClickListener(v -> showTestAlert());

        btnQuickStart.setOnClickListener(v -> onStartCheckIn(true));
        btnQuickStop.setOnClickListener(v -> stopCheckInService());
        btnQuickCheckIn.setOnClickListener(v -> manualCheckIn());
        btnQuickSOS.setOnClickListener(v -> triggerSOS());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (telephonyManager != null && callStateListener != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE}, REQ_READ_PHONE_STATE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (telephonyManager != null && callStateListener != null) {
            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // Incoming call, ignore
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // Call answered
                    callAnswered = true;
                    callTimeoutHandler.removeCallbacksAndMessages(null);
                    appendLog("Call answered by user.");
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // Call ended
                    if (!callAnswered && currentCallNumber != null) {
                        appendLog("Call not answered, sending SMS...");
                        sendSOSsms(currentCallNumber);
                    }
                    currentCallNumber = null;
                    callAnswered = false;
                    callTimeoutHandler.removeCallbacksAndMessages(null);
                    break;
            }
        }
    }

    private void loadSaved() {
        etInterval.setText(String.valueOf(prefs.getInt("interval", 5)));
        etWarning.setText(String.valueOf(prefs.getInt("warning", 10)));
        etContacts.setText(prefs.getString("contacts", ""));
        etMessage.setText(prefs.getString("message", "I missed my safety check-in. Please help. My last known location: {LOCATION}"));
        String contacts = prefs.getString("contacts", "None");
        tvSavedContacts.setText("Saved Contacts: " + (TextUtils.isEmpty(contacts) ? "None" : contacts));
    }

    private void saveSettings() {
        int interval = parseIntOr(etInterval.getText().toString(), 5);
        int warning = parseIntOr(etWarning.getText().toString(), 10);
        String contacts = etContacts.getText().toString();
        String message = etMessage.getText().toString();

        prefs.edit()
                .putInt("interval", interval)
                .putInt("warning", warning)
                .putString("contacts", contacts)
                .putString("message", message)
                .apply();

        tvSavedContacts.setText("Saved Contacts: " + (TextUtils.isEmpty(contacts) ? "None" : contacts));
        appendLog(String.format(Locale.getDefault(), "Settings saved (interval=%d min, warning=%d s)", interval, warning));
    }

    private int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onStartCheckIn(boolean quick) {
        saveSettings();
        int interval = prefs.getInt("interval", 5);
        int warning = prefs.getInt("warning", 10);
        String contacts = prefs.getString("contacts", "");
        String message = prefs.getString("message", "");

        if (checkInTimer != null) {
            checkInTimer.cancel();
        }

        long intervalMillis = interval * 60 * 1000L;

        checkInTimer = new CountDownTimer(intervalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                long minutesPart = secondsLeft / 60;
                long secondsPart = secondsLeft % 60;
                tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d remaining", minutesPart, secondsPart));

                if (secondsLeft == warning) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(Safety_check_in.this, R.raw.beep);
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> mp.release());

                    triggerSOS();
                    appendLog("Warning time reached: SOS alert sent automatically.");
                }
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("Check-in interval elapsed!");
                appendLog("Check-in timer finished.");

                String contactsCsv = prefs.getString("contacts", "");
                if (!TextUtils.isEmpty(contactsCsv)) {
                    String[] parts = contactsCsv.split(",");
                    for (String raw : parts) {
                        raw = raw.trim();
                        if (raw.matches("^\\+?[0-9]{6,}$")) {
                            makeCall(raw);
                            return; // call first valid number only
                        }
                    }
                }

                appendLog("No valid emergency contact number to call.");
            }
        }.start();

        Intent svc = new Intent(this, CheckInService.class);
        svc.putExtra("interval", interval);
        svc.putExtra("warning", warning);
        svc.putExtra("contacts", contacts);
        svc.putExtra("message", message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        }

        appendLog("Check-in started");
    }

    private void makeCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQ_CALL_PHONE);
            return;
        }
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);

        currentCallNumber = phoneNumber;
        callAnswered = false;

        callTimeoutHandler.postDelayed(() -> {
            if (!callAnswered && currentCallNumber != null) {
                appendLog("Call timeout expired, sending SMS...");
                sendSOSsms(currentCallNumber);
            }
        }, CALL_TIMEOUT_MS);
    }

    private void sendSOSsms(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            String locationText = (lastLocation != null)
                    ? String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", lastLocation.getLatitude(), lastLocation.getLongitude())
                    : "Location not available";

            String messageTemplate = prefs.getString("message", "I missed my safety check-in. Please help. My last known location: {LOCATION}");
            String finalMessage = messageTemplate.replace("{LOCATION}", locationText);

            try {
                SmsManager.getDefault().sendTextMessage(phoneNumber, null, finalMessage, null, null);
                appendLog("SMS sent to " + phoneNumber);
            } catch (Exception e) {
                e.printStackTrace();
                appendLog("Failed to send SMS to " + phoneNumber);
            }
        } else {
            appendLog("No SMS permission to send message.");
            permLauncher.launch(new String[]{Manifest.permission.SEND_SMS});
        }
    }

    private void stopCheckInService() {
        Intent svc = new Intent(this, CheckInService.class);
        stopService(svc);
        tvCountdown.setText("No check-in started yet.");
        appendLog("Check-in stopped");

        if (checkInTimer != null) {
            checkInTimer.cancel();
            checkInTimer = null;
        }
    }

    private void manualCheckIn() {
        Intent i = new Intent("com.example.safetyapp.ACTION_CHECKIN");
        sendBroadcast(i);
        appendLog("Manual Check-In triggered");
        Toast.makeText(this, "Check-in recorded", Toast.LENGTH_SHORT).show();
    }

    private void triggerSOS() {
        saveSettings();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndSendSOS();
        } else {
            permLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS});
        }
    }

    private void fetchLocationAndSendSOS() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        lastLocation = location;
                        String locationText = location != null ?
                                String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", location.getLatitude(), location.getLongitude())
                                : "Location not available";

                        String contactsCsv = prefs.getString("contacts", "");
                        String messageTemplate = prefs.getString("message", "I missed my safety check-in. Please help. My last known location: {LOCATION}");
                        String finalMessage = messageTemplate.replace("{LOCATION}", locationText);

                        boolean sent = false;
                        if (!TextUtils.isEmpty(contactsCsv)) {
                            String[] parts = contactsCsv.split(",");
                            for (String raw : parts) {
                                raw = raw.trim();
                                if (raw.matches("^\\+?[0-9]{6,}$")) {
                                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                        try {
                                            SmsManager.getDefault().sendTextMessage(raw, null, finalMessage, null, null);
                                            sent = true;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                                        sendIntent.setData(Uri.parse("sms:" + raw));
                                        sendIntent.putExtra("sms_body", finalMessage);
                                        startActivity(sendIntent);
                                        sent = true;
                                    }
                                } else if (raw.contains("@")) {
                                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + raw));
                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Emergency Alert");
                                    emailIntent.putExtra(Intent.EXTRA_TEXT, finalMessage);
                                    startActivity(emailIntent);
                                    sent = true;
                                }
                            }
                        }
                        if (!sent) {
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setType("text/plain");
                            share.putExtra(Intent.EXTRA_TEXT, finalMessage);
                            startActivity(Intent.createChooser(share, "Send alert via"));
                        }
                        appendLog("SOS triggered. Location: " + locationText);
                        Toast.makeText(this, "SOS sent (or composer opened)", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        appendLog("Failed to get location for SOS");
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            lastLocation = location;
            if (location != null) {
                tvLastLocation.setText(String.format(Locale.getDefault(), "Last known location: %.5f, %.5f",
                        location.getLatitude(), location.getLongitude()));
                appendLog("Location updated");
            } else {
                tvLastLocation.setText("Last known location: Not available");
            }
        });
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
        } else {
            fetchLocation();
        }
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            } else {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Notifications handled by system (no runtime permission needed)", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTestAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Test Alert")
                .setMessage("This simulates a missed check-in alert. It will NOT send real messages.")
                .setPositiveButton("OK", (dialog, which) -> appendLog("Test alert executed"))
                .show();
    }

    private void appendLog(String text) {
        String prev = tvLog.getText().toString();
        String updated = prev.equals("No entries yet.") ? text : (prev + "\n" + text);
        tvLog.setText(updated);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_READ_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (telephonyManager != null && callStateListener != null) {
                    telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                }
                Toast.makeText(this, "Read phone state permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Read phone state permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 200) {
            Toast.makeText(this, "Notification permission result handled", Toast.LENGTH_SHORT).show();
        }
    }
}

