package com.homage.networking.parsers;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

public class Parser {
    String TAG = "TAG_"+getClass().getName();

    protected Context context;

    protected String parserName;
    protected Class expectedObjectClass;
    public Object objectToParse;
    public HashMap<String, Error> errors;
    public Error error;
    public HashMap<String, Object> parseInfo;

    public Parser() {
        super();
        this.context = context;
    }

    public static String parseOID(JSONObject obj) throws JSONException {
        return obj.getJSONObject("_id").getString("$oid");
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

    public void parse() throws JSONException {
        throw new UnsupportedOperationException("parse not implemented for derived parser.");
    }
}
