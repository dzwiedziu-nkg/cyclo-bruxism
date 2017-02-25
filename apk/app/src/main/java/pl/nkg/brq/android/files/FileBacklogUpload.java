package pl.nkg.brq.android.files;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.R;

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

                Log.d("APP", file.toString());
                if(jsonObject != null){
                    if (sendFile(jsonObject)){
                        file.delete();
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

        String fileName = jsonObject.getString("name");
        jsonObject.remove("name");

        String bikeType = jsonObject.getString("bikeType");
        jsonObject.remove("bikeType");

        String phonePlacement = jsonObject.getString("phonePlacement");
        jsonObject.remove("phonePlacement");

        String isPublic = jsonObject.getString("isPublic");
        jsonObject.remove("isPublic");

        try {
            if (ConstValues.DATA_SENDING_ACTIVE == false){
                return false;
            }

            URL url = new URL(ConstValues.BASE_URL + "/mydatabase/saveTrip/"
                    + userName + "/"
                    + fileName + "/"
                    + bikeType + "/"
                    + phonePlacement + "/"
                    + isPublic + "/");

            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setConnectTimeout(ConstValues.CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(ConstValues.CONNECTION_TIMEOUT);
            urlConnection.connect();

            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(jsonObject.toString());
            wr.flush();
            wr.close();

            InputStream in = urlConnection.getInputStream();
            String encoding = urlConnection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String myResponse = IOUtils.toString(in, encoding);

            if (myResponse.equals("true")) {
                return true;
            }

        } catch (Exception e){
            Log.e("MyApp", e.getMessage());
        }

        return false;
    }
}
