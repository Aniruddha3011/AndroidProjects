package com.example.emergencysos.Screens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class SosBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.emergencysos.ACTION_TRIGGER_SOS".equals(intent.getAction())) {
            Toast.makeText(context, "SOS Broadcast Received", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(context, SosService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
