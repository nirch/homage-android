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
//        Create pulledAt
        long pulledAt = System.currentTimeMillis();

        RemakeParser remakeParser = new RemakeParser(pulledAt);
        JSONObject remakeInfo = null;


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

        boolean callFromMEScreen = false;
        boolean skipValueIsNull = false;

        if(this.requestInfo != null) {

            if (this.requestInfo.get("me") != null) {
                callFromMEScreen = this.requestInfo.get("me").toString().equals("true");
            } else {
                callFromMEScreen = false;
            }
            if (this.requestInfo.get("skip") != null) {
                skipValueIsNull = this.requestInfo.get("skip").toString().equals("null");
            } else {
                skipValueIsNull = false;
            }

            //        Is this the first page?
            //        If yes delete all remakes not with this pulledAt
            if (skipValueIsNull && !callFromMEScreen) {
                if (remakeInfo != null) {
                    //        Get Story ID
                    String storyOID = Parser.parseOID(remakeInfo.getJSONObject("story_id"));
                    Story story = Story.findByOID(storyOID);
                    User excludedUser = User.getCurrent();
                    //        Delete Remakes
                    story.deleteRemakes(excludedUser, pulledAt);
                }
            }
        }
    }
}