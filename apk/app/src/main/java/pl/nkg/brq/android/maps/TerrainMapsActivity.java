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

import java.util.concurrent.ExecutionException;

import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.R;
import pl.nkg.brq.android.network.NetworkGetRating;

public class TerrainMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private Intent intent;

    private GoogleMap mMap;
    private JSONArray terrainDataArray;

    // Domyślne przybliżenie mapy
    private static float cameraZoom = 17.0f;
    // Przybliżenie powyżeje którego mapa przestanie ładować dane ( mniejsza wartość to większe oddalenie mapy! )
    private static float cameraMaxZoom = 14.0f;
    // Wyrażony procentowo obszar ekranu poza któym ładują się dane.
    // Przykładowo przy 0.1f: 10% szerokości wyświetlonej mapy po obu stronach jest załadowane.
    private static float cameraMargin = 0.2f;

    private static float polyWidth = 1.0f;
    private static float mapOffset = 0.00005f;

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

    // Przy załadowaniu mapy ustawiamy jej początkową lokacje
    // CameraIdleListener doświerza dane na mapie za każdym razem kiedy zakończone jest jej przesuwanie
    // ( a także przy jej pierwszym włączeniu )
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng startPosition = new LatLng(50.0847, 19.9596);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, cameraZoom));

        mMap.setOnCameraIdleListener(this);
    }

    @Override
    public void onCameraIdle() {
        drawTerrain();
    }

    // Metoda która odczytuje aktualne wymiary mapy ( szerokość i długość geograficzną jej krańców )
    // i wysyła odpowiednie zapytanie o dane na serwer.
    private void drawTerrain(){
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        double north = bounds.northeast.latitude;
        double south = bounds.southwest.latitude;
        double west = bounds.northeast.longitude;
        double east = bounds.southwest.longitude;
        double zoom = mMap.getCameraPosition().zoom;

        if (zoom < cameraMaxZoom) {
            mMap.clear();
            return;
        }

        double nsOffset = (north - south) * cameraMargin;
        double weOffset = (west - east) * cameraMargin;

        north += nsOffset;
        south -= nsOffset;
        west += weOffset;
        east -= weOffset;

        if (!getData(north, south, east, west)){
            Toast.makeText(getApplicationContext(), R.string.network_problems_toast, Toast.LENGTH_SHORT).show();
            return;
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

    private boolean getData(double north, double south, double east, double west){
        try {
            String terrainDataString = new NetworkGetRating().execute(
                    Double.toString(north),
                    Double.toString(south),
                    Double.toString(east),
                    Double.toString(west)
            ).get();

            if (terrainDataString == null) {
                return false;
            }

            JSONObject tripResponseJson = new JSONObject(terrainDataString);
            terrainDataArray = tripResponseJson.getJSONArray("array");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return true;
    }
}
