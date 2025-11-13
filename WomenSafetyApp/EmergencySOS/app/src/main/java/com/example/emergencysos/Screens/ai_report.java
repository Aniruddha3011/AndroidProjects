package com.example.emergencysos.Screens;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emergencysos.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ai_report extends AppCompatActivity {

    // Declare the UI Elements from the XML layout
    private TextInputEditText editTextDescription;
    private MaterialButton buttonGenerateReport;

    // Report Page 1
    private MaterialCardView cardViewReport;
    private TextView textViewReportOutput;

    // Report Page 2
    private MaterialCardView cardViewReportPage2;
    private TextView textViewReportPage2Output;

    // Containers and Navigation
    private FrameLayout reportContainer;
    private LinearLayout navigationLayout;
    private MaterialButton buttonNextPage;
    private MaterialButton buttonPrevPage;
    private MaterialButton buttonSavePdf; // Button for saving PDF

    // Permission constant
    private static final int STORAGE_PERMISSION_CODE = 101;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line links the Java file to the XML layout file
        setContentView(R.layout.activity_ai_report);


        editTextDescription = findViewById(R.id.editTextDescription);
        buttonGenerateReport = findViewById(R.id.buttonGenerateReport);
        cardViewReport = findViewById(R.id.cardViewReport);
        textViewReportOutput = findViewById(R.id.textViewReportOutput);
        cardViewReportPage2 = findViewById(R.id.cardViewReportPage2);
        textViewReportPage2Output = findViewById(R.id.textViewReportPage2Output);
        reportContainer = findViewById(R.id.reportContainer);
        navigationLayout = findViewById(R.id.navigationLayout);
        buttonNextPage = findViewById(R.id.buttonNextPage);
        buttonPrevPage = findViewById(R.id.buttonPrevPage);
        buttonSavePdf = findViewById(R.id.buttonSavePdf);


        // Set a click listener on the main generate button
        buttonGenerateReport.setOnClickListener(v -> {
            String description = editTextDescription.getText().toString();
            if (description.trim().isEmpty()) {
                Toast.makeText(ai_report.this, "Please describe the incident.", Toast.LENGTH_SHORT).show();
                return;
            }
            generateAndDisplayReport(description);
        });

        // Set click listener for the "Next Page" button
        buttonNextPage.setOnClickListener(v -> {
            cardViewReport.setVisibility(View.GONE);
            cardViewReportPage2.setVisibility(View.VISIBLE);
            buttonNextPage.setVisibility(View.GONE);
            buttonPrevPage.setVisibility(View.VISIBLE);
        });

        // Set click listener for the "Previous Page" button
        buttonPrevPage.setOnClickListener(v -> {
            cardViewReport.setVisibility(View.VISIBLE);
            cardViewReportPage2.setVisibility(View.GONE);
            buttonNextPage.setVisibility(View.VISIBLE);
            buttonPrevPage.setVisibility(View.GONE);
        });

        // Set click listener for the "Save PDF" button
        buttonSavePdf.setOnClickListener(v -> {
            checkPermissionAndSavePdf();
        });
    }

    /**
     * Generates the report content and splits it across two pages.
     * @param description The raw incident description provided by the user.
     */
    private void generateAndDisplayReport(String description) {
        String incidentTime = extractTime(description);
        String incidentLocation = extractLocation(description);
        String incidentType = classifyIncident(description);
        String reportDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // --- Build Page 1 Content ---
        StringBuilder page1Builder = new StringBuilder();
        page1Builder.append("<b><u>CONFIDENTIAL - PRELIMINARY REPORT (1/2)</u></b><br>");
        page1Builder.append("<b>PUNE CITY POLICE DEPARTMENT</b><br>");
        page1Builder.append("========================================================<br><br>");
        page1Builder.append("<b>CASE DETAILS:</b><br>");
        page1Builder.append("<b>Case ID: </b>").append(System.currentTimeMillis()).append("<br>");
        page1Builder.append("<b>Date & Time Filed: </b>").append(reportDate).append("<br>");
        page1Builder.append("<b>Current Status: </b> <font color='#C2185B'>PRELIMINARY</font><br><br>");
        page1Builder.append("<b>INCIDENT SUMMARY:</b><br>");
        page1Builder.append("<b>- Incident Type (AI Classified): </b>").append(incidentType).append("<br>");
        page1Builder.append("<b>- Approximate Time Mentioned: </b>").append(incidentTime).append("<br>");
        page1Builder.append("<b>- Location Mentioned: </b>").append(incidentLocation).append("<br><br>");
        page1Builder.append("<b>DETAILED DESCRIPTION (as provided by complainant):</b><br>");
        page1Builder.append("<i>\"").append(description).append("\"</i><br>");

        // --- Build Page 2 Content ---
        StringBuilder page2Builder = new StringBuilder();
        page2Builder.append("<b><u>CONFIDENTIAL - PRELIMINARY REPORT (2/2)</u></b><br>");
        page2Builder.append("========================================================<br><br>");
        page2Builder.append("<b>INITIAL AI ANALYSIS & RECOMMENDED ACTIONS:</b><br>");
        page2Builder.append("1. <b>Classification Confidence:</b> Based on keywords, the incident is classified as '").append(incidentType).append("'. This requires human verification.<br>");
        page2Builder.append("2. <b>Severity Assessment (Provisional):</b> The system flags this report for immediate review by a duty officer.<br>");
        page2Builder.append("3. <b>Next Steps for Complainant:</b> Preserve any evidence (e.g., screenshots, photos, videos). Please visit the nearest police station with the Case ID to file a formal First Information Report (FIR).<br>");
        page2Builder.append("4. <b>Internal Action:</b> The report has been logged in the system. The jurisdiction will be determined based on the specified location for further action.<br><br>");
        page2Builder.append("<b>*** END OF SYSTEM-GENERATED REPORT ***</b><br><br>");
        page2Builder.append("<i><b>Disclaimer:</b> This is a system-generated document and is not legally binding until officially signed and stamped by a designated police officer. Its purpose is for initial information logging only.</i>");


        // Set the text for both pages using Html.fromHtml to render bold tags
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textViewReportOutput.setText(Html.fromHtml(page1Builder.toString(), Html.FROM_HTML_MODE_COMPACT));
            textViewReportPage2Output.setText(Html.fromHtml(page2Builder.toString(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            textViewReportOutput.setText(Html.fromHtml(page1Builder.toString()));
            textViewReportPage2Output.setText(Html.fromHtml(page2Builder.toString()));
        }

        // --- Set Initial Visibility ---
        reportContainer.setVisibility(View.VISIBLE);
        navigationLayout.setVisibility(View.VISIBLE);
        cardViewReport.setVisibility(View.VISIBLE); // Show page 1
        cardViewReportPage2.setVisibility(View.GONE); // Hide page 2
        buttonNextPage.setVisibility(View.VISIBLE); // Show next button
        buttonPrevPage.setVisibility(View.GONE); // Hide previous button
    }


    private void checkPermissionAndSavePdf() {
        // Check if we have permission to write to external storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // If permission is already granted, create and save the PDF
            createAndSavePdf();
        } else {
            // If not, request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If permission is granted by the user, create and save the PDF
                createAndSavePdf();
            } else {
                // If permission is denied, show a message
                Toast.makeText(this, "Storage Permission Denied. Cannot save PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void createAndSavePdf() {
        // Combine the text from both pages for the PDF
        String fullReportText = textViewReportOutput.getText().toString() + "\n\n" + textViewReportPage2Output.getText().toString();

        // Create a new PDF document
        PdfDocument pdfDocument = new PdfDocument();

        // Define page attributes
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // Get the canvas to draw on
        Canvas canvas = page.getCanvas();
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(ContextCompat.getColor(this, android.R.color.black));
        textPaint.setTextSize(10f); // Set text size for PDF

        // Use StaticLayout to handle text wrapping
        StaticLayout staticLayout = new StaticLayout(
                fullReportText,
                textPaint,
                canvas.getWidth() - 40, // width of the text area (canvas width - margins)
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false);

        // Draw the text on the canvas
        canvas.save();
        canvas.translate(20, 20); // Set margins
        staticLayout.draw(canvas);
        canvas.restore();

        // Finish the page
        pdfDocument.finishPage(page);

        // Define the file path and name
        String fileName = "IncidentReport_" + System.currentTimeMillis() + ".pdf";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, fileName);

        try {
            // Write the document content to the file
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved to Downloads folder: " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }


        pdfDocument.close();
    }


    private String extractTime(String text) {
        Pattern pattern = Pattern.compile("(\\d{1,2}\\s?(AM|PM|am|pm)|yesterday|today|evening|morning|afternoon|night)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(0) : "Not specified";
    }

    private String extractLocation(String text) {
        Pattern pattern = Pattern.compile("(near|at|on|in)\\s+([A-Z][a-zA-Z\\s.'-]{2,}(Road|Street|Nagar|Colony|Mall|Station|Pune|Mumbai))", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(2).trim() : "Not specified";
    }

    private String classifyIncident(String text) {
        String lowerCaseText = text.toLowerCase();
        if (lowerCaseText.contains("snatched") || lowerCaseText.contains("stole") || lowerCaseText.contains("theft")) {
            return "Theft / Snatching";
        }
        if (lowerCaseText.contains("harassed") || lowerCaseText.contains("followed") || lowerCaseText.contains("stalked")) {
            return "Harassment / Stalking";
        }
        if (lowerCaseText.contains("assault") || lowerCaseText.contains("hit") || lowerCaseText.contains("pushed")) {
            return "Physical Assault";
        }
        return "General Incident";
    }
}
