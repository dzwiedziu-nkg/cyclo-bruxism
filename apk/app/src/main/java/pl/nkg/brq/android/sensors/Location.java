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
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import pl.nkg.brq.android.services.SensorsService;

import static android.content.Context.LOCATION_SERVICE;

public class Location extends AbstractSensor {

    private static final String TAG = Location.class.getSimpleName();

    private LocationManager mLocationManager;

    private final Value mLongitude = new Value();
    private final Value mLatitude = new Value();
    private final Value mAltitude = new Value();
    private final Value mAccuracy = new Value();
    private final Value mSpeed = new Value();
    //private final Value mValues[] = {mLongitude, mLatitude, mAltitude, mAccuracy, mSpeed};


    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(android.location.Location location) {
            Log.d("MyApp", "ISRunning: " + String.valueOf(isRunning()));
            if (isRunning()) {
                updateValues(location);
                emitNewValueAvailableListener();
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.d("MyApp", "onStatusChanged?");

        }

        @Override
        public void onProviderEnabled(String s) {
            Log.d("MyApp", "onProviderEnabled?");

        }

        @Override
        public void onProviderDisabled(String s) {
            Log.d("MyApp", "onProviderDisabled?");

        }
    };


    private synchronized void updateValues(android.location.Location location) {
        mLongitude.setValue(location.getLongitude());
        mLatitude.setValue(location.getLatitude());
        mAltitude.setValue(location.getAltitude());
        mAccuracy.setValue(location.getAccuracy());
        mSpeed.setValue(location.getSpeed());
    }

    public Location(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void start() throws SecurityException {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        try {
            mLocationManager.removeUpdates(mLocationListener);
        } catch (SecurityException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    public Value getAccuracy() {
        mAccuracy.setNextClean(true);
        return mAccuracy;
    }

    public Value getAltitude() {
        mAltitude.setNextClean(true);
        return mAltitude;
    }

    public Value getLatitude() {
        mLatitude.setNextClean(true);
        return mLatitude;
    }

    public Value getLongitude() {
        mLongitude.setNextClean(true);
        return mLongitude;
    }

    public Value getSpeed() {
        mSpeed.setNextClean(true);
        return mSpeed;
    }

    public Value peekAccuracy() {
        return mAccuracy;
    }

    public Value peekAltitude() {
        return mAltitude;
    }

    public Value peekLatitude() {
        return mLatitude;
    }

    public Value peekLongitude() {
        return mLongitude;
    }

    public Value peekSpeed() {
        return mSpeed;
    }
}
