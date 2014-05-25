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
          "remakes_num": 1,
          "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/thum+main.jpg",
          "level": 0,
          "_id": {
            "$oid": "536e10a0a25ca8ffc0000188"
          },
          "scenes": [
            {
              "selfie": true,
              "id": 1,
              "ebox": "C:/Development/Algo/Full.ebox",
              "contour": "C:/Users/Administrator/Documents/Contours/torso 360.ctr",
              "duration": 5000,
              "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/thumb+1.jpg",
              "silhouette": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/silo+1.png",
              "context": "You've spotted a burglary with your superior vision!",
              "script": "You're on the look out for crime, and you spot something",
              "video": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/scene+1.mp4"
            },
            {
              "selfie": true,
              "id": 2,
              "ebox": "C:/Development/Algo/Full.ebox",
              "contour": "C:/Users/Administrator/Documents/Contours/torso 360.ctr",
              "duration": 3000,
              "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/thumb+2.jpg",
              "silhouette": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/silo+2.png",
              "context": "You are on the chase",
              "script": "Nodbody can run faster than you!",
              "video": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/scene+2.mp4"
            },
            {
              "selfie": true,
              "id": 3,
              "ebox": "C:/Development/Algo/Full.ebox",
              "contour": "C:/Users/Administrator/Documents/Contours/torso 360.ctr",
              "duration": 3000,
              "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/thumb+3.jpg",
              "silhouette": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/silo+3.png",
              "context": "Take down the bandit!(remember to be with your back to the camera)!",
              "script": "Lunge and surprise this crook!",
              "video": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/scene+3.mp4"
            },
            {
              "selfie": true,
              "id": 4,
              "ebox": "C:/Development/Algo/Full.ebox",
              "contour": "C:/Users/Administrator/Documents/Contours/knees up 360.ctr",
              "duration": 5500,
              "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/thumb+4.jpg",
              "silhouette": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/silo+4.png",
              "context": "You gotta fly, more crime awaits justice!",
              "script": "Take off to the next adventure",
              "video": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/scene+4.mp4"
            }
          ],
          "description": "Defend your city!",
          "name": "Superior Man 360",
          "active": true,
          "thumbnail_rip": 15,
          "order_id": 16,
          "video": "https://s3.amazonaws.com/homageapp/Stories/Superior+Man+360/Superior+Man+360.mp4"
        }
     */

    @Override
    public void parse() throws JSONException {
        JSONObject storyInfo = (JSONObject)objectToParse;
        Log.v(TAG, String.format("Parsing a story: %s", storyInfo.getString("name")));

        Story story = Story.findOrCreate(Parser.parseOID(storyInfo));
        story.remakesNum =      storyInfo.getInt("remakes_num");
//        story.thumbnail =       storyInfo.getString("thumbnail");
//        story.level =           storyInfo.getInt("level");
        story.description =     storyInfo.getString("description");
        story.name =            storyInfo.getString("name");
        story.active =          storyInfo.getBoolean("active");
//        story.thumbnailRip =    storyInfo.getInt("thumbnail_rip");
//        story.orderId =         storyInfo.getInt("order_id");
//        story.video =           storyInfo.getString("video");
        story.save();
    }
}