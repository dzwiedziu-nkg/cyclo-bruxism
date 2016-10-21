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

import org.greenrobot.eventbus.EventBus;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import pl.nkg.brq.android.events.SensorsRecord;
import pl.nkg.brq.android.sensors.Distance;
import pl.nkg.brq.android.sensors.DistanceBluetooth;
import pl.nkg.brq.android.sensors.Location;
import pl.nkg.brq.android.sensors.Noise;
import pl.nkg.brq.android.sensors.Quake;
import pl.nkg.brq.android.sensors.Value;

public class SensorsService extends Service {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
    private static final String TAG = SensorsService.class.getSimpleName();

    // TODO: konfigurowalne
    private static final String BLE_ADDRESS = "98:4F:EE:0F:90:DC";
    private static final String BLUETOOTH_ADDRESS = "98:D3:33:80:73:28";
    private static final int saveDuration = 200;

    private final IBinder mBinder = new LocalBinder();

    private LinkedBlockingQueue<SensorsRecord> mQueue;
    private boolean mFinish;

    private Timer mTimer = new Timer();
    private Thread mWriteThread;

    private Noise mNoise;
    private Quake mQuake;
    private Distance mDistance;
    private Location mLocation;

    private synchronized boolean isFinish() {
        return mFinish;
    }

    private synchronized void setFinish(boolean finish) {
        mFinish = finish;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNoise = new Noise();
        mQuake = new Quake(this);
        mDistance = new DistanceBluetooth(BLUETOOTH_ADDRESS);
        mLocation = new Location(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mNoise.start();
        mQuake.start();
        mDistance.start();
        mLocation.start();
        mQueue = new LinkedBlockingQueue<>();

        setFinish(false);

        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isFinish()) {
                    putRecordInQueue();
                }
            }
        }, saveDuration, saveDuration);

        mWriteThread = new Thread(new Runnable() {


            @Override
            public void run() {
                File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/brq_" + dateFormat.format(new Date()) + ".csv");

                while (!isFinish()) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mQueue.size() == 0 || !isFinish()) {
                        continue;
                    }

                    writeRecord(file);
                }
            }
        });
        mWriteThread.start();
        Toast.makeText(SensorsService.this, "Service start", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    private void putRecordInQueue() {
        SensorsRecord record = new SensorsRecord();
        final Location location = mLocation;
        synchronized (location) {
            record.latitude = location.getLatitude().getValue();
            record.longitude = location.getLongitude().getValue();
            record.altitude = location.getAltitude().getValue();
            record.accuracy = location.getAccuracy().getValue();
            record.speed = location.getSpeed().getValue();
        }

        Value distance = mDistance.getValue();
        record.distance  = (int)(distance.getTimestampOfUpdated() + 2 * saveDuration < System.currentTimeMillis() ? distance.getValue() : distance.getMinValue());
        //record.distance = (int) mDistance.getValue().getMinValue();
        record.soundNoise = mNoise.getValue().getValue();
        record.shake = mQuake.getValue().getMaxValue();
        mQueue.add(record);
        EventBus.getDefault().post(record);
    }

    private void writeRecord(File file) {
        try {
            FileOutputStream fOut = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            int count = mQueue.size();
            while (mQueue.size() > 0) {
                SensorsRecord record = mQueue.poll();
                String rec = record.timestamp + "\t" +
                        record.longitude + "\t" +
                        record.latitude + "\t" +
                        record.altitude + "\t" +
                        record.accuracy + "\t" +
                        record.speed + "\t" +
                        record.soundNoise + "\t" +
                        record.shake + "\t" +
                        record.distance;
                osw.write(rec + "\n");
                Log.d(TAG, rec);
            }
            osw.flush();
            osw.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setFinish(true);
        mNoise.stop();
        mQuake.stop();
        mDistance.stop();
        mLocation.stop();
        mTimer.cancel();
        mWriteThread.interrupt();
        Toast.makeText(SensorsService.this, "Service destroyed", Toast.LENGTH_SHORT).show();
    }

    public class LocalBinder extends Binder {
        public SensorsService getService() {
            return SensorsService.this;
        }
    }
}
