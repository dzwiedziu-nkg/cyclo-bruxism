package pl.nkg.brq.android.files;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

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

public class FileBacklogUpload extends AsyncTask<ArrayList<File>, Void, Void> {

    boolean fileSent = false;
    Context mContext;

    public FileBacklogUpload(Context context){
        this.mContext = context;
    }

    @Override
    protected Void doInBackground(ArrayList<File>... arrayLists) {
        ArrayList<File> fileList = arrayLists[0];

        InputStream inputStream = null;
        String jsonText = null;
        JSONObject jsonObject = null;

        for (File file : fileList){
            try {
                inputStream = new FileInputStream(file);
                jsonText = IOUtils.toString( inputStream );
                jsonObject =  new JSONObject(jsonText);

                if(jsonObject != null){
                    file.delete();
                    if (sendFile(jsonObject)){
                        fileSent = true;
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

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (fileSent) {
            Toast.makeText(mContext, R.string.backlog_file_sent_toast,  Toast.LENGTH_SHORT).show();
        }
    }

    public boolean sendFile(JSONObject jsonObject) throws JSONException {
        String userName = jsonObject.getString("userName");
        jsonObject.remove("userName");

        String fileName = jsonObject.getString("fileName");
        jsonObject.remove("fileName");

        String bikeType = jsonObject.getString("bikeType");
        jsonObject.remove("bikeType");

        String phonePlacement = jsonObject.getString("phonePlacement");
        jsonObject.remove("phonePlacement");

        String isPublic = jsonObject.getString("isPublic");
        jsonObject.remove("isPublic");

        String tripDate = jsonObject.getString("tripDate");
        jsonObject.remove("tripDate");

        String response = new NetworkSaveTrip().uploadTrip(jsonObject, userName, fileName, bikeType, phonePlacement, isPublic, tripDate);

        if (response.equals("true")) {
            return true;
        }

        return false;
    }
}
