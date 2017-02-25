package pl.nkg.brq.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import pl.nkg.brq.android.R;
import pl.nkg.brq.android.network.NetworkSaveTrip;

/**
 * Created by aaa on 2017-02-23.
 */

public class FileBacklogUploadService extends Service {

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<File> fileList = (ArrayList<File>)intent.getSerializableExtra(getString(R.string.file_list_key));

        InputStream inputStream = null;
        String jsonText = null;
        JSONObject jsonObject = null;

        for (File file : fileList){
            try {
                inputStream = new FileInputStream(file);
                jsonText = IOUtils.toString( inputStream );
                jsonObject =  new JSONObject(jsonText);

                if(jsonObject != null){
                    if (sendFile(jsonObject)){
                        file.delete();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        return super.onStartCommand(intent, flags, startId);
    }

    public boolean sendFile(JSONObject jsonObject) throws JSONException {
        String userName = jsonObject.getString("userName");
        jsonObject.remove("userName");

        String fileName = jsonObject.getString("name");
        jsonObject.remove("name");

        String bikeType = jsonObject.getString("bikeType");
        jsonObject.remove("bikeType");

        String phonePlacement = jsonObject.getString("phonePlacement");
        jsonObject.remove("phonePlacement");

        String isPublic = jsonObject.getString("isPublic");
        jsonObject.remove("isPublic");

        try {
            String response =  new NetworkSaveTrip().execute(jsonObject, userName, fileName, bikeType, phonePlacement, isPublic).get();

            if ( response.equals("true") ) {
                return true;
            }
        } catch (Exception e){
            Log.e("MyApp", e.getMessage());
        }

        return false;
    }

    public class LocalBinder extends Binder {
        public FileBacklogUploadService getService() {
            return FileBacklogUploadService.this;
        }
    }
}
