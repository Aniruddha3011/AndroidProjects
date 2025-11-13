package com.example.emergencysos.Screens;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

public class EmergencyKeypadDialog {

    public static void show(Context context, String emergencyNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter Emergency Code");

        final EditText input = new EditText(context);
        input.setHint("Type 01, 02...");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String code = input.getText().toString().trim();
            SmsManager smsManager = SmsManager.getDefault();

            switch (code) {
                case "01":

                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE)
                            == PackageManager.PERMISSION_GRANTED) {
                        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + emergencyNumber));
                        context.startActivity(callIntent);
                    } else {
                        Toast.makeText(context, "CALL_PHONE permission not granted", Toast.LENGTH_LONG).show();
                    }
                    break;

                case "02":
                    String[] messages = {
                            "I need help immediately!",
                            "Emergency! Please call me.",
                            "Danger! Contact me ASAP."
                    };
                    smsManager.sendTextMessage(emergencyNumber, null, messages[0], null, null);
                    Toast.makeText(context, "Emergency SMS sent", Toast.LENGTH_SHORT).show();
                    break;

                case "03":
                    smsManager.sendTextMessage(emergencyNumber, null, "I am being followed!", null, null);
                    Toast.makeText(context, "03: Message sent", Toast.LENGTH_SHORT).show();
                    break;

                case "04":
                    smsManager.sendTextMessage(emergencyNumber, null, "I am in danger! Track me!", null, null);
                    Toast.makeText(context, "04: Message sent", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Toast.makeText(context, "Invalid code. Try 01-04.", Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
