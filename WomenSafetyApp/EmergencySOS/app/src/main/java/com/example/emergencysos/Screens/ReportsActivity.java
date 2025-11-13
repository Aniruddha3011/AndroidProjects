package com.example.emergencysos.Screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.emergencysos.Models.ReportModel;
import com.example.emergencysos.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class ReportsActivity extends AppCompatActivity {

    LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_reports);

        container = findViewById(R.id.report_container);

        loadReports();
        loadAIReportCard(); // Added: Show AI report
    }

    private void loadReports() {
        SharedPreferences prefs = getSharedPreferences("REPORTS", MODE_PRIVATE);
        String json = prefs.getString("report_list", "[]");

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<ReportModel>>() {}.getType();
        ArrayList<ReportModel> reports = gson.fromJson(json, type);

        if (reports == null) {
            reports = new ArrayList<>();
        }

        for (ReportModel report : reports) {
            if (report.getFilePath() != null && !report.getFilePath().isEmpty()) {
                addReportCard(report);
            }
        }
    }

    private void addReportCard(ReportModel report) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_report_card, container, false);

        TextView tvLocation = cardView.findViewById(R.id.tv_location);
        TextView tvDatetime = cardView.findViewById(R.id.tv_datetime);
        TextView tvType = cardView.findViewById(R.id.tv_type);
        Button btnView = cardView.findViewById(R.id.btn_view);
        ImageView btnDelete = cardView.findViewById(R.id.btn_delete);

        tvLocation.setText(report.getLocation() != null ? report.getLocation() : "Unknown Location");
        tvDatetime.setText(report.getDatetime() != null ? report.getDatetime() : "Unknown Time");
        tvType.setText(report.getType() != null ? report.getType() : "Unknown Type");

        btnView.setOnClickListener(v -> {
            String path = report.getFilePath();

            if (path == null || path.isEmpty()) {
                Toast.makeText(this, "File path is missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(path);

            if (file.exists()) {
                Uri fileUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                );

                Intent openPDF = new Intent(Intent.ACTION_VIEW);
                openPDF.setDataAndType(fileUri, "application/pdf");
                openPDF.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(openPDF, "Open with"));
            } else {
                Toast.makeText(this, "PDF file not found!", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> {
            container.removeView(cardView);
            deleteReport(report);
        });

        container.addView(cardView);
    }

    private void deleteReport(ReportModel target) {
        SharedPreferences prefs = getSharedPreferences("REPORTS", MODE_PRIVATE);
        String json = prefs.getString("report_list", "[]");

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<ReportModel>>() {}.getType();
        ArrayList<ReportModel> reports = gson.fromJson(json, type);

        if (reports != null) {
            String targetPath = target.getFilePath();

            reports.removeIf(report ->
                    report.getFilePath() != null &&
                            targetPath != null &&
                            report.getFilePath().equals(targetPath)
            );

            prefs.edit().putString("report_list", gson.toJson(reports)).apply();
        }
    }

    //AI Report Card
    private void loadAIReportCard() {
        File aiReportFile = new File(getExternalFilesDir(null), "ai_report.pdf");

        if (!aiReportFile.exists()) {
            return;
        }

        View cardView = LayoutInflater.from(this).inflate(R.layout.item_report_card, container, false);

        TextView tvLocation = cardView.findViewById(R.id.tv_location);
        TextView tvDatetime = cardView.findViewById(R.id.tv_datetime);
        TextView tvType = cardView.findViewById(R.id.tv_type);
        Button btnView = cardView.findViewById(R.id.btn_view);
        ImageView btnDelete = cardView.findViewById(R.id.btn_delete);

        tvLocation.setText("Generated by AI");
        tvDatetime.setText("Latest Summary");
        tvType.setText("AI Report");


        btnView.setOnClickListener(v -> {
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    aiReportFile
            );

            Intent openPDF = new Intent(Intent.ACTION_VIEW);
            openPDF.setDataAndType(fileUri, "application/pdf");
            openPDF.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(openPDF, "Open AI Report"));
        });

        btnDelete.setOnClickListener(v -> {
            if (aiReportFile.delete()) {
                container.removeView(cardView);
                Toast.makeText(this, "AI Report deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete AI Report", Toast.LENGTH_SHORT).show();
            }
        });

        container.addView(cardView, 0);
    }
}
