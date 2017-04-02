package pl.nkg.brq.android.maps;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.R;
import pl.nkg.brq.android.network.NetworkGetTrip;

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

import java.util.concurrent.ExecutionException;

public class TripMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private Intent intent;

    private GoogleMap mMap;

    private JSONArray tripDataArray;
    private String tripBikeUsed;
    private String tripPhonePlacement;

    // Domyślne przybliżenie mapy
    private static float cameraZoom = 17.0f;
    // Szerokość linii którą rysujemy drogę
    private static float polyWidth = 4.0f;

    // Tablica z kolorami dla poszczególnych ocen
    private int[] colorGradeList = new int[11];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_maps);
        this.bindColors();

        intent = getIntent();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void bindColors(){
        colorGradeList[1] = ConstValues.colorGradeOne;
        colorGradeList[2] = ConstValues.colorGradeTwo;
        colorGradeList[3] = ConstValues.colorGradeThree;
        colorGradeList[4] = ConstValues.colorGradeFour;
        colorGradeList[5] = ConstValues.colorGradeFive;
        colorGradeList[6] = ConstValues.colorGradeSix;
        colorGradeList[7] = ConstValues.colorGradeSeven;
        colorGradeList[8] = ConstValues.colorGradeEight;
        colorGradeList[9] = ConstValues.colorGradeNine;
        colorGradeList[10] = ConstValues.colorGradeTen;
    }

    /**
     * Przy załadowaniu mapy pobieramy z serwera dane do wyświetlenia i rysujemy je na mapie
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            String tripId = intent.getExtras().getString("tripId");

            String tripResponseString = new NetworkGetTrip().execute(tripId).get();
            JSONObject tripResponseJson = new JSONObject(tripResponseString);

            String tripDataString = tripResponseJson.getString("trip_data");
            JSONObject tripDataObject = new JSONObject(tripDataString);

            tripDataArray = tripDataObject.getJSONArray("trip_data");
            tripBikeUsed = tripResponseJson.getString("bike_used");
            tripPhonePlacement = tripResponseJson.getString("phone_placement");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        drawTrip();
    }

    /**
     * Rysujemy polyline reprezentujący odpowiednimi kolorami przebytą drogę
     */
    private void drawTrip(){
        LatLng startPosition;
        LatLng endPosition;

        try {
            startPosition = new LatLng(
                tripDataArray.getJSONObject(0).getDouble("latitude"),
                tripDataArray.getJSONObject(0).getDouble("longitude")
            );

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, cameraZoom));
            mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("Start")
                .snippet("Bike: " + tripBikeUsed + ", place: " + tripPhonePlacement)).showInfoWindow();

            endPosition = new LatLng(
                    tripDataArray.getJSONObject(tripDataArray.length() - 1).getDouble("latitude"),
                    tripDataArray.getJSONObject(tripDataArray.length() - 1).getDouble("longitude")
            );

            mMap.addMarker(new MarkerOptions()
                    .position(endPosition)
                    .title("End"));

            JSONObject currentRecord;
            JSONObject nextRecord = tripDataArray.getJSONObject(1);

            for (int i = 1; i < tripDataArray.length() - 1; i++) {
                currentRecord = nextRecord;
                nextRecord = tripDataArray.getJSONObject(i);

                PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true)
                    .width(polyWidth)
                    .color(colorGradeList[((Double) currentRecord.getDouble("rating")).intValue()]);

                polylineOptions.add(new LatLng(
                    currentRecord.getDouble("latitude"),
                    currentRecord.getDouble("longitude")
                ));

                polylineOptions.add(new LatLng(
                    nextRecord.getDouble("latitude"),
                    nextRecord.getDouble("longitude")
                ));

                mMap.addPolyline(polylineOptions);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
