package pl.nkg.brq.android.maps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
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

    // Domyślne przybliżenie mapy ( mniejsza wartość to większe oddalenie mapy! )
    private float cameraZoom = 17.0f;
    // Przybliżenie powyżeje którego mapa zmniejsza rozdzielczość i kumuluje dane w większe kwadraty
    private static float cameraResTwoZoom = 16.5f;
    private static float cameraResThreeZoom = 15.25f;
    private static float cameraResFourZoom = 13.75f;
    private static float cameraResFiveZoom = 12.0f;
    // Przybliżenie powyżeje którego mapa przestanie ładować dane
    private static float cameraMinZoom = 10.0f;
    // Wyrażony procentowo obszar ekranu poza któym ładują się dane.
    // Przykładowo przy 0.1f: 10% szerokości wyświetlonej mapy po obu stronach jest załadowane.
    private static float cameraMargin = 0.2f;

    // Szerokość boków rysowanych kwadratów
    private static float polyWidth = 1.0f;
    // Wartości wykorzystywane do obliczeń i wyśrodkowania kwadratów dla poszczególnych rozdzielczości
    private static float resOneMapOffset = 0.00005f;
    private static float resTwoMapOffset = 0.00025f;
    private static float resThreeMapOffset = 0.0005f;
    private static float resFourMapOffset = 0.0025f;
    private static float resFiveMapOffset = 0.005f;

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

    /**
     * Przypisanie kolorów do odpoewiednich ocen
     */
    private void bindColors() {
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

    /**
     * Przy załadowaniu mapy ustawiamy jej początkową lokacje
     * CameraIdleListener odświeża dane na mapie za każdym razem kiedy zakończone jest
     * jej przesuwanie, a także przy jej pierwszym włączeniu
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng startPosition = null;

        // Pobieramy obecne położenie użytkownika i jeśli się uda to przesuwamy tam mapę
        // Tych danych często nie da się pobrać ponieważ istnieją tylko jeżeli używaliśmy wcześniej GPS
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Domyślne położenie mapy (Kraków), jeżeli nie ma pozwolenia na używanie GPS
            startPosition =  new LatLng(50.0847, 19.9596);
            cameraZoom = 10.0f;
        } else {
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location != null){
                // Przesunięcie do obecnego miejsca użytkownika
                startPosition =  new LatLng(location.getLatitude(), location.getLongitude());
            } else {
                // Domyślne położenie mapy przy braku lokalizacji użytkownika (Kraków),
                // jeżeli nie udało się pobrać aktualnego położenia użytkownika
                startPosition =  new LatLng(50.0847, 19.9596);
                cameraZoom = 10.0f;
            }
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, cameraZoom));

        mMap.setOnCameraIdleListener(this);
    }

    @Override
    public void onCameraIdle() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()){
            Toast.makeText(getApplicationContext(), R.string.no_internet_toast, Toast.LENGTH_LONG).show();
            return;
        }

        drawTerrain();
    }

    /**
     * Metoda która odczytuje aktualne wymiary mapy ( szerokość i długość geograficzną jej krańców )
     * i wysyła odpowiednie zapytanie o dane na serwer.
     * Jeżeli mapa jest za bardzo oddalona to nie wysyłamy zapytania i nic nie rysujemy
     */
    private void drawTerrain(){
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        double north = bounds.northeast.latitude;
        double south = bounds.southwest.latitude;
        double west = bounds.northeast.longitude;
        double east = bounds.southwest.longitude;
        double zoom = mMap.getCameraPosition().zoom;
        int resolution;
        float offset;

        // Sprawdzanie maksymalnego oddalenia
        if (zoom < cameraMinZoom) {
            mMap.clear();
            return;
        }

        double nsOffset = (north - south) * cameraMargin;
        double weOffset = (west - east) * cameraMargin;

        north += nsOffset;
        south -= nsOffset;
        west += weOffset;
        east -= weOffset;

        // Ustawienie opcji odpowiednich dla obecnego oddalenia mapy
        if (zoom > cameraResTwoZoom) {
            resolution = 1;
            offset = resOneMapOffset;
        } else if (zoom > cameraResThreeZoom) {
            resolution = 2;
            offset = resTwoMapOffset;
        } else if (zoom > cameraResFourZoom) {
            resolution = 3;
            offset = resThreeMapOffset;
        } else if (zoom > cameraResFiveZoom) {
            resolution = 4;
            offset = resFourMapOffset;
        } else {
            resolution = 5;
            offset = resFiveMapOffset;
        }

        // Pobranie danych z serwera
        if (!getData(north, south, east, west, resolution)){
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

                polygonOptions.add(new LatLng(record.getDouble("latitude") + offset, record.getDouble("longitude") + offset),
                                   new LatLng(record.getDouble("latitude") + offset, record.getDouble("longitude") - offset),
                                   new LatLng(record.getDouble("latitude") - offset, record.getDouble("longitude") - offset),
                                   new LatLng(record.getDouble("latitude") - offset, record.getDouble("longitude") + offset));

                mMap.addPolygon(polygonOptions);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda pobierająca dane z serwera na podstawie aktualnego położenia i wymiarów mapy
     * @param north - północna krawędź mapy
     * @param south - południowa krawędź mapy
     * @param east - wschodnia krawędź mapy
     * @param west - zachodnia krawędź mapy
     * @param resolution - rozdzielczość z jaką pobierane są dane z mapy
     * @return - true jeżeli udało się pobrać nowe dane, false jeżeli nie
     */
    private boolean getData(double north, double south, double east, double west, int resolution){
        try {
            String terrainDataString = new NetworkGetRating().execute(
                    Double.toString(north),
                    Double.toString(south),
                    Double.toString(east),
                    Double.toString(west),
                    Integer.toString(resolution)
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
