package com.homage.networking.parsers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

public class Parser {
    String TAG = getClass().getName();

    protected String parserName;
    protected Class expectedObjectClass;
    public Object objectToParse;
    public HashMap<String, Error> errors;
    public Error error;
    public HashMap<String, Object> parseInfo;

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
