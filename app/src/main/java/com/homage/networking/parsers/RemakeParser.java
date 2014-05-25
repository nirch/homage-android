package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class RemakeParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

     /**
     *
     * Example For a remake object.
     *
      {
          "story_id": {
            "$oid": "534fc5b9924daff68b0000e9"
          },
          "thumbnail_s3_key": "Remakes/5381a31e70b35d082f00000d/The Oscars_5381a31e70b35d082f00000d.jpg",
          "footages": [
            {
              "status": 0,
              "scene_id": 1,
              "processed_video_s3_key": "Remakes/5381a31e70b35d082f00000d/processed_scene_1.mov",
              "raw_video_s3_key": "Remakes/5381a31e70b35d082f00000d/raw_scene_1.mov"
            },
            {
              "status": 0,
              "scene_id": 2,
              "processed_video_s3_key": "Remakes/5381a31e70b35d082f00000d/processed_scene_2.mov",
              "raw_video_s3_key": "Remakes/5381a31e70b35d082f00000d/raw_scene_2.mov"
            },
            {
              "status": 0,
              "scene_id": 3,
              "processed_video_s3_key": "Remakes/5381a31e70b35d082f00000d/processed_scene_3.mov",
              "raw_video_s3_key": "Remakes/5381a31e70b35d082f00000d/raw_scene_3.mov"
            }
          ],
          "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Oscar+480/Oscar+thumb.jpg",
          "_id": {
            "$oid": "5381a31e70b35d082f00000d"
          },
          "status": 0,
          "video_s3_key": "Remakes/5381a31e70b35d082f00000d/The Oscars_5381a31e70b35d082f00000d.mp4",
          "created_at": "2014-05-25 08:00:30 +0000",
          "user_id": {
            "$oid": "5381797a70b35d082f00000c"
          }
      }

     */

    @Override
    public void parse() throws JSONException {
        JSONObject remakeInfo = (JSONObject)objectToParse;
        String oid = Parser.parseOID(remakeInfo);
        Log.v(TAG, String.format("Parsing a remake with oid:%s", oid));

        Remake remake = Remake.findOrCreate(oid);

        remake.status =         remakeInfo.getInt("status");
        remake.thumbnailURL =   Parser.safeStringFromJSONObject(remakeInfo, "thumbnail");
        remake.videoURL =       Parser.safeStringFromJSONObject(remakeInfo, "video");
        remake.shareURL =       Parser.safeStringFromJSONObject(remakeInfo, "share_link");
        remake.grade =          Parser.safeIntFromJSONObject(remakeInfo, "grade");
        remake.stillPublic =    true;
        remake.createdAt =      parseDate(remakeInfo.getString("created_at"));

        String userOID = Parser.parseOID(remakeInfo.getJSONObject("user_id"));
        User user = User.findByOID(userOID);
        assert(user.getClass() == User.class);
        remake.user = user;

        String storyOID = Parser.parseOID(remakeInfo.getJSONObject("story_id"));
        Story story = Story.findByOID(storyOID);
        assert(story.getClass() == Story.class);
        remake.story = story;
        remake.save();

        // Parse the footages for this remake.
        FootagesParser footagesParser = new FootagesParser();
        footagesParser.objectToParse = remakeInfo.getJSONArray("footages");
        footagesParser.remake = remake;
        footagesParser.parse();

    }
}