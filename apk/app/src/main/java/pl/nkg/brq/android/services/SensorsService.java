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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.files.FileAccess;
import pl.nkg.brq.android.R;
import pl.nkg.brq.android.events.SensorsRecord;
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

    private static final String BLE_ADDRESS = "98:4F:EE:0F:90:DC";
    private static final String BLUETOOTH_ADDRESS = "98:D3:33:80:73:28";

    // Ilość czasu pomiędzy pomiarami, w milisekundach
    private static final int saveDuration = 500;
    // Dokładnośc z jaką mierzymy dane, w metrach
    private static final double GPS_ACCURACY = 50.0f;

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
    private String userName;
    private String bikeType;
    private String phonePlacement;
    private String isPublic;
    private String tripDate;

    private SharedPreferences preferences;
    private Intent myIntent;
    private FileAccess fileAccess;

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
        fileAccess = new FileAccess();

        mNoise.start();
        mQuake.start();
        mDistance.start();
        mLocation.start();
        mQueue = new LinkedBlockingQueue<>();

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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
                userName = preferences.getString(getString(R.string.pref_user_logged_key), "");
                bikeType = preferences.getString(getString(R.string.pref_bike_key), "");
                phonePlacement = preferences.getString(getString(R.string.pref_placement_key), "");
                isPublic = Boolean.toString(preferences.getBoolean(getString(R.string.pref_sharing_key), true));
                fileName = (String) myIntent.getExtras().get(getString(R.string.trip_name_key));
                tripDate = dateFormat.format(new Date());

                // Domyślna nazwa jeśli użytkownik żadnej nie podał:
                if( fileName.equals("")) {
                    fileName = "brq_" + tripDate;
                }

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
        Toast.makeText(SensorsService.this, R.string.service_start_toast, Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    /**
     * Dodanie pojedynczego pomiaru do kolejki, oraz sprawdzanie poprawności danych
     */
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

        // Zabezpiecznie przed zapisywaniem niepoprawnych danych dźwiękowych
        // Na niektórych modelach telefonów kilka pierwszych pomiarów głośności zwraca wartość nieskończoną
        if (Double.isInfinite(record.soundNoise)){
            return;
        }

        // Zabezpieczenie przed zapisywaniem danych zapisanych zanim złapał GPS
        // Opcja SAVE_EMPTY_DATA pozwala na zapisywanie takich danych w celu testowania aplikacji
        if (!ConstValues.SAVE_EMPTY_DATA && record.latitude == 0.0 && record.longitude == 0.0){
            return;
        }

        // Zabezpieczenie przed zapisywaniem niedokładnych danych z GPS.
        // Jeżeli błąd jest zbyt duży ( stała GPS_ACCURACY ), dane są odrzucone.
        if (record.accuracy > GPS_ACCURACY){
            return;
        }

        mQueue.add(record);
        EventBus.getDefault().post(record);
    }

    /**
     * Zapisanie danych do obiektu JSON i ich wysłanie na serwer, jeśli to możliwe
     */
    private void writeRecord() {
        jsonObjectMain = new JSONObject();
        JSONArray jsonArrayMain = new JSONArray();
        JSONObject jsonRecord;

        try {
            while (mQueue.size() > 0) {
                jsonRecord = new JSONObject();
                SensorsRecord record = mQueue.poll();

                jsonRecord.put("longitude", record.longitude);
                jsonRecord.put("latitude", record.latitude);

                jsonRecord.put("soundNoise", record.soundNoise);
                jsonRecord.put("shake", record.shake);

                // jsonRecord.put("timestamp", record.timestamp);
                // jsonRecord.put("altitude", record.altitude);
                // jsonRecord.put("accuracy", record.accuracy);
                //jsonRecord.put("speed", record.speed);
                //jsonRecord.put("distance", record.distance);

                jsonArrayMain.put(jsonRecord);
            }

            // Jeśli nie ma danych nic nie wysyłamy
            if (jsonArrayMain.length() == 0){
                makeToast(getString(R.string.nothing_to_send_toast));
                return;
            }

            jsonObjectMain.put("trip_data", jsonArrayMain);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Pobieranie informacji na temat dostępnego połączenia internetowego
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        String connectionType = preferences.getString(getString(R.string.pref_connection_key), "");

        // Wysyłanie danych przez wifi
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            sendFile();
        // Wysyłanie danych przez internet, jeżeli nie ma wifi a użytkownik zgodził się na używanie internetu
        } else if(activeNetworkInfo != null && activeNetworkInfo.isConnected() && connectionType.equals("internet")){
            sendFile();
        // Lokalne zapisanie danych
        } else {
            fileAccess.saveJSONFile(jsonObjectMain , fileName, userName, bikeType, phonePlacement, isPublic, tripDate);
            makeToast(getString(R.string.file_saved_locally_toast));
        }
    }

    /**
     * Metoda obsługująca wysyłanie danych na serwer oraz pokazywanie informacji o rezultacie użytkownikowi
     */
    public void sendFile() {
        String userName = preferences.getString(getString(R.string.pref_user_logged_key), "");
        String bikeType = preferences.getString(getString(R.string.pref_bike_key), "");
        String phonePlacement = preferences.getString(getString(R.string.pref_placement_key), "");
        String isPublic = Boolean.toString(preferences.getBoolean(getString(R.string.pref_sharing_key), true));

        try {
            String response =  new NetworkSaveTrip().execute(this.jsonObjectMain, userName, fileName, bikeType, phonePlacement, isPublic, tripDate).get();

            // Sprawdzamy czy wysyłanie się powiodło
            if ( response.equals("true") ) {
                makeToast(getString(R.string.upload_success_toast));
                return;
            } else if (response.equals("false_invalid_form")){
                Log.d("MyApp", "Upload failed");
            } else if (response.equals("false_post")){
                Log.d("MyApp", "Post failed");
            } else if (response.equals("false_timeout")){
                makeToast(getString(R.string.timeout_exception_toast));
            }

            // Pokazanie wiadomości o niepowodzeniu przesłania
            makeToast(getString(R.string.file_saved_locally_toast));
        } catch (Exception e){
            Log.e("MyApp", e.getMessage());
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
    }

    /**
     * Metoda ułatwiająca pokazywanie toast
     * @param text - tekst do wyświetlenia
     */
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
