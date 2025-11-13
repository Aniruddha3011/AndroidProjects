package com.example.emergencysos.Screens;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.emergencysos.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class CrimeChartActivity extends AppCompatActivity {

    BarChart barChart;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_chart);

        barChart = findViewById(R.id.barChart);
        loadCrimeData();
    }

    private void loadCrimeData() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        try {
            InputStream is = getAssets().open("crime_Data");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            JSONArray dataArray = root.getJSONArray("data");

            int index = 0;

            for (int i = 0; i < dataArray.length(); i++) {
                JSONArray row = dataArray.getJSONArray(i);
                String state = row.getString(1);
                String crime2022 = row.getString(4); // index 4 is "2022"

                // Skip total rows
                if (state.toLowerCase().contains("total")) continue;

                try {
                    int crimeCount = Integer.parseInt(crime2022.replace(",", ""));
                    entries.add(new BarEntry(index, crimeCount));
                    labels.add(state);
                    index++;
                } catch (NumberFormatException e) {
                    Log.e("CrimeChart", "Invalid number for state " + state + ": " + crime2022);
                }
            }

            if (entries.isEmpty()) {
                Log.e("CrimeChart", "No data parsed");
                return;
            }

            BarDataSet dataSet = new BarDataSet(entries, "Crimes in 2022");
            dataSet.setColor(ContextCompat.getColor(this, R.color.purple));

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.9f);
            barChart.setData(barData);

            Description description = new Description();
            description.setText("State-wise Crime Count (2022)");
            barChart.setDescription(description);
            barChart.setFitBars(true);
            barChart.animateY(1000);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);
            xAxis.setLabelRotationAngle(-45);

            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int i = Math.round(value);
                    return i >= 0 && i < labels.size() ? labels.get(i) : "";
                }
            });

            barChart.getAxisRight().setEnabled(false);
            barChart.invalidate();

        } catch (Exception e) {
            Log.e("CrimeChart", "Error loading JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
