package com.example.codebase;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizer map view for entrant join locations.
 */
public class ViewEntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";

    private String eventId;
    private String eventTitle;

    private GoogleMap googleMap;
    private View progressBar;
    private View mapStateLayout;
    private TextView tvEventTitle;
    private TextView tvStateTitle;
    private TextView tvStateSubtitle;
    private View mapFragmentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entrant_map);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        if (eventTitle == null || eventTitle.trim().isEmpty()) {
            eventTitle = getString(R.string.untitled_event);
        }

        progressBar = findViewById(R.id.entrantMapProgressBar);
        mapStateLayout = findViewById(R.id.layoutEntrantMapState);
        tvEventTitle = findViewById(R.id.tvEntrantMapEventTitle);
        tvStateTitle = findViewById(R.id.tvEntrantMapStateTitle);
        tvStateSubtitle = findViewById(R.id.tvEntrantMapStateSubtitle);
        mapFragmentView = findViewById(R.id.entrantMapFragment);

        tvEventTitle.setText(eventTitle);
        findViewById(R.id.btnEntrantMapBack).setOnClickListener(v -> finish());

        if (!BuildConfig.MAPS_ENABLED) {
            progressBar.setVisibility(View.GONE);
            mapFragmentView.setVisibility(View.GONE);
            showState(
                    getString(R.string.entrant_map_disabled_title),
                    getString(R.string.entrant_map_disabled_subtitle)
            );
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            progressBar.setVisibility(View.GONE);
            mapFragmentView.setVisibility(View.GONE);
            showState(
                    getString(R.string.entrant_map_error_title),
                    getString(R.string.entrant_map_error_subtitle)
            );
            Toast.makeText(this, R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.entrantMapFragment);
        if (mapFragment == null) {
            mapFragmentView.setVisibility(View.GONE);
            showState(
                    getString(R.string.entrant_map_error_title),
                    getString(R.string.entrant_map_error_subtitle)
            );
            progressBar.setVisibility(View.GONE);
            return;
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap readyMap) {
        googleMap = readyMap;
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        loadEntrantLocations();
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
        if (googleMap == null) {
            return;
        }

        googleMap.clear();
        if (entrantLocations.isEmpty()) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(53.5461, -113.4938),
                    10f
            ));
            showState(
                    getString(R.string.entrant_map_empty_title),
                    getString(R.string.entrant_map_empty_subtitle)
            );
            return;
        }

        mapStateLayout.setVisibility(View.GONE);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (EntrantLocation entrantLocation : entrantLocations) {
            LatLng latLng = new LatLng(
                    entrantLocation.getLatitude(),
                    entrantLocation.getLongitude()
            );
            boundsBuilder.include(latLng);
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(resolveMarkerTitle(entrantLocation))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (entrantLocations.size() == 1) {
            EntrantLocation single = entrantLocations.get(0);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(single.getLatitude(), single.getLongitude()),
                    13.5f
            ));
            return;
        }

        mapFragmentView.post(() -> {
            LatLngBounds bounds = boundsBuilder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140));
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
}
