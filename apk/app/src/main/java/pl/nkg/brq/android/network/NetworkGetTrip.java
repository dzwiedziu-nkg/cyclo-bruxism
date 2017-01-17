package pl.nkg.brq.android.network;

import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import pl.nkg.brq.android.ConstValues;

/**
 * Created by aaa on 2016-12-06.
 */

public class NetworkGetTrip extends AsyncTask<String, Void, String> {

    private Exception exception;

    protected String doInBackground(String... urls) {
        try {
            String id = urls[0];

            URL url = new URL(ConstValues.BASE_URL + "/mydatabase/getTrip/"
                    + id);

            URLConnection urlConnection =  url.openConnection();

            InputStream in = urlConnection.getInputStream();
            String encoding = urlConnection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String myResponse = IOUtils.toString(in, encoding);

            return myResponse;
        } catch (Exception e) {
            this.exception = e;
            return null;
        }
    }
}