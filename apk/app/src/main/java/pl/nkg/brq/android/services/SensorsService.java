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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.R;
import pl.nkg.brq.android.events.SensorsRecord;
import pl.nkg.brq.android.network.NetworkAccessLogin;
import pl.nkg.brq.android.network.NetworkSaveTrip;
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
    private Handler handler;

    private Noise mNoise;
    private Quake mQuake;
    private Distance mDistance;
    private Location mLocation;

    private JSONObject jsonObjectMain;
    private String fileName;
    Intent myIntent;

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
        handler = new Handler();
        myIntent = intent;

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
                //File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/brq_" + dateFormat.format(new Date()) + ".csv");
                fileName = "brq_" + dateFormat.format(new Date());

                while (!isFinish()) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mQueue.size() == 0 || !isFinish()) {
                        continue;
                    }

                    writeRecord();
                }
            }
        });
        mWriteThread.start();
        Toast.makeText(SensorsService.this, R.string.service_start, Toast.LENGTH_SHORT).show();
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
        record.soundNoise = mNoise.getValue().getValue();
        record.shake = mQuake.getValue().getMaxValue();

        //zabezpiecznie przed zapisywaniem niepoprawnych danych dźwiękowych
        if (Double.isInfinite(record.soundNoise)){
            return;
        }

        mQueue.add(record);
        EventBus.getDefault().post(record);
    }

    private void writeRecord() {
        jsonObjectMain = new JSONObject();
        JSONArray jsonArrayMain = new JSONArray();
        JSONObject jsonRecord;

        try {
            while (mQueue.size() > 0) {
                SensorsRecord record = mQueue.poll();

                //zabezpieczenie przed zapisywaniem danych zanim złapie GPS
                if (!ConstValues.SAVE_EMPTY_DATA && record.latitude == 0.0 && record.longitude == 0.0 ){
                    continue;
                }

                jsonRecord = new JSONObject();

                jsonRecord.put("timestamp", record.timestamp);
                jsonRecord.put("longitude", record.longitude);
                jsonRecord.put("latitude", record.latitude);

/*
                jsonRecord.put("altitude", record.altitude);
                jsonRecord.put("accuracy", record.accuracy);
                jsonRecord.put("speed", record.speed);
                jsonRecord.put("soundNoise", record.soundNoise);
                jsonRecord.put("shake", record.shake);
                jsonRecord.put("distance", record.distance);
*/

                jsonRecord.put("rating", getRating(record.soundNoise, record.shake));

                jsonArrayMain.put(jsonRecord);
            }

            jsonObjectMain.put("trip_data", jsonArrayMain);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendFile();
    }

    //zwraca ocenę z zakresu 1-10. 1 to najlepsza, 10 najgorsza
    public double getRating(double soundNoise, double shake){
        int count = 0;
        double noiseGrade = 0.0;
        double shakeGrade = 0.0;

        //wystawienie oceny na podstawie dźwięku
        if(! Double.isNaN(soundNoise)) {
            count++;
            noiseGrade = (soundNoise - 20.0) / 10;

            //normalizacja danych dźwiękowych do oceny z zakresu 1-10
            noiseGrade = Math.min(noiseGrade, 10.0);
            noiseGrade = Math.max(noiseGrade, 1.0);
        }

        //wystawienie oceny na podstawie wstrząsów
        if(! Double.isNaN(shake)) {
            count++;
            shakeGrade = shake * 3 / 10;

            //normalizacja danych do oceny z zakresu 1-10
            shakeGrade = Math.min(shakeGrade, 10.0);
            shakeGrade = Math.max(shakeGrade, 1.0);
        }

        double grade;
        if( count > 0 ) {
            grade = (noiseGrade + shakeGrade) / count;
        } else {
            grade = 0.0;
        }

        grade = (double) Math.round(grade * 10) / 10;

        return grade;
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
    }

    public void sendFile() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String userName = preferences.getString(getString(R.string.pref_user_logged_key), "");
        String name = (String) myIntent.getExtras().get(getString(R.string.trip_name_key));
        String bikeType = preferences.getString(getString(R.string.pref_bike_key), "");
        String phonePlacement = preferences.getString(getString(R.string.pref_placement_key), "");
        String isPublic = Boolean.toString(preferences.getBoolean(getString(R.string.pref_sharing_key), true));

        //zapasowa nazwa jeśli żadnej nie podano:
        if (name.equals("")) {
            name = fileName;
        }


        try {
            String response =  new NetworkSaveTrip().execute(jsonObjectMain, userName, name, bikeType, phonePlacement, isPublic).get();

            if ( response.equals("true") ) {
                makeToast(getString(R.string.upload_success_toast));
            } else if (response.equals("false_invalid_form")){
                Log.d("MyApp", "Upload failed");
            } else if (response.equals("false_post")){
                Log.d("MyApp", "Post failed");
            } else if (response.equals("false_timeout")){
                makeToast(getString(R.string.timeout_exception_toast));
            }
        } catch (Exception e){
            Log.e("MyApp", e.getMessage());
        }
    }

    private void makeToast(final String text){
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public class LocalBinder extends Binder {
        public SensorsService getService() {
            return SensorsService.this;
        }
    }
}
