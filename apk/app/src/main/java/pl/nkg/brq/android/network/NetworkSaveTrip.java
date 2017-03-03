package pl.nkg.brq.android.network;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import pl.nkg.brq.android.ConstValues;
import pl.nkg.brq.android.files.FileAccess;

/**
 * Created by aaa on 2016-12-06.
 */

public class NetworkSaveTrip extends AsyncTask<Object, Void, String> {

    protected String doInBackground(Object... urls) {
        if (!ConstValues.DATA_SENDING_ACTIVE) {
            return "true";
        }

        JSONObject jsonObjectMain = (JSONObject) urls[0];
        String userName = (String) urls[1];
        String fileName = (String) urls[2];
        String bikeType = (String) urls[3];
        String phonePlacement = (String) urls[4];
        String isPublic = (String) urls[5];
        String tripDate = (String) urls[6];
        String response = "false";

        try {
            JSONArray jsonArray = jsonObjectMain.getJSONArray("trip_data");
            JSONArray jsonArrayToSent;
            JSONObject jsonObjectToSent;
            JSONArray jsonArrayRest;

            while (jsonArray.length() > 0){
                jsonArrayToSent = new JSONArray();
                jsonArrayRest = new JSONArray();

                if (jsonArray.length() < ConstValues.DATA_CHUNK_SIZE) {
                    jsonArrayToSent = jsonArray;
                } else {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        if (i < ConstValues.DATA_CHUNK_SIZE){
                            jsonArrayToSent.put(jsonArray.get(i));
                        } else {
                            jsonArrayRest.put(jsonArray.get(i));
                        }
                    }
                }

                jsonObjectToSent = new JSONObject().put("trip_data", jsonArrayToSent);
                response = uploadTrip(jsonObjectToSent, userName, fileName, bikeType, phonePlacement, isPublic, tripDate);

                //zapisanie pliku na dysku w przypadku błędu przesyłania
                if (!response.equals("true")) {
                    JSONObject jsonObjectToSave = new JSONObject().put("trip_data", jsonArray);

                    FileAccess fileAccess = new FileAccess();
                    fileAccess.saveJSONFile(jsonObjectToSave, fileName, userName, bikeType, phonePlacement, isPublic, tripDate);
                    return response;
                }

                jsonArray = jsonArrayRest;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return response;
    }

    public String uploadTrip(JSONObject jsonObject,
                                String userName,
                                String fileName,
                                String bikeType,
                                String phonePlacement,
                                String isPublic,
                                String tripDate) {

        try {
            URL url = new URL(ConstValues.BASE_URL + "/mydatabase/saveTrip/"
                    + userName + "/"
                    + fileName + "/"
                    + bikeType + "/"
                    + phonePlacement + "/"
                    + isPublic + "/"
                    + tripDate + "/");

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

            urlConnection.disconnect();
            return myResponse;

        } catch (java.net.SocketTimeoutException e) {
            return "false_timeout";
        } catch (Exception e) {
            Log.e("MyApp", e.getMessage());
            return "";
        }
    }
}