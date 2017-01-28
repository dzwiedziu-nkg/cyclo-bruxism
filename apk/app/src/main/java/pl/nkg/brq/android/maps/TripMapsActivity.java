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

    private final static int colorGradeOne = Color.rgb(58, 240, 22);
    private final static int colorGradeTwo = Color.rgb(142, 240, 22);
    private final static int colorGradeThree = Color.rgb(203, 240, 22);
    private final static int colorGradeFour = Color.rgb(240, 240, 22);
    private final static int colorGradeFive = Color.rgb(240, 210, 22);
    private final static int colorGradeSix = Color.rgb(240, 182, 22);
    private final static int colorGradeSeven = Color.rgb(240, 140, 22);
    private final static int colorGradeEight = Color.rgb(240, 113, 22);
    private final static int colorGradeNine = Color.rgb(240, 70, 22);
    private final static int colorGradeTen = Color.rgb(240, 40, 22);

    private int[] colorGradeList = new int[11];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_maps);
        this.bindColors();

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

    private void bindColors(){
        colorGradeList[1] = colorGradeOne;
        colorGradeList[2] = colorGradeTwo;
        colorGradeList[3] = colorGradeThree;
        colorGradeList[4] = colorGradeFour;
        colorGradeList[5] = colorGradeFive;
        colorGradeList[6] = colorGradeSix;
        colorGradeList[7] = colorGradeSeven;
        colorGradeList[8] = colorGradeEight;
        colorGradeList[9] = colorGradeNine;
        colorGradeList[10] = colorGradeTen;
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

            JSONObject valueCurrent;
            JSONObject valueNext = tripDataArray.getJSONObject(1);

            for (int i = 1; i < tripDataArray.length() - 1; i++) {
                valueCurrent = valueNext;
                valueNext = tripDataArray.getJSONObject(i);

                PolylineOptions polylineOptions = new PolylineOptions().
                        geodesic(true).
                        width(polyWidth).
                        color(colorGradeList[((Double) valueCurrent.getDouble("rating")).intValue()]);

                polylineOptions.add(new LatLng(
                        valueCurrent.getDouble("latitude"),
                        valueCurrent.getDouble("longitude")
                ));

                polylineOptions.add(new LatLng(
                        valueNext.getDouble("latitude"),
                        valueNext.getDouble("longitude")
                ));

                mMap.addPolyline(polylineOptions);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
