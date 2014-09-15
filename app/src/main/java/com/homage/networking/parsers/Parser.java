package com.homage.networking.parsers;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class Parser {
    String TAG = "TAG_"+getClass().getName();

    protected Context context;

    protected String parserName;
    protected Class expectedObjectClass;
    protected long startParseTime;

    public Object objectToParse;
    public HashMap<String, Error> errors;
    public Error error;
    public HashMap<String, Object> responseInfo;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private SimpleDateFormat dateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * By default, parsers expect a json object.
     * If a derived parser expects other kind of objects,
     * override the constructor and assign something else to expectedObjectClass.
     */
    public Parser() {
        super();
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        // By default, expects a json object.
        expectedObjectClass = JSONObject.class;

        //
        responseInfo = new HashMap<String, Object>();
    }

    public static String parseOID(JSONObject obj) throws JSONException {
        if (obj.has("_id")) return obj.getJSONObject("_id").getString("$oid");
        if (obj.has("$oid")) return obj.getString("$oid");
        return null;
    }

    public void readResponseBuffer(BufferedReader rd) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }

        if (expectedObjectClass == JSONObject.class) {
            objectToParse = new JSONObject(sb.toString());
        } else if (expectedObjectClass == JSONArray.class) {
            objectToParse = new JSONArray(sb.toString());
        } else {
            throw new JSONException("Unexpected object type returned from server.");
        }
    }

    public Date parseDate(String sDate) {
        try {
            return dateFormatter.parse(sDate);
        } catch (Exception ex) {
            return null;
        }
    }

    public void parse() throws JSONException {
        throw new UnsupportedOperationException("parse not implemented for derived parser.");
    }

    protected void startBenchMark() {
        startParseTime = System.currentTimeMillis();
    }

    protected void endBenchMark() {
        long delta = System.currentTimeMillis() - startParseTime;
        Log.v(TAG, String.format("Parse benchmark: %d", delta));
    }

    //region *** Helpers for parsing JSON objects ***
    protected String parseString(String key, String defaultValue) throws JSONException {
        return parseString((JSONObject)objectToParse, key, defaultValue);
    }
    protected String parseString(JSONObject obj, String key, String defaultValue) throws JSONException {
        if (obj.has(key)) return obj.getString(key);
        return defaultValue;
    }

    protected boolean parseBool(String key, boolean defaultValue) throws JSONException {
        return parseBool((JSONObject) objectToParse, key, defaultValue);
    }
    protected boolean parseBool(JSONObject obj, String key, boolean defaultValue) throws JSONException {
        if (obj.has(key)) return obj.getBoolean(key);
        return defaultValue;
    }

    protected int parseInt(String key, int defaultValue) throws JSONException {
        return parseInt((JSONObject) objectToParse, key, defaultValue);
    }
    protected int parseInt(JSONObject obj, String key, int defaultValue) throws JSONException {
        if (obj.has(key)) return obj.getInt(key);
        return defaultValue;
    }

    protected double parseDouble(String key, double defaultValue) throws JSONException {
        return parseDouble((JSONObject) objectToParse, key, defaultValue);
    }
    protected double parseDouble(JSONObject obj, String key, double defaultValue) throws JSONException {
        if (obj.has(key)) return obj.getDouble(key);
        return defaultValue;
    }

    protected long parseDateAsTimestamp(String key, long defaultValue) throws JSONException {
        return parseDateAsTimestamp((JSONObject)objectToParse, key, defaultValue);
    }

    protected long parseDateAsTimestamp(JSONObject obj, String key, long defaultValue) throws JSONException {
        if (!obj.has(key)) return defaultValue;
        String sDate = obj.getString(key);
        try {
            Date dDate = dateFormatter.parse(sDate);
            long lDate = dDate.getTime();
            return lDate;
        } catch (Exception ex) {
            try {
                // Handle older format, just in case.
                Date dDate = dateFormatter2.parse(sDate);
                long lDate = dDate.getTime();
                return lDate;
            } catch (Exception ex2) {
                return defaultValue;
            }
        }
    }

    protected int parseBoolAsInt(String key, int defaultValue) throws JSONException {
        return parseBoolAsInt((JSONObject) objectToParse, key, defaultValue);
    }
    protected int parseBoolAsInt(JSONObject obj, String key, int defaultValue) throws JSONException {
        if (obj.has(key)) {
            Boolean b = obj.getBoolean(key);
            if (b) {
                return 1;
            } else {
                return 0;
            }
        }
        return defaultValue;
    }
    /*
    Deprecated - Sugar ORM doesn't support Date object very well.
                 Will store dates to the database as simple long timestamp instead.
    protected Date parseDate(String key, Date defaultValue) throws JSONException {
        return parseDate((JSONObject) objectToParse, key, defaultValue);
    }

    protected Date parseDate(JSONObject obj, String key, Date defaultValue) throws JSONException {
        if (!obj.has(key)) return defaultValue;
        String sDate = obj.getString(key);
        try {
            return dateFormatter.parse(sDate);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
    */

    //endregion
}
