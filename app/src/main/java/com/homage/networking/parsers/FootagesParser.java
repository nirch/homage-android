package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Footage;
import com.homage.model.Remake;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FootagesParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

    public Remake remake;

    public FootagesParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }

    @Override
    public void parse() throws JSONException {
        JSONArray footagesInfo = (JSONArray)objectToParse;
        for (int i=0; i<footagesInfo.length();i++) {
            JSONObject footageInfo = footagesInfo.getJSONObject(i);
            parseFootage(footageInfo);
        }
    }

    public void parseFootage(JSONObject footageInfo) throws JSONException {
        Log.d(TAG, String.format("FOOT >>> %s", footageInfo));

        int sceneId = footageInfo.getInt("scene_id");
        Footage footage = remake.findFootageOrCreate(sceneId);

        footage.processedVideoS3Key =       footageInfo.getString("processed_video_s3_key");
        footage.rawVideoS3Key =             footageInfo.getString("raw_video_s3_key");
        footage.status =                    footageInfo.getInt("status");

        footage.save();
    }
}


