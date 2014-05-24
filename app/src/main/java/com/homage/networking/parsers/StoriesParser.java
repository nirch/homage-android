package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Story;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

public class StoriesParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

    public StoriesParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }



    @Override
    public void parse() throws JSONException {
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


