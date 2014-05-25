package com.homage.networking.parsers;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class Parser {
    String TAG = "TAG_"+getClass().getName();

    protected Context context;

    protected String parserName;
    protected Class expectedObjectClass;
    public Object objectToParse;
    public HashMap<String, Error> errors;
    public Error error;
    public HashMap<String, Object> parseInfo;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * By default, parsers expect a json object.
     * If a derived parser expects other kind of objects,
     * override the constructor and assign something else to expectedObjectClass.
     */
    public Parser() {
        super();
        this.context = context;

        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        // By default, expects a json object.
        expectedObjectClass = JSONObject.class;
    }

    public static String parseOID(JSONObject obj) throws JSONException {
        if (obj.has("_id")) return obj.getJSONObject("_id").getString("$oid");
        if (obj.has("$oid")) return obj.getString("$oid");
        return null;
    }

    public static String safeStringFromJSONObject(JSONObject obj, String key) throws JSONException {
        if (obj.has(key)) return obj.getString(key);
        return null;
    }

    public static int safeIntFromJSONObject(JSONObject obj, String key) throws JSONException {
        if (obj.has(key)) return obj.getInt(key);
        return 0;
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
}
