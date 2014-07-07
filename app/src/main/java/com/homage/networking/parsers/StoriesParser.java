package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Story;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.security.Timestamp;

public class StoriesParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

    static private int lastParseTime = 0;
    final static private int threshold = 30000;

    public StoriesParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }

    @Override
    public void parse() throws JSONException {
        if (lastParseTime > 0) {
            int now = (int)(System.currentTimeMillis());
            int delta = now - lastParseTime;
            if (delta < threshold) {
                Log.d(TAG, "Stories parsed very recently. ignored.");
                return;
            }
            lastParseTime = now;
        }

        lastParseTime = (int)(System.currentTimeMillis());

        JSONArray stories = (JSONArray)objectToParse;
        StoryParser storyParser = new StoryParser();
        JSONObject story;

        for (int i=0; i<stories.length();i++) {
            story = stories.getJSONObject(i);
            storyParser.objectToParse = story;
            storyParser.parse();
        }
    }
}


