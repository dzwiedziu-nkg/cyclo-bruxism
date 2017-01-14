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

    private Exception exception;

    protected String doInBackground(Object... urls) {
        try {

            Log.d("MYAPP", "TEST----POCZATEK----");

            JSONObject jsonObjectMain = (JSONObject) urls[0];
            String userName = (String) urls[1];
            String name = (String) urls[2];
            String bikeType = (String) urls[3];
            String phonePlacement = (String) urls[4];
            String isPublic = (String) urls[5];

            Log.d("MYAPP", jsonObjectMain.toString());

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
            urlConnection.connect();

            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(jsonObjectMain.toString());
            wr.flush();
            wr.close();

            InputStream in = urlConnection.getInputStream();
            String encoding = urlConnection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String myResponse = IOUtils.toString(in, encoding);

            Log.d("MYAPP", myResponse);

            Log.d("MYAPP", "TEST----KONIEC----");
            Log.d("MYAPP", myResponse);

            /*
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Cache-Control", "no-cache");
            //urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.connect();

            DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
            InputStream inputStream;

            outputStream.writeBytes(file.toString());
            outputStream.flush();
            outputStream.close();

            if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                inputStream = urlConnection.getInputStream();
            } else {
                inputStream = urlConnection.getErrorStream();
            }

            String encoding = urlConnection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String myResponse = IOUtils.toString(inputStream, encoding);

            inputStream.close();

            */

            return myResponse;

        } catch (Exception e) {
            this.exception = e;
            return null;
        }
    }
}