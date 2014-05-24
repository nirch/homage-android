/** _________
   /   _____/ ______________  __ ___________
   \_____  \_/ __ \_  __ \  \/ // __ \_  __ \
   /        \  ___/|  | \/\   /\  ___/|  | \/
  /_______  /\___  >__|    \_/  \___  >__|
          \/     \/                 \/

    Abstract class for supplying basic methods for networking with a simple HTTP webservice.
    Important: Derived class should be implemented as a singleton.

    You must provide the configuration in resource files.
    Needed values in the resource file:

    <!-- Flag for indicating if to use the production or development settings -->
    <bool name="is_production_server">false</bool>

    <!-- DEV Server -->
    <string name="server_dev_host">homage-server-app-dev.elasticbeanstalk.com</string>
    <integer name="server_dev_port">80</integer>
    <string name="server_dev_protocol">http</string>

    <!-- Production Server -->
    <string name="server_prod_host">homage-server-app-prod.elasticbeanstalk.com</string>
    <integer name="server_prod_port">80</integer>
    <string name="server_prod_protocol">http</string>
*/

package com.homage.networking.server;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.homage.app.R;
import com.homage.networking.parsers.Parser;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import android.support.v4.content.LocalBroadcastManager;


abstract public class Server {
    String TAG = "TAG_"+getClass().getName();

    public static String SR_SUCCESS = "success";


    private boolean isProductionServer;
    private String host;
    private int port;
    private String protocol;
    private boolean alreadyInitialized;
    private DefaultHttpClient client;
    private HashMap<Integer, String> namedURLS;
    private Context context;

    public void init(Context context) {
        this.context = context;

        // Can be initialized only once!
        if (alreadyInitialized)
            throw new AssertionError("Tried to initialize Server more than once.");

        Resources res = context.getResources();
        isProductionServer = res.getBoolean(R.bool.is_production_server);
        if (isProductionServer) {
            host = res.getString(R.string.server_prod_host);
            port = res.getInteger(R.integer.server_prod_port);
            protocol = res.getString(R.string.server_prod_protocol);
        } else {
            host = res.getString(R.string.server_dev_host);
            port = res.getInteger(R.integer.server_dev_port);
            protocol = res.getString(R.string.server_dev_protocol);
        }

        // initialize a client
        client = new DefaultHttpClient();
    }
    //endregion

    public void initURLSCache(ArrayList<Integer> urlIDs) {
        Resources res = context.getResources();
        namedURLS = new HashMap<Integer, String>();
        for (Integer urlID : urlIDs) {
            String url = res.getString(urlID);
            namedURLS.put(urlID, url);
        }
    }

    public String url(int urlID) {
        String relativeURL = namedURLS.get(urlID);
        String url;
        if (port == 80) {
            url = String.format("%s://%s/%s", protocol, host, relativeURL);
        } else {
            url = String.format("%s://%s:%d/%s", protocol, host, port, relativeURL);
        }
        Log.v(TAG, String.format("Prepared url:%s", url));
        return url;
    }

    /**
     * Do a GET async HTTP request to the server.
     *
     * @param urlID the ID of the url (as defined in server_cfg.xml) (required!)
     * @param parameters a hashmap of parameters for the get request. (optional)
     * @param intentName the intent name the will be broadcasted locally, when finished (optional)
     * @param info some info that will be attached to the request/response (optional)
     * @param parser the parser that will handle the response (optional)
     */
    public void GET(
            int urlID,
            HashMap<String,String> parameters,
            String intentName,
            HashMap<String,Object> info,
            Parser parser) {

        // Send the GET request in the background.
        String url = url(urlID);
        new BackgroundRequest().execute("GET", url, parameters, intentName, info, parser);
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class BackgroundRequest extends AsyncTask<Object, Void, Boolean> {
        private String method;
        private String url;
        private HashMap<String,String> parameters;
        private String intentName;
        private HashMap<String,Object> info;
        private Parser parser;

        protected HttpRequestBase requestForMethod(String method) throws ServerException {
            method = method.toUpperCase();
            if (method.equals("GET")) {
                return new HttpGet();
            } else if (method.equals("POST")) {
                return new HttpPost();
            } else if (method.equals("DELETE")) {
                return new HttpDelete();
            } else if (method.equals("PUT")) {
                return new HttpPut();
            } else {
                throw new ServerException(String.format("Used unsupported request method %s", method));
            }
        }

        @Override
        protected Boolean doInBackground(Object... arg) {
            method = (String)arg[0];
            url = (String)arg[1];
            parameters = (HashMap<String,String>)arg[2];
            intentName = (String)arg[3];
            info = (HashMap<String,Object>)arg[4];
            parser = (Parser)arg[5];
            Log.d(TAG, String.format("Request: %s %s %s", method, url, parameters != null ? parameters.toString() : ""));

            try {
                // Choose the request according to the method type.
                HttpRequestBase request = requestForMethod(method);

                // Set the URI
                request.setURI(new URI(url));
                HttpResponse response = client.execute(request);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();

                if (statusCode == 200) {

                    //
                    // Successful response
                    //
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    if (parser != null) {
                        parser.readResponseBuffer(rd);
                        parser.parse();
                    }

                } else {

                    //
                    // HTTP response with wrong status code.
                    //
                    Log.e(TAG, String.format("Request/Response error. status code: %d", statusCode));
                    return false;

                }

            } catch (Exception e) {

                //
                // Exception in response or while parsing response.
                //
                Log.e(TAG, String.format("Request/Response error: %s %s", e.getClass().toString(), e.getMessage()));
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.v(TAG, String.format("Response result: %b", result));
            Intent intent = new Intent(intentName);
            intent.putExtra(SR_SUCCESS, result);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

}
