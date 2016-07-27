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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.nkg.brq.android.R;
import pl.nkg.brq.android.events.SensorsRecord;
import pl.nkg.brq.android.events.SensorsServiceState;
import pl.nkg.brq.android.services.SensorsService;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_RESPONSE = 29;

    @Bind(R.id.button_on)
    Button mButtonOn;
    @Bind(R.id.button_off)
    Button mButtonOff;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    @OnClick(R.id.button_on)
    public void onButtonOnClick() {
        startService(new Intent(MainActivity.this, SensorsService.class));
    }

    @OnClick(R.id.button_off)
    public void onButtonOffClick() {
        stopService(new Intent(MainActivity.this, SensorsService.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SensorsServiceState state) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SensorsRecord record) {
        mSpeedTextView.setText((int) (record.speed * 3.6) + " km/h");
        mAltitudeTextView.setText((int) record.altitude + " m n.p.m.");
        mShakeTextView.setText((int) (record.shake * 100) / 100.0 + " m/s²");
        mNoiseTextView.setText((int) record.soundNoise + " db");
        mDistanceTextView.setText((double) record.distance / 100.0 + " m");

        if (record.distance < 120 && record.distance != 0) {
            mWarningTextView.setVisibility(View.VISIBLE);
            mWarningTextView.setText((double) record.distance / 100.0 + " m");
            mWarningTextView.setTextSize(Math.min(5000 / record.distance, 100));
        } else {
            mWarningTextView.setVisibility(View.GONE);
        }
    }
}
