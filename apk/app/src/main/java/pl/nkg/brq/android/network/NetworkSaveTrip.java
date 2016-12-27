package pl.nkg.brq.android.network;

import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by aaa on 2016-12-06.
 */

public class NetworkSaveTrip extends AsyncTask<Object, Void, String> {

    private Exception exception;

    protected String doInBackground(Object... urls) {
        try {
            File file = (File) urls[0];
            String userName = (String) urls[1];
            String name = (String) urls[2];
            String bikeType = (String) urls[3];
            String phonePlacement = (String) urls[4];
            String isPublic = (String) urls[5];

            String attachmentName = file.getName();
            String attachmentFileName = file.getName();
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";

            URL url = new URL("http://192.168.0.14:8000/mydatabase/saveTrip/"
                    + userName + "/"
                    + name + "/"
                    + bikeType + "/"
                    + phonePlacement + "/"
                    + isPublic);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setConnectTimeout(7000);
            urlConnection.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

            InputStream inputStream = urlConnection.getInputStream();
            String encoding = urlConnection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String myResponse = IOUtils.toString(inputStream, encoding);

            return myResponse;
        } catch (Exception e) {
            this.exception = e;
            return null;
        }
    }
}