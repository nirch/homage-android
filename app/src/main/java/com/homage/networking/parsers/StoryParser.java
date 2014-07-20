package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Story;

import org.json.JSONException;
import org.json.JSONObject;

public class StoryParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

     /**
     *
     * Example For a story object.
     *
        {
          "_id": {"$oid": "536e10a0a25ca8ffc0000188"},
          "remakes_num": 1,
          "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/thum+main.jpg",
          "level": 0,
          "description": "Defend your city!",
          "name": "Superior Man 360",
          "active": true,
          "thumbnail_rip": 15,
          "order_id": 16,
          "video": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/Superior+Man+360.mp4",
          "scenes": [ ... ],
        }
     */

    @Override
    public void parse() throws JSONException {
        JSONObject storyInfo = (JSONObject)objectToParse;
        Log.v(TAG, String.format("Parsing a story: %s", storyInfo.getString("name")));

        Story story = Story.findOrCreate(Parser.parseOID(storyInfo), false);
        story.remakesNum =      parseInt("remakes_num",0);
        story.thumbnail =       parseString("thumbnail",null);
        story.level =           parseInt("level",0);
        story.description =     parseString("description",null);
        story.name =            parseString("name",null);
        story.active =          parseBoolAsInt("active", 0);
        story.thumbnailRip =    parseInt("thumbnail_rip",0);
        story.orderId =         parseInt("order_id",0);
        story.video =           parseString("video",null);
        story.save();

        // Parse the scenes for this story.
        ScenesParser scenesParser = new ScenesParser();
        scenesParser.objectToParse = storyInfo.getJSONArray("scenes");
        scenesParser.story = story;
        scenesParser.parse();
    }
}