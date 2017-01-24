package pl.nkg.brq.android.maps;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import pl.nkg.brq.android.R;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TripMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private Intent intent;

    private GoogleMap mMap;
    private JSONArray tripDataArray;

    private float cameraZoom = 18.0f;
    private float polyWidth = 4.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_maps);

        intent = getIntent();
        String tripDataString = (String) intent.getExtras().get(getString(R.string.trip_array_key));

        try {
            tripDataArray = new JSONArray(tripDataString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        drawTrip();
    }

    private void drawTrip(){
        LatLng startPosition;

        try {
            startPosition = new LatLng(
                    tripDataArray.getJSONObject(0).getDouble("latitude"),
                    tripDataArray.getJSONObject(0).getDouble("longitude")
            );

            mMap.addMarker(new MarkerOptions().position(startPosition).title("Start"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, cameraZoom));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    width(polyWidth).
                    color(Color.BLUE)
            ;

            for (int i = 0; i < tripDataArray.length(); i++) {
                JSONObject value = tripDataArray.getJSONObject(i);
                polylineOptions.add(new LatLng(
                        (Double) value.get("latitude"),
                        (Double) value.get("longitude")
                ));
            }

            mMap.addPolyline(polylineOptions);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
