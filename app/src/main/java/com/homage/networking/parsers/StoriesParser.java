package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.MemCache;
import com.homage.model.Story;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.security.Timestamp;

public class StoriesParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

    static private int lastParseTime = 0;
    final static private int threshold = 15000;

    public StoriesParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }

    @Override
    public void parse() throws JSONException {
        startBenchMark();

        if (lastParseTime > 0) {
            int now = (int)(System.currentTimeMillis());
            int delta = now - lastParseTime;
            if (delta < threshold) {
                Log.d(TAG, String.format("Stories parsed very recently (%d < %d). ignored.", delta, threshold));
                return;
            }
            lastParseTime = now;
        }
        lastParseTime = (int)(System.currentTimeMillis());

        //
        // Parse, cache and persist all stories in a single transaction.
        //
        MemCache cache = MemCache.sh();
        cache.refreshStories();
        JSONArray stories = (JSONArray)objectToParse;
        StoryParser storyParser = new StoryParser();
        JSONObject storyInfo;
        for (int i=0; i<stories.length();i++) {
            storyInfo = stories.getJSONObject(i);
            storyParser.objectToParse = storyInfo;
            storyParser.parse();
        }
        cache.persistStories();

        //
        // Parse, cache and persist all scenes for all stories in a single transaction.
        //
        cache.refreshScenes();
        ScenesParser scenesParser = new ScenesParser();
        for (int i=0; i<stories.length();i++) {
            storyInfo = stories.getJSONObject(i);
            String oid = Parser.parseOID(storyInfo);
            Story story = cache.getStoryByOID(oid);
            scenesParser.story = story;
            scenesParser.objectToParse = storyInfo.getJSONArray("scenes");
            scenesParser.parse();
        }
        cache.persistScenes();

        endBenchMark();
    }
}


