package pl.nkg.brq.android.maps;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.R;

public class TerrainMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private Intent intent;

    private GoogleMap mMap;
    private JSONArray terrainDataArray;

    private static float cameraZoom = 17.0f;
    private static float cameraMaxZoom = 15.0f;
    private static float polyWidth = 1.0f;
    private static float mapOffset = 0.00005f;

    private int[] colorGradeList = new int[11];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_maps);
        this.bindColors();

        intent = getIntent();
        String terrainDataString = (String) intent.getExtras().get(getString(R.string.trip_array_key));

        try {
            terrainDataArray = new JSONArray(terrainDataString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void bindColors(){
        colorGradeList[1] = ConstValues.colorTransparentGradeOne;
        colorGradeList[2] = ConstValues.colorTransparentGradeTwo;
        colorGradeList[3] = ConstValues.colorTransparentGradeThree;
        colorGradeList[4] = ConstValues.colorTransparentGradeFour;
        colorGradeList[5] = ConstValues.colorTransparentGradeFive;
        colorGradeList[6] = ConstValues.colorTransparentGradeSix;
        colorGradeList[7] = ConstValues.colorTransparentGradeSeven;
        colorGradeList[8] = ConstValues.colorTransparentGradeEight;
        colorGradeList[9] = ConstValues.colorTransparentGradeNine;
        colorGradeList[10] = ConstValues.colorTransparentGradeTen;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng startPosition;
        try {
            startPosition = new LatLng(
                    terrainDataArray.getJSONObject(0).getDouble("latitude"),
                    terrainDataArray.getJSONObject(0).getDouble("longitude")
            );

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, cameraZoom));
            mMap.setOnCameraIdleListener(this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraIdle() {
        drawTerrain();
    }

    private void drawTerrain(){
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        double north = bounds.northeast.latitude;
        double south = bounds.southwest.latitude;
        double west = bounds.northeast.longitude;
        double east = bounds.southwest.longitude;
        double zoom = mMap.getCameraPosition().zoom;

        Log.d("myAPP", "N: " + Double.toString(north) + " " +
                "S: " + Double.toString(south) + " " +
                "W: " + Double.toString(west) + " " +
                "E: " + Double.toString(east) + " " +
                "Zoom: " + Double.toString(zoom) + " ");

        if ( zoom < cameraMaxZoom ) {
            mMap.clear();
            return;
        }

        String terrainDataString = (String) intent.getExtras().get(getString(R.string.trip_array_key));

        try {
            terrainDataArray = new JSONArray(terrainDataString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            mMap.clear();

            for (int i = 0; i < terrainDataArray.length(); i++) {
                JSONObject record = terrainDataArray.getJSONObject(i);

                PolygonOptions polygonOptions = new PolygonOptions().
                        geodesic(true).
                        strokeWidth(polyWidth).
                        fillColor(colorGradeList[((Double) record.getDouble("rating")).intValue()]).
                        strokeColor(Color.rgb(255, 255, 255));

                polygonOptions.add(new LatLng(record.getDouble("latitude") + mapOffset, record.getDouble("longitude") + mapOffset),
                                   new LatLng(record.getDouble("latitude") + mapOffset, record.getDouble("longitude") - mapOffset),
                                   new LatLng(record.getDouble("latitude") - mapOffset, record.getDouble("longitude") - mapOffset),
                                   new LatLng(record.getDouble("latitude") - mapOffset, record.getDouble("longitude") + mapOffset));

                mMap.addPolygon(polygonOptions);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
