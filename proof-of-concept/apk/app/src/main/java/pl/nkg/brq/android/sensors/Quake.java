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

package pl.nkg.brq.android.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import pl.nkg.brq.android.Utils;

public class Quake extends ListeningSensor {

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    private float gravity[] = {0, 0, 0};
    private double linear_acceleration[] = {0, 0, 0};

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            final float alpha = 0.8f;

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];

            double mag = Utils.pitagoras(linear_acceleration);
            updateValueFromExternal(mag);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    public Quake(Context context) {
        senSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void start() throws SecurityException {
        senSensorManager.registerListener(mSensorEventListener, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        senSensorManager.unregisterListener(mSensorEventListener);
    }
}
