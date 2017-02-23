package pl.nkg.brq.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

import pl.nkg.brq.android.R;

/**
 * Created by aaa on 2017-02-23.
 */

public class FileBacklogUploadService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("APP", "START");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("APP", "TUTAJ");
        File[] fileList = (File[]) intent.getExtras().get(getString(R.string.file_list_key));


        for (int i = 0; i < fileList.length; i++){
            Log.d("Files", "FileName:" + fileList[i].getName());
            Log.d("Files", "File:" + fileList[i].toString());
            Log.d("Files", "Size:" + fileList[i].length());
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
