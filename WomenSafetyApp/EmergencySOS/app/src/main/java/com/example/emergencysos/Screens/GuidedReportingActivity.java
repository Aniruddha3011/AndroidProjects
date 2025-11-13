package com.example.emergencysos.Screens;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.emergencysos.Models.ReportModel;
import com.example.emergencysos.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;

public class GuidedReportingActivity extends AppCompatActivity {

    EditText dateTextView, timeTextView, locationEditText, peopleInvolvedEditText;
    Spinner incidentTypeSpinner;
    Button btnSelectEvidence, btnSaveDraft;
    TextView fileNameTextView;
    Uri selectedEvidenceUri;

    private static final int REQUEST_CODE_FILE_PICKER = 101;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_reporting);

        // Initialize views
        dateTextView = findViewById(R.id.et_date);
        timeTextView = findViewById(R.id.et_time);
        locationEditText = findViewById(R.id.et_location);
        peopleInvolvedEditText = findViewById(R.id.et_people_involved);
        incidentTypeSpinner = findViewById(R.id.spinner_incident_type);
        btnSelectEvidence = findViewById(R.id.btn_select_evidence);
        fileNameTextView = findViewById(R.id.tv_selected_file);
        btnSaveDraft = findViewById(R.id.savedraft); // new button

        // Set up spinner
        String[] incidentTypes = new String[]{
                "Harassment", "Theft", "Vandalism", "Assault", "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, incidentTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        incidentTypeSpinner.setAdapter(adapter);

        // Date and time pickers
        dateTextView.setOnClickListener(v -> showDatePicker());
        timeTextView.setOnClickListener(v -> showTimePicker());

        // Evidence file picker
        btnSelectEvidence.setOnClickListener(v -> openFilePicker());

        // Save Draft action
        btnSaveDraft.setOnClickListener(v -> saveReportAsPDF());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            dateTextView.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String selectedTime = String.format("%02d:%02d", hourOfDay, minute);
            timeTextView.setText(selectedTime);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "video/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Evidence File"), REQUEST_CODE_FILE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FILE_PICKER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedEvidenceUri = data.getData();
            String fileName = getFileNameFromUri(selectedEvidenceUri);
            fileNameTextView.setText(fileName != null ? fileName : "File selected");
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null && uri != null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private void saveReportAsPDF() {
        String date = dateTextView.getText().toString();
        String time = timeTextView.getText().toString();
        String location = locationEditText.getText().toString();
        String people = peopleInvolvedEditText.getText().toString();
        String type = incidentTypeSpinner.getSelectedItem().toString();
        String evidence = fileNameTextView.getText().toString();


        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600, 800, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(14);
        int y = 40;

        canvas.drawText("Guided Incident Report (Draft)", 200, y, paint);
        y += 40;
        canvas.drawText("Date: " + date, 20, y, paint);
        y += 25;
        canvas.drawText("Time: " + time, 20, y, paint);
        y += 25;
        canvas.drawText("Location: " + location, 20, y, paint);
        y += 25;
        canvas.drawText("People Involved: " + people, 20, y, paint);
        y += 25;
        canvas.drawText("Incident Type: " + type, 20, y, paint);
        y += 25;
        canvas.drawText("Evidence File: " + evidence, 20, y, paint);
        y += 25;

        document.finishPage(page);

        // Save file
        String fileName = "Guided_Report_Draft_" + System.currentTimeMillis() + ".pdf";
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
        if (!directory.exists()) directory.mkdirs();

        File filePath = new File(directory, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            document.writeTo(fos);

            Toast.makeText(this, "Draft saved to: " + filePath.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Save to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("REPORTS", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            Gson gson = new Gson();
            String json = prefs.getString("report_list", "[]");
            Type listType = new TypeToken<ArrayList<ReportModel>>() {}.getType();
            ArrayList<ReportModel> reports = gson.fromJson(json, listType);

            if (reports == null) reports = new ArrayList<>();

            // ✅ Use correct constructor with (location, datetime, type, filePath)
            String datetime = date + " " + time;
            ReportModel report = new ReportModel(location, datetime, type, filePath.getAbsolutePath());

            reports.add(report);

            String updatedJson = gson.toJson(reports);
            editor.putString("report_list", updatedJson);
            editor.apply();

            // Optionally open the saved PDF
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    filePath
            );

            Intent openPDF = new Intent(Intent.ACTION_VIEW);
            openPDF.setDataAndType(pdfUri, "application/pdf");
            openPDF.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(Intent.createChooser(openPDF, "Open PDF with"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save draft", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }
}
