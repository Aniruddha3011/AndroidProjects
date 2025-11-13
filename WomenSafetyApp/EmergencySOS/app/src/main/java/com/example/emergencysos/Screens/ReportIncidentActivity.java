package com.example.emergencysos.Screens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencysos.R;

public class ReportIncidentActivity extends AppCompatActivity {

    Button guidedButton,Aireporting;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_incident);

        guidedButton = findViewById(R.id.guidedButton);
        Aireporting = findViewById(R.id.AIreporting);

        guidedButton.setOnClickListener(v -> {
            Intent intent = new Intent(ReportIncidentActivity.this, GuidedReportingActivity.class);
            startActivity(intent);
        });
        Aireporting.setOnClickListener(v -> {
            Intent intent = new Intent(ReportIncidentActivity.this, ai_report.class);
            startActivity(intent);
        });





    }
}
