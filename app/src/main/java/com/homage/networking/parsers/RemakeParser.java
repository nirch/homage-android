package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;

import org.json.JSONException;
import org.json.JSONObject;

public class RemakeParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

     /**
     *
     * Example For a remake object.
     *
      {
          "_id": {"$oid": "5381a31e70b35d082f00000d"},

          "story_id": {"$oid": "534fc5b9924daff68b0000e9"},
          "thumbnail_s3_key": "Remakes/5381a31e70b35d082f00000d/The Oscars_5381a31e70b35d082f00000d.jpg",
          "thumbnail": "https://s3.amazonaws.com/homageapp/Stories/Oscar+480/Oscar+thumb.jpg",

          "status": 0,
          "video_s3_key": "Remakes/5381a31e70b35d082f00000d/The Oscars_5381a31e70b35d082f00000d.mp4",
          "created_at": "2014-05-25 08:00:30 +0000",
          "user_id": {"$oid": "5381797a70b35d082f00000c"},
          "footages": [
            .
            .
            .
          ]
      }

     */

    @Override
    public void parse() throws JSONException {
        JSONObject remakeInfo = (JSONObject)objectToParse;
        String oid = Parser.parseOID(remakeInfo);
        Log.v(TAG, String.format("Parsing a remake with oid:%s", oid));

        String userOID = Parser.parseOID(remakeInfo.getJSONObject("user_id"));
        User user = User.findByOID(userOID);

        String storyOID = Parser.parseOID(remakeInfo.getJSONObject("story_id"));
        Story story = Story.findByOID(storyOID);

        if (story == null) {
            Log.e(TAG, String.format("Can't parse remakes for unknown story: %s. Skipped.", storyOID));
            return;
        }

        int status = parseInt("status",-1);

        if (status == Remake.Status.DELETED.getValue()) {
            Remake remake = Remake.findByOID(oid);
            if (remake != null) remake.delete();
            return;
        }

        Remake remake = Remake.findOrCreate(oid,story,user);

        remake.status =         parseInt("status",-1);
        remake.thumbnailURL =   parseString("thumbnail",null);
        remake.videoURL =       parseString("video",null);
        remake.shareURL =       parseString("share_link",null);
        remake.userFullname =       parseString("user_fullname",null);
        remake.grade =          parseInt("grade",0);
        remake.createdAt =      parseDateAsTimestamp("created_at",-1);
        remake.stillPublic =    true;
        remake.userID      =    Parser.parseOID(remakeInfo.getJSONObject("user_id"));
        remake.isLiked =        parseBool("is_liked",false);
        remake.likesCount =     parseInt("like_count",0);
        remake.sharesCount =    parseInt("share_count",0);
        remake.viewsCount =     parseInt("views",0);

        remake.save();

        responseInfo.put("remakeOID", remake.getOID());

        Log.d(TAG, String.format("Remake:%s status:%d", remake.getOID(), remake.status));

        // Parse the footages for this remake.
        FootagesParser footagesParser = new FootagesParser();
        footagesParser.objectToParse = remakeInfo.getJSONArray("footages");
        footagesParser.remake = remake;
        footagesParser.parse();

    }
}