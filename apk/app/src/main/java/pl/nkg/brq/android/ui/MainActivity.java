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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.preference.PreferenceManager;

import java.util.concurrent.ExecutionException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.R;
import pl.nkg.brq.android.events.SensorsRecord;
import pl.nkg.brq.android.events.SensorsServiceState;
import pl.nkg.brq.android.network.NetworkGetTripList;
import pl.nkg.brq.android.services.SensorsService;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_RESPONSE = 29;

    @Bind(R.id.button_on)
    Button mButtonOn;
    @Bind(R.id.button_off)
    Button mButtonOff;
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
                Log.w("BleActivity", "Location access not granted!");
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
        updateDescription();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateDescription();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        if (itemId == R.id.action_quit) {
            finish();
            return true;
        }

        if (itemId == R.id.action_logout) {
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
        Intent sensorIntent = new Intent(MainActivity.this, SensorsService.class);
        sensorIntent.putExtra(getString(R.string.trip_name_key), nameEditText.getText().toString());

        startService(sensorIntent);

        //TODO zapisanie ustawień rowera itp

        mNameTextView.setVisibility(View.VISIBLE);
        mSpeedTextView.setVisibility(View.VISIBLE);
        mAltitudeTextView.setVisibility(View.VISIBLE);
        mShakeTextView.setVisibility(View.VISIBLE);
        mNoiseTextView.setVisibility(View.VISIBLE);
        mDistanceTextView.setVisibility(View.VISIBLE);

        nameEditText.setVisibility(View.GONE);

        mButtonOn.setVisibility(View.GONE);
        mButtonOff.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.button_off)
    public void onButtonOffClick() {
        stopService(new Intent(MainActivity.this, SensorsService.class));

        mNameTextView.setVisibility(View.GONE);
        mSpeedTextView.setVisibility(View.GONE);
        mAltitudeTextView.setVisibility(View.GONE);
        mShakeTextView.setVisibility(View.GONE);
        mNoiseTextView.setVisibility(View.GONE);
        mDistanceTextView.setVisibility(View.GONE);

        nameEditText.setVisibility(View.VISIBLE);

        mButtonOn.setVisibility(View.VISIBLE);
        mButtonOff.setVisibility(View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SensorsServiceState state) {

    }

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
        Log.d("APP", "TEST---------");
        try {
            String response =  new NetworkGetTripList().execute("user", ConstValues.MODE_ALL_USERS).get();
            Log.d("APP", response);
            JSONObject obj = new JSONObject(response);
            Log.d("APP", obj.toString());
            JSONArray arrayJ = obj.getJSONArray("array");
            Log.d("APP", arrayJ.toString());
            Log.d("APP", arrayJ.get(0).toString());
            Log.d("APP", arrayJ.get(1).toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
