package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RemakesParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

    public RemakesParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }

    @Override
    public void parse() throws JSONException {
        JSONArray remakes = (JSONArray)objectToParse;

        RemakeParser remakeParser = new RemakeParser();
        JSONObject remakeInfo;

        for (int i=0; i<remakes.length();i++) {
            remakeInfo = remakes.getJSONObject(i);
            String remakeOID = parseOID(remakeInfo);
            remakeParser.objectToParse = remakeInfo;

            try {
                remakeParser.parse();
            } catch (Exception ex) {
                Log.e(TAG, String.format("Failed parsing remake with OID:%s . skipped.", remakeOID) );
            }
        }
    }
}