package com.example.codebase;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizer map view for entrant join locations.
 */
public class ViewEntrantMapActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private static final GeoPoint EDMONTON_FALLBACK_POINT =
            new GeoPoint(53.5461, -113.4938);

    private String eventId;
    private String eventTitle;

    private MapView mapView;
    private View progressBar;
    private View mapStateLayout;
    private TextView tvEventTitle;
    private TextView tvStateTitle;
    private TextView tvStateSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_view_entrant_map);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        if (eventTitle == null || eventTitle.trim().isEmpty()) {
            eventTitle = getString(R.string.untitled_event);
        }

        mapView = findViewById(R.id.entrantMapView);
        progressBar = findViewById(R.id.entrantMapProgressBar);
        mapStateLayout = findViewById(R.id.layoutEntrantMapState);
        tvEventTitle = findViewById(R.id.tvEntrantMapEventTitle);
        tvStateTitle = findViewById(R.id.tvEntrantMapStateTitle);
        tvStateSubtitle = findViewById(R.id.tvEntrantMapStateSubtitle);

        tvEventTitle.setText(eventTitle);
        findViewById(R.id.btnEntrantMapBack).setOnClickListener(v -> finish());

        configureMap();

        if (eventId == null || eventId.trim().isEmpty()) {
            progressBar.setVisibility(View.GONE);
            mapView.setVisibility(View.GONE);
            showState(
                    getString(R.string.entrant_map_error_title),
                    getString(R.string.entrant_map_error_subtitle)
            );
            Toast.makeText(this, R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        loadEntrantLocations();
    }

    private void configureMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.setTilesScaledToDpi(true);
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(EDMONTON_FALLBACK_POINT);
    }

    private void loadEntrantLocations() {
        progressBar.setVisibility(View.VISIBLE);
        mapStateLayout.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("entrantLocations")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<EntrantLocation> entrantLocations = new ArrayList<>();
                    snapshot.getDocuments().forEach(documentSnapshot -> {
                        EntrantLocation entrantLocation =
                                documentSnapshot.toObject(EntrantLocation.class);
                        if (entrantLocation != null && entrantLocation.hasValidCoordinates()) {
                            entrantLocations.add(entrantLocation);
                        }
                    });
                    renderEntrantLocations(entrantLocations);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showState(
                            getString(R.string.entrant_map_error_title),
                            getString(R.string.entrant_map_error_subtitle)
                    );
                    Toast.makeText(this, R.string.entrant_map_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void renderEntrantLocations(List<EntrantLocation> entrantLocations) {
        progressBar.setVisibility(View.GONE);
        mapView.getOverlays().clear();

        if (entrantLocations.isEmpty()) {
            mapView.getController().setZoom(10.0);
            mapView.getController().setCenter(EDMONTON_FALLBACK_POINT);
            showState(
                    getString(R.string.entrant_map_empty_title),
                    getString(R.string.entrant_map_empty_subtitle)
            );
            mapView.invalidate();
            return;
        }

        mapStateLayout.setVisibility(View.GONE);
        List<GeoPoint> points = new ArrayList<>();
        for (EntrantLocation entrantLocation : entrantLocations) {
            GeoPoint point = new GeoPoint(
                    entrantLocation.getLatitude(),
                    entrantLocation.getLongitude()
            );
            points.add(point);

            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(resolveMarkerTitle(entrantLocation));
            mapView.getOverlays().add(marker);
        }

        if (points.size() == 1) {
            mapView.getController().setZoom(13.5);
            mapView.getController().animateTo(points.get(0));
            mapView.invalidate();
            return;
        }

        mapView.post(() -> {
            BoundingBox bounds = BoundingBox.fromGeoPointsSafe(points);
            mapView.zoomToBoundingBox(bounds, true, 140);
            mapView.invalidate();
        });
    }

    private String resolveMarkerTitle(EntrantLocation entrantLocation) {
        String entrantName = entrantLocation.getEntrantName();
        if (entrantName == null || entrantName.trim().isEmpty()) {
            return getString(R.string.entrant_map_fallback_marker);
        }
        return entrantName;
    }

    private void showState(String title, String subtitle) {
        tvStateTitle.setText(title);
        tvStateSubtitle.setText(subtitle);
        mapStateLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }
}
