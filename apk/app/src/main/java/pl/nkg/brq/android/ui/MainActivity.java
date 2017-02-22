/*
 * Copyright (c) by Michał Niedźwiecki 2016
 * Contact: nkg753 on gmail or via GitHub profile: dzwiedziu-nkg
 *
 * This file is part of Bike Road Quality.
 *
 * Bike Road Quality is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bike Road Quality is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package pl.nkg.brq.android.ui;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.R;
import pl.nkg.brq.android.events.SensorsRecord;
import pl.nkg.brq.android.events.SensorsServiceState;
import pl.nkg.brq.android.maps.TerrainMapsActivity;
import pl.nkg.brq.android.maps.TripMapsActivity;
import pl.nkg.brq.android.maps.TripObject;
import pl.nkg.brq.android.network.NetworkGetRating;
import pl.nkg.brq.android.network.NetworkGetTrip;
import pl.nkg.brq.android.network.NetworkGetTripList;
import pl.nkg.brq.android.services.SensorsService;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_RESPONSE = 29;

    @Bind(R.id.button_on)
    Button mButtonOn;
    @Bind(R.id.trip_name_info)
    TextView mNameTextView;
    @Bind(R.id.speedTextView)
    TextView mSpeedTextView;
    @Bind(R.id.altitudeTextView)
    TextView mAltitudeTextView;
    @Bind(R.id.shakeTextView)
    TextView mShakeTextView;
    @Bind(R.id.noiseTextView)
    TextView mNoiseTextView;
    @Bind(R.id.distanceTextView)
    TextView mDistanceTextView;
    @Bind(R.id.warningTextView)
    TextView mWarningTextView;

    EditText nameEditText;
    TextView bikeTextView;
    TextView placementTextView;

    private boolean trackingToggle;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH},
                        MY_PERMISSION_RESPONSE);
            }
        }

        ButterKnife.bind(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        nameEditText = (EditText) findViewById(R.id.trip_name_edit);
        bikeTextView = (TextView) findViewById(R.id.bike_type_info);
        placementTextView = (TextView) findViewById(R.id.phone_placement_info);
        trackingToggle = false;
        updateDescription();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateDescription();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId){
            case R.id.action_selecet_trip:
                selectTripDialog(ConstValues.MODE_ALL_USERS);
                return true;

            case R.id.action_selecet_trip_user_only:
                selectTripDialog(ConstValues.MODE_USER_ONLY);
                return true;

            case R.id.action_selecet_terrain:
                startTerrainMap();
                return true;

            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            case R.id.action_quit:
                finish();
                return true;

            case R.id.action_logout:
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.pref_user_logged_key), "");
                editor.commit();

                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    //Opis jakiego rowera używamy itp...
    protected void updateDescription() {
        bikeTextView.setText(getString(R.string.bike_type_info) + sharedPreferences.getString(getString(R.string.pref_bike_key), ""));
        placementTextView.setText(getString(R.string.placement_info) + sharedPreferences.getString(getString(R.string.pref_placement_key), ""));
    }

    @OnClick(R.id.button_on)
    public void onButtonOnClick() {
        if(trackingToggle == false) {
            trackingToggle = true;
            mButtonOn.setText(getString(R.string.button_off_text));

            Intent sensorIntent = new Intent(MainActivity.this, SensorsService.class);
            sensorIntent.putExtra(getString(R.string.trip_name_key), nameEditText.getText().toString());

            startService(sensorIntent);

            mNameTextView.setVisibility(View.VISIBLE);
            mSpeedTextView.setVisibility(View.VISIBLE);
            mAltitudeTextView.setVisibility(View.VISIBLE);
            mShakeTextView.setVisibility(View.VISIBLE);
            mNoiseTextView.setVisibility(View.VISIBLE);
            mDistanceTextView.setVisibility(View.VISIBLE);

            nameEditText.setVisibility(View.GONE);
        } else {
            trackingToggle = false;
            mButtonOn.setText(getString(R.string.button_on_text));

            stopService(new Intent(MainActivity.this, SensorsService.class));

            mNameTextView.setVisibility(View.GONE);
            mSpeedTextView.setVisibility(View.GONE);
            mAltitudeTextView.setVisibility(View.GONE);
            mShakeTextView.setVisibility(View.GONE);
            mNoiseTextView.setVisibility(View.GONE);
            mDistanceTextView.setVisibility(View.GONE);

            nameEditText.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SensorsServiceState state) {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SensorsRecord record) {
        mNameTextView.setText(getString(R.string.name_text) + nameEditText.getText().toString());
        mSpeedTextView.setText(getString(R.string.speed_text) + (int) (record.speed * 3.6) + " km/h");
        mAltitudeTextView.setText(getString(R.string.altitude_text) + (int) record.altitude + " m n.p.m.");
        mShakeTextView.setText( getString(R.string.acceleration_text) +  (int) (record.shake * 100) / 100.0 + " m/s²");
        mNoiseTextView.setText( getString(R.string.noise_text) + (int) record.soundNoise + " db");
        mDistanceTextView.setText((double) record.distance / 100.0 + " m");

        if (record.distance < 120 && record.distance != 0) {
            mWarningTextView.setVisibility(View.VISIBLE);
            mWarningTextView.setText((double) record.distance / 100.0 + " m");
            mWarningTextView.setTextSize(Math.min(5000 / record.distance, 100));
        } else {
            mWarningTextView.setVisibility(View.GONE);
        }
    }

    public void testButton(View view){
        //test
        Log.d("APP", "TEST---------" + Boolean.toString(this.trackingToggle));
    }

    public void selectTripDialog(String mode){
        final Dialog tripSelectDialog = new Dialog(this);
        tripSelectDialog.setContentView(R.layout.select_trip_dialog);

        tripSelectDialog.setTitle(R.string.title_dialog_select);

        tripSelectDialog.show();

        String userName = sharedPreferences.getString(getString(R.string.pref_user_logged_key), "");
        ArrayList<TripObject> tripObjects = new ArrayList<>();

        try {
            String response =  new NetworkGetTripList().execute(userName, mode).get();
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray responseArray = jsonResponse.getJSONArray("array");

            for (int i = 0; i < responseArray.length(); i++) {
                JSONObject value = responseArray.getJSONObject(i);
                tripObjects.add(new TripObject(
                        value.getInt("id"),
                        value.getString("name")
                ));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final ListView tripListView = (ListView) tripSelectDialog.findViewById(R.id.trip_listview);

        ArrayAdapter<TripObject> adapter = new ArrayAdapter<TripObject>(
                this,
                android.R.layout.simple_list_item_single_choice,
                tripObjects
        );
        tripListView.setAdapter(adapter);
        tripListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        Button selectButton = (Button)tripSelectDialog.findViewById(R.id.button_select_trip);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tripListView.getCheckedItemPosition() != -1 ){
                    TripObject tripObject = (TripObject)tripListView.getAdapter().getItem(tripListView.getCheckedItemPosition());
                    JSONArray tripDataArray = new JSONArray();


                    try {
                        String tripResponseString = new NetworkGetTrip().execute(Integer.toString(tripObject.getId())).get();
                        JSONObject tripResponseJson = new JSONObject(tripResponseString);

                        String tripDataString = tripResponseJson.getString("trip_data");
                        tripDataArray = (new JSONObject(tripDataString)).getJSONArray("trip_data");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent mapIntent = new Intent(getApplicationContext(), TripMapsActivity.class);
                    mapIntent.putExtra(getString(R.string.trip_array_key), tripDataArray.toString());

                    startActivity(mapIntent);
                }

                tripSelectDialog.dismiss();
            }
        });
    }

    public void startTerrainMap(){
        JSONArray terrainDataArray = new JSONArray();

        try {
            String tripResponseString = new NetworkGetRating().execute().get();
            JSONObject tripResponseJson = new JSONObject(tripResponseString);

            terrainDataArray = tripResponseJson.getJSONArray("array");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent mapIntent = new Intent(getApplicationContext(), TerrainMapsActivity.class);
        mapIntent.putExtra(getString(R.string.trip_array_key), terrainDataArray.toString());

        startActivity(mapIntent);
    }
}
