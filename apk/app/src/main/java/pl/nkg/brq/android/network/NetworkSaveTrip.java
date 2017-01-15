package pl.nkg.brq.android.network;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
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

/**
 * Created by aaa on 2016-12-06.
 */

public class NetworkSaveTrip extends AsyncTask<Object, Void, String> {

    protected String doInBackground(Object... urls) {
        try {
            JSONObject jsonObjectMain = (JSONObject) urls[0];
            String userName = (String) urls[1];
            String name = (String) urls[2];
            String bikeType = (String) urls[3];
            String phonePlacement = (String) urls[4];
            String isPublic = (String) urls[5];

            URL url = new URL(ConstValues.BASE_URL + "/mydatabase/saveTrip/"
                    + userName + "/"
                    + name + "/"
                    + bikeType + "/"
                    + phonePlacement + "/"
                    + isPublic + "/");

            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setConnectTimeout(ConstValues.CONNECTION_TIMEOUT);
            urlConnection.connect();

            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(jsonObjectMain.toString());
            wr.flush();
            wr.close();

            InputStream in = urlConnection.getInputStream();
            String encoding = urlConnection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String myResponse = IOUtils.toString(in, encoding);

            return myResponse;

        } catch (java.net.SocketTimeoutException e) {
            return "false_timeout";
        } catch (Exception e) {
            Log.e("MyApp", e.getMessage());
            return "";
        }
    }
}