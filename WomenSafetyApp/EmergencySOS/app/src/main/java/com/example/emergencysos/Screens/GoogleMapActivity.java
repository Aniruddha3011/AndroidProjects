package com.example.emergencysos.Screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private int lastNearbyCount = -1;

    private FirebaseFirestore firestore;
    private androidx.appcompat.widget.SearchView searchView;
    private List<CrimeZone> zones = new ArrayList<>();
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final String CHANNEL_ID = "zone_alert_channel";
    private boolean notified = false;
    private LatLng currentLatLng;
    private Polyline currentRoute;
    private LatLng currentDestination;
    private Button fabNavigate;
    private TextView tvNearbyCount;
    private final List<Polyline> routePolylines = new ArrayList<>();


    private String lastDirectionsJson = null;

    private int selectedRouteIndex = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        searchView = findViewById(R.id.map_search);
        tvNearbyCount = findViewById(R.id.tvNearbyCount);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
        fabNavigate = findViewById(R.id.fab_navigate);

        // Open in-app navigation instead of external Google Maps app
        fabNavigate.setOnClickListener(view -> {
            if (currentDestination != null && lastDirectionsJson != null) {
                Intent i = new Intent(GoogleMapActivity.this, NavigationActivity.class);
                i.putExtra("directions_json", lastDirectionsJson);
                i.putExtra("destination_lat", currentDestination.latitude);
                i.putExtra("destination_lng", currentDestination.longitude);
                i.putExtra("selected_route_index", selectedRouteIndex);
                startActivity(i);
            } else {
                Toast.makeText(this, "Please search and draw a route first.", Toast.LENGTH_SHORT).show();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Zone Alerts", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        setupSearch();

        @SuppressLint("RestrictedApi")
        androidx.appcompat.widget.SearchView.SearchAutoComplete searchAutoComplete =
                searchView.findViewById(androidx.appcompat.R.id.search_src_text);

        if (searchAutoComplete != null) {
            searchAutoComplete.setTypeface(Typeface.DEFAULT_BOLD);
            searchAutoComplete.setTextColor(getResources().getColor(android.R.color.black));
            searchAutoComplete.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        }

    }

    private void countNearbyPeople(Location myLocation) {
        FirebaseFirestore.getInstance().collection("users_locations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;
                    String myUid = FirebaseAuth.getInstance().getUid();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (!doc.getId().equals(myUid)) {
                            Double lat = doc.getDouble("lat");
                            Double lng = doc.getDouble("lng");
                            if (lat != null && lng != null) {
                                float[] distance = new float[1];
                                Location.distanceBetween(
                                        myLocation.getLatitude(), myLocation.getLongitude(),
                                        lat, lng,
                                        distance
                                );
                                if (distance[0] <= 500) {
                                    count++;
                                }
                            }
                        }
                    }

                    if (count != lastNearbyCount) {
                        lastNearbyCount = count;
                        tvNearbyCount.setText("Nearby: " + count); // ⬅ NEW
                    }
                });
    }


    private void startNearbyListener() {
        FirebaseFirestore.getInstance()
                .collection("users_locations")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || currentLatLng == null) return;

                    int count = 0;
                    String myUid = FirebaseAuth.getInstance().getUid();

                    for (DocumentSnapshot doc : snapshots) {
                        if (!doc.getId().equals(myUid)) {
                            Double lat = doc.getDouble("lat");
                            Double lng = doc.getDouble("lng");
                            if (lat != null && lng != null) {
                                float[] distance = new float[1];
                                Location.distanceBetween(
                                        currentLatLng.latitude, currentLatLng.longitude,
                                        lat, lng,
                                        distance
                                );
                                if (distance[0] <= 500) {
                                    count++;
                                }
                            }
                        }
                    }

                    if (count != lastNearbyCount) {
                        lastNearbyCount = count;
                        tvNearbyCount.setText("Nearby: " + count);
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
                detectStateAndLoadZones(location);
                startLocationUpdates();
                startNearbyListener();
            }
        });
    }

    private void detectStateAndLoadZones(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                String state = addresses.get(0).getAdminArea();
                if (state != null) fetchCrimeZones(state.toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create().setInterval(5000).setFastestInterval(3000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        checkUserInDangerZone(location);
                    }
                }
            }, getMainLooper());
        }
    }

    private void checkUserInDangerZone(Location userLocation) {
        for (CrimeZone zone : zones) {
            float[] result = new float[1];
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), zone.center.latitude, zone.center.longitude, result);
            if (result[0] <= zone.radius && !notified) {
                sendNotification("Alert", "You are in a " + zone.level + " risk area.");
                notified = true;
                break;
            }
        }
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocationOnMap(query);
                fetchCrimeZones(query.toLowerCase().trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void searchLocationOnMap(String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng destination = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 12));
                currentDestination = destination;

                if (currentRoute != null) currentRoute.remove();
                drawRouteTo(destination);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchCrimeZones(String state) {
        firestore.collection("crime_zones").document(state).get().addOnSuccessListener(documentSnapshot -> {
            zones.clear();
            mMap.clear();
            parseZones(documentSnapshot);
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load zones.", Toast.LENGTH_SHORT).show());
    }

    private void parseZones(DocumentSnapshot documentSnapshot) {
        List<Object> zoneList = (List<Object>) documentSnapshot.get("zones");
        if (zoneList != null) {
            for (Object obj : zoneList) {
                Map<String, Object> zoneMap = (Map<String, Object>) obj;
                double lat = ((Number) zoneMap.get("lat")).doubleValue();
                double lng = ((Number) zoneMap.get("lng")).doubleValue();
                double radius = ((Number) zoneMap.get("radius")).doubleValue();
                String level = (String) zoneMap.get("level");

                CrimeZone zone = new CrimeZone(new LatLng(lat, lng), level, radius);
                zones.add(zone);
            }
            drawCrimeZones();
        }
    }

    private void drawCrimeZones() {
        for (CrimeZone zone : zones) {
            int strokeColor = zone.getStrokeColor();
            int fillColor = zone.getFillColor();

            List<LatLng> points = new ArrayList<>();
            double r = zone.radius / 111000.0;
            double lat = zone.center.latitude;
            double lng = zone.center.longitude;

            for (int i = 0; i < 360; i += 30) {
                double angle = Math.toRadians(i);
                points.add(new LatLng(lat + r * Math.cos(angle), lng + r * Math.sin(angle)));
            }

            mMap.addPolygon(new PolygonOptions().addAll(points).strokeColor(strokeColor).fillColor(fillColor).strokeWidth(3));
            mMap.addMarker(new MarkerOptions().position(zone.center).title(zone.level + " Risk Zone"));
        }
    }

    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.warning)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build());
        }
    }

    private void drawRouteTo(LatLng destination) {
        if (currentLatLng == null) {
            Toast.makeText(this, "Current location not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&alternatives=true&mode=driving" +
                "&key=AIzaSyAaOYMb_7BUpxnwIRNtYCXS_DmWs64tEqU";

        OkHttpClient client = new OkHttpClient();

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(this, "API call failed", Toast.LENGTH_SHORT).show());
                    return;
                }

                String json = response.body().string();

                // save the directions JSON so our in-app navigator can reuse it
                lastDirectionsJson = json;

                JSONObject root = new JSONObject(json);

                String status = root.optString("status", "UNKNOWN");
                if (!"OK".equals(status)) {
                    runOnUiThread(() -> Toast.makeText(this, "Directions API error: " + status, Toast.LENGTH_LONG).show());
                    return;
                }

                JSONArray routes = root.getJSONArray("routes");

                // compute a risk score for every route, pick the lowest
                final double[] routeScores = new double[routes.length()];
                for (int i = 0; i < routes.length(); i++) {
                    JSONObject route = routes.getJSONObject(i);
                    JSONObject overview = route.getJSONObject("overview_polyline");
                    String points = overview.getString("points");
                    List<LatLng> decodedPath = decodePolyline(points);
                    double score = computeRiskScore(decodedPath);
                    routeScores[i] = score;
                }

                // find smallest score (safest)
                double min = Double.MAX_VALUE;
                int bestIdx = 0;
                for (int i = 0; i < routeScores.length; i++) {
                    if (routeScores[i] < min) {
                        min = routeScores[i];
                        bestIdx = i;
                    }
                }
                selectedRouteIndex = bestIdx; // store selected route index

                runOnUiThread(() -> {
                    for (Polyline poly : routePolylines) poly.remove();
                    routePolylines.clear();

                    for (int i = 0; i < routes.length(); i++) {
                        try {
                            JSONObject route = routes.getJSONObject(i);
                            JSONObject overview = route.getJSONObject("overview_polyline");
                            String points = overview.getString("points");
                            List<LatLng> decodedPath = decodePolyline(points);

                            // color selected route green, others red
                            int color = (i == selectedRouteIndex) ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");

                            Polyline polyline = mMap.addPolyline(new PolylineOptions()
                                    .addAll(decodedPath)
                                    .color(color)
                                    .width(10f));
                            routePolylines.add(polyline);

                            // if this is the selected route, keep reference and zoom to it
                            if (i == selectedRouteIndex) {
                                if (currentRoute != null) currentRoute.remove();
                                currentRoute = polyline;
                                if (!decodedPath.isEmpty()) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(decodedPath.get(0), 14));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // optional: show a small hint to user about choice
                    Toast.makeText(this, "Safest route selected (green).", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Compute a simple numeric risk score for a polyline path.
     * Lower = safer. Uses your existing CrimeZone list and basic heuristics.
     */
    private double computeRiskScore(List<LatLng> pathPoints) {
        if (pathPoints == null || pathPoints.isEmpty()) return Double.MAX_VALUE;

        double total = 0.0;
        int count = 0;

        for (LatLng pt : pathPoints) {
            double pointRisk = 0.0; // 0 (safe) .. 1 (danger)
            for (CrimeZone zone : zones) {
                float[] result = new float[1];
                Location.distanceBetween(pt.latitude, pt.longitude, zone.center.latitude, zone.center.longitude, result);
                if (result[0] <= zone.radius) {
                    // weight by zone level
                    String lvl = zone.level == null ? "" : zone.level.trim().toLowerCase();
                    if (lvl.contains("high")) pointRisk = Math.max(pointRisk, 1.0);
                    else if (lvl.contains("medium")) pointRisk = Math.max(pointRisk, 0.6);
                    else if (lvl.contains("low")) pointRisk = Math.max(pointRisk, 0.3);
                    else pointRisk = Math.max(pointRisk, 0.5);
                }
            }
            total += pointRisk;
            count++;
        }


        double avg = (count == 0) ? 0.0 : (total / count);

        // optionally penalize longer paths through risky areas by scaling with fraction of points risky
        int riskyCount = 0;
        for (LatLng pt : pathPoints) {
            for (CrimeZone zone : zones) {
                float[] result = new float[1];
                Location.distanceBetween(pt.latitude, pt.longitude, zone.center.latitude, zone.center.longitude, result);
                if (result[0] <= zone.radius && (zone.level != null && zone.level.equalsIgnoreCase("High"))) {
                    riskyCount++;
                    break;
                }
            }
        }
        double riskyFraction = (pathPoints.size() == 0) ? 0.0 : ((double) riskyCount / pathPoints.size());

      
        return avg + 0.7 * riskyFraction;
    }

    private boolean isPathDangerous(List<LatLng> pathPoints) {
        for (LatLng point : pathPoints) {
            for (CrimeZone zone : zones) {
                float[] result = new float[1];
                Location.distanceBetween(point.latitude, point.longitude, zone.center.latitude, zone.center.longitude, result);
                if (result[0] <= zone.radius && zone.level.equalsIgnoreCase("High")) {
                    return true;
                }
            }
        }
        return false;
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
}
