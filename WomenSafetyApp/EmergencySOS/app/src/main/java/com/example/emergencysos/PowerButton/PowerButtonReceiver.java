package com.example.emergencysos.PowerButton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class PowerButtonReceiver extends BroadcastReceiver {

    private static int powerPressCount = 0;
    private static long lastPressTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastPressTime < 4000) {
                powerPressCount++;
            } else {
                powerPressCount = 0;
            }

            lastPressTime = currentTime;

            if (powerPressCount == 4) {
                powerPressCount = 0;

                Toast.makeText(context, "Triple Power Button Detected!", Toast.LENGTH_SHORT).show();

                // Start SOS Service
                Intent serviceIntent = new Intent(context, SosService.class);
                context.startService(serviceIntent);
            }
        }
    }
}

