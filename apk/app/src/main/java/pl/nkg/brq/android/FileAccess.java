package pl.nkg.brq.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import pl.nkg.brq.android.services.FileBacklogUploadService;
import pl.nkg.brq.android.ui.MainActivity;

/**
 * Created by aaa on 2017-02-23.
 */

public class FileAccess {

    private void createDirectory(){
        File folder = new File(Environment.getExternalStorageDirectory() + "/" + ConstValues.DIRECTORY_NAME);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    // Zapisanie pliku z danymi lokalnie
    public void saveJSONFile(JSONObject jsonObject, String fileName, String userName, String bikeType, String phonePlacement, String isPublic){
        this.createDirectory();

        File file = new File(Environment.getExternalStorageDirectory()
                + "/" + ConstValues.DIRECTORY_NAME
                + "/" + fileName + ".json");

        try {
            jsonObject.put("name", fileName);
            jsonObject.put("userName", userName);
            jsonObject.put("bikeType", bikeType);
            jsonObject.put("phonePlacement", phonePlacement);
            jsonObject.put("isPublic", isPublic);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            FileOutputStream fOut = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(jsonObject.toString());

            osw.flush();
            osw.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Zwraca listę zapisanych plików
    public File[] getAllSavedData(){
        this.createDirectory();

        File folder = new File(Environment.getExternalStorageDirectory() + "/" + ConstValues.DIRECTORY_NAME);
        File[] fileList = folder.listFiles();

        return fileList;
    }
}
