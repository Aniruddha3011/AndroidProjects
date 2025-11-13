package com.example.emergencysos.Screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.emergencysos.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * NavigationActivity updated to include:
 *  - top turn instruction banner
 *  - floating controls (report/mute/compass)
 *  - bottom panel with ETA and distance + Stop button
 *
 * Core navigation logic (parsing steps, TTS, rerouting) preserved.
 */
public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextToSpeech tts;
    private Marker userMarker;
    private Polyline routePolyline;
    private List<LatLng> routePoints = new ArrayList<>();
    private List<NavigationStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private LatLng destination;
    private String directionsJson;
    private TextView tvInstruction;
    private TextView tvTopInstruction;
    private TextView tvEta;
    private TextView tvRemainDistance;
    private Button btnStop;
    private ImageButton btnReport, btnMute, btnCompass;
    private final List<Polyline> stepPolylines = new ArrayList<>();
    private final AtomicBoolean rerouting = new AtomicBoolean(false);

    private LocationCallback locationCallback; // stored callback

    private static final float STEP_ARRIVAL_THRESHOLD_METERS = 30f;
    private static final float OFF_ROUTE_THRESHOLD_METERS = 50f;

    private static final DecimalFormat DIST_FORMAT = new DecimalFormat("#.##");

    // new: which route index in the directions JSON we should use
    private int selectedRouteIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // UI
        tvInstruction = findViewById(R.id.tv_instruction);
        tvTopInstruction = findViewById(R.id.tv_top_instruction);
        tvEta = findViewById(R.id.tv_eta);
        tvRemainDistance = findViewById(R.id.tv_remain_distance);
        btnStop = findViewById(R.id.btn_stop_nav);
        btnReport = findViewById(R.id.btn_report);
        btnMute = findViewById(R.id.btn_mute);
        btnCompass = findViewById(R.id.btn_compass);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        directionsJson = getIntent().getStringExtra("directions_json");
        double dlat = getIntent().getDoubleExtra("destination_lat", 0);
        double dlng = getIntent().getDoubleExtra("destination_lng", 0);
        destination = new LatLng(dlat, dlng);

        selectedRouteIndex = getIntent().getIntExtra("selected_route_index", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        btnStop.setOnClickListener(v -> finish());

        // sample handlers for floating buttons (you can wire your own actions)
        btnReport.setOnClickListener(v -> {
            Toast.makeText(this, "Report pressed", Toast.LENGTH_SHORT).show();
            // TODO: open report UI
        });

        btnMute.setOnClickListener(v -> {
            // mute/unmute TTS
            if (tts != null) {
                if (tts.isSpeaking()) {
                    tts.stop();
                    Toast.makeText(this, "Muted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "TTS ready", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCompass.setOnClickListener(v -> {
            if (mMap != null && userMarker != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), 18));
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // optional: set UI settings to look more like navigation
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        if (directionsJson != null) {
            parseDirectionsAndDraw(directionsJson);
        } else {
            Toast.makeText(this, "No saved directions JSON. Please restart route from main map.", Toast.LENGTH_LONG).show();
        }
        startLocationUpdates();
    }

    private void parseDirectionsAndDraw(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray routesArr = root.getJSONArray("routes");
            if (routesArr.length() == 0) return;

            // ensure selectedRouteIndex is within bounds
            int routeIndexToUse = Math.max(0, Math.min(selectedRouteIndex, routesArr.length() - 1));
            JSONObject route = routesArr.getJSONObject(routeIndexToUse);

            String overviewPoints = route.getJSONObject("overview_polyline").getString("points");
            routePoints = decodePolyline(overviewPoints);

            if (routePolyline != null) routePolyline.remove();
            routePolyline = mMap.addPolyline(new PolylineOptions().addAll(routePoints).width(16f)); // thicker for nav

            // prepare steps
            steps.clear();
            JSONArray legs = route.getJSONArray("legs");
            if (legs.length() > 0) {
                JSONArray stepsArr = legs.getJSONObject(0).getJSONArray("steps");
                for (int i = 0; i < stepsArr.length(); i++) {
                    JSONObject s = stepsArr.getJSONObject(i);
                    String htmlInstr = s.optString("html_instructions", "");
                    String instr;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        instr = Html.fromHtml(htmlInstr, Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        instr = Html.fromHtml(htmlInstr).toString();
                    }
                    JSONObject startLoc = s.getJSONObject("start_location");
                    JSONObject endLoc = s.getJSONObject("end_location");
                    double sLat = startLoc.getDouble("lat");
                    double sLng = startLoc.getDouble("lng");
                    double eLat = endLoc.getDouble("lat");
                    double eLng = endLoc.getDouble("lng");
                    String stepPolyline = s.getJSONObject("polyline").getString("points");
                    List<LatLng> stepPts = decodePolyline(stepPolyline);
                    int dist = s.getJSONObject("distance").optInt("value", 0);

                    steps.add(new NavigationStep(instr, new LatLng(sLat, sLng), new LatLng(eLat, eLng), stepPts, dist));
                }
            }

            // draw small invisible step polylines (kept for debugging)
            for (Polyline p : stepPolylines) p.remove();
            stepPolylines.clear();
            for (NavigationStep step : steps) {
                Polyline p = mMap.addPolyline(new PolylineOptions().addAll(step.polyPoints).width(8f).visible(false));
                stepPolylines.add(p);
            }

            if (!routePoints.isEmpty()) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routePoints.get(0), 16));
            }

            currentStepIndex = 0;
            if (!steps.isEmpty()) {
                showAndSpeakStep(steps.get(0));
            }

            // update bottom panel initially
            updateBottomPanel();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to parse directions.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 201);
            return;
        }

        LocationRequest request = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // store callback so we can remove it later
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location loc = locationResult.getLastLocation();
                if (loc == null) return;

                LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
                if (userMarker == null) {
                    userMarker = mMap.addMarker(new MarkerOptions().position(ll).title("You"));
                } else {
                    userMarker.setPosition(ll);
                }

                // follow
                mMap.animateCamera(CameraUpdateFactory.newLatLng(ll));

                // step arrival check
                if (currentStepIndex < steps.size()) {
                    NavigationStep current = steps.get(currentStepIndex);
                    float[] res = new float[1];
                    Location.distanceBetween(ll.latitude, ll.longitude, current.end.latitude, current.end.longitude, res);
                    if (res[0] <= STEP_ARRIVAL_THRESHOLD_METERS) {
                        currentStepIndex++;
                        if (currentStepIndex < steps.size()) {
                            showAndSpeakStep(steps.get(currentStepIndex));
                        } else {
                            showAndSpeak("You have arrived at your destination.");
                        }
                        updateBottomPanel();
                    }
                }

                // off-route detection
                float minDist = Float.MAX_VALUE;
                for (LatLng p : routePoints) {
                    float[] r = new float[1];
                    Location.distanceBetween(ll.latitude, ll.longitude, p.latitude, p.longitude, r);
                    if (r[0] < minDist) minDist = r[0];
                }

                if (minDist > OFF_ROUTE_THRESHOLD_METERS && !rerouting.get()) {
                    rerouting.set(true);
                    performReroute(ll);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
    }

    private void showAndSpeakStep(NavigationStep step) {
        showAndSpeak(step.instruction);
        // update top banner text
        runOnUiThread(() -> {
            tvTopInstruction.setText(step.instruction);
            tvInstruction.setText(step.instruction);
        });
    }

    private void showAndSpeak(String text) {
        runOnUiThread(() -> tvInstruction.setText(text));
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_ADD, null, "NAV_TTS");
    }

    private void performReroute(LatLng origin) {
        new Thread(() -> {
            try {
                String apiKey = getString(R.string.google_map_api);
                String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                        + origin.latitude + "," + origin.longitude
                        + "&destination=" + destination.latitude + "," + destination.longitude
                        + "&mode=driving&alternatives=false&key=" + apiKey;

                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder().url(url).build();
                Response resp = client.newCall(req).execute();
                if (!resp.isSuccessful()) {
                    rerouting.set(false);
                    return;
                }
                String json = resp.body().string();

                runOnUiThread(() -> {
                    // on reroute we will re-parse and draw the new route (we keep using index 0 from new response)
                    parseDirectionsAndDraw(json);
                    updateBottomPanel();
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rerouting.set(false);
            }
        }).start();
    }

    /**
     * Update bottom panel: remaining distance and ETA (simple estimate)
     */
    private void updateBottomPanel() {
        // sum distances of remaining steps
        int remainMeters = 0;
        for (int i = Math.max(0, currentStepIndex); i < steps.size(); i++) {
            remainMeters += steps.get(i).distanceMeters;
        }

        double remainKm = remainMeters / 1000.0;
        // estimate time: assume avg speed 40 km/h (you can improve)
        double avgSpeedKmph = 40.0;
        double hours = (remainKm / avgSpeedKmph);
        int minutes = (int) Math.round(hours * 60);

        runOnUiThread(() -> {
            tvRemainDistance.setText(DIST_FORMAT.format(remainKm) + " km");
            tvEta.setText((minutes <= 1 ? "<1" : String.valueOf(minutes)) + " min");
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        // remove location updates
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private static class NavigationStep {
        String instruction;
        LatLng start;
        LatLng end;
        List<LatLng> polyPoints;
        int distanceMeters;

        NavigationStep(String instruction, LatLng start, LatLng end, List<LatLng> polyPoints, int distanceMeters) {
            this.instruction = instruction;
            this.start = start;
            this.end = end;
            this.polyPoints = polyPoints;
            this.distanceMeters = distanceMeters;
        }
    }
}
