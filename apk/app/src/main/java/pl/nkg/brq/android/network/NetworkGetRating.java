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

public class NetworkGetRating extends AsyncTask<String, Void, String> {

    private Exception exception;

    protected String doInBackground(String... urls) {
        try {
            URL url = new URL(ConstValues.BASE_URL + "/mydatabase/getRating/");

            URLConnection urlConnection =  url.openConnection();
            urlConnection.setConnectTimeout(ConstValues.CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(ConstValues.CONNECTION_TIMEOUT);

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