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

package pl.nkg.brq.android.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import pl.nkg.brq.android.Utils;
import pl.nkg.brq.android.sensors.Noise;

public class SensorsService extends Service implements SensorEventListener, LocationListener {

    private static final String TAG = SensorsService.class.getSimpleName();

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private final IBinder mBinder = new LocalBinder();
    private Noise mNoise;
    private LocationManager mLocationManager;

    private LinkedBlockingQueue<Record> mQueue;
    private boolean mFinish;
    public static int distance;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    public static  int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

    private String address = "98:4F:EE:0F:90:DC";
    private UUID service = UUID.fromString("181C");
    private UUID characteristic = UUID.fromString("2A19");

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic chr = mBluetoothGatt.getService(service).getCharacteristic(characteristic);
                mBluetoothGatt.setCharacteristicNotification(chr, true);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

/*        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logData("onCharacteristicRead", -1, characteristic.getValue(), false, 0, characteristic.getUuid());
                mListener.onCommunicationEvent(CommunicationEventType.characteristicRead, characteristic.getValue(), characteristic.getService().getUuid(), characteristic.getUuid(), null);
            }
        }*/

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            distance = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            //logData("onCharacteristicChanged", -1, characteristic.getValue(), false, 0, characteristic.getUuid());
            //mListener.onCommunicationEvent(CommunicationEventType.characteristicChanged, characteristic.getValue(), characteristic.getService().getUuid(), characteristic.getUuid(), null);
        }
/*
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logData("onCharacteristicWrite", 1, characteristic.getValue(), false, 0, characteristic.getUuid());
                mListener.onCommunicationEvent(CommunicationEventType.characteristicWrite, characteristic.getValue(), characteristic.getService().getUuid(), characteristic.getUuid(), null);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logData("onDescriptorWrite", 1, descriptor.getValue(), false, 0, descriptor.getUuid());
                mListener.onCommunicationEvent(CommunicationEventType.descriptorWrite, descriptor.getValue(), descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(), descriptor.getUuid());
            }
        }*/

    };

    public boolean connect() {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = BluetoothGatt.STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = BluetoothGatt.STATE_CONNECTING;
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mQueue = new LinkedBlockingQueue<>();

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mNoise = new Noise();

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            Log.e(TAG, "GPS permission not granted", e);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connect();
        mNoise.startRecorder();
        mFinish = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mFinish) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mQueue.size() == 0) {
                        continue;
                    }

                    File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/brq.csv");

                    try {
                        FileOutputStream fOut = new FileOutputStream(file, true);
                        OutputStreamWriter osw = new OutputStreamWriter(fOut);
                        int count = mQueue.size();
                        while (mQueue.size() > 0) {
                            Record record = mQueue.poll();
                            String rec = record.timestamp + "\t" +
                                    record.lon + "\t" +
                                    record.lat + "\t" +
                                    record.alt + "\t" +
                                    record.acc + "\t" +
                                    record.speed + "\t" +
                                    record.db + "\t" +
                                    record.mag + "\t" +
                                    record.distance;
                            osw.write(rec + "\n");
                            Log.d(TAG, rec);
                        }
                        osw.flush();
                        osw.close();
                        //SensorsService.this.getApplicationContext().
                        //Toast.makeText(SensorsService.this, "Flushed: " + count, Toast.LENGTH_SHORT).show();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        Toast.makeText(SensorsService.this, "Service start", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        senSensorManager.unregisterListener(this);
        mNoise.stopRecorder();
        mFinish = true;
        Toast.makeText(SensorsService.this, "Service destroyed", Toast.LENGTH_SHORT).show();
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    float gravity[] = {0, 0, 0};
    double linear_acceleration[] = {0, 0, 0};

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

        Record record = new Record();
        //String loc = "";
        if (mLocation != null) {
            //loc = mLocation.getLatitude() + " lat; " + mLocation.getLongitude() + " lon; " + mLocation.getAltitude() + " alt; " + mLocation.getAccuracy() + " acc; " + mLocation.getSpeed() + "m/s";
            record.acc = mLocation.getAccuracy();
            record.lon = mLocation.getLongitude();
            record.lat = mLocation.getLatitude();
            record.alt = mLocation.getAltitude();
            record.speed = mLocation.getSpeed();
        }

        //Log.d(TAG, mag + " m/s²; " + mNoise.getNoise() + "dB " + loc);
        record.mag = mag;
        record.db = mNoise.getNoise();
        record.distance = distance;

        if (mQueue != null) {
            mQueue.add(record);
        }

        if (mConnectionState != BluetoothGatt.STATE_CONNECTED || mConnectionState != BluetoothGatt.STATE_CONNECTING) {
            connect();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private Location mLocation;

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public class LocalBinder extends Binder {
        public SensorsService getService() {
            return SensorsService.this;
        }
    }

    private class Record {
        public double mag;
        public double db;
        public double lon;
        public double lat;
        public double alt;
        public double acc;
        public double speed;
        public long timestamp;
        public double distance;

        public Record() {
            timestamp = System.currentTimeMillis();
        }
    }
}
