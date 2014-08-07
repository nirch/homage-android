package com.homage.networking.parsers;

import com.homage.model.Scene;
import com.homage.model.Story;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScenesParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

    public Story story;

    public ScenesParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }

    @Override
    public void parse() throws JSONException {
        JSONArray scenesInfo = (JSONArray)objectToParse;

        for (int i=0; i<scenesInfo.length();i++) {
            JSONObject sceneInfo = scenesInfo.getJSONObject(i);
            parseScene(sceneInfo);
        }
    }

    public void parseScene(JSONObject sceneInfo) throws JSONException {
        /**
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
            }
         */

        int sceneId = parseInt(sceneInfo, "id",0);
        Scene scene = Scene.findOrCreate(story, sceneId, true);
        scene.tag = Scene.sTag(story.getOID(), sceneId);
        scene.context = parseString(sceneInfo, "context", null);
        scene.script = parseString(sceneInfo, "script",null);
        scene.duration = parseInt(sceneInfo, "duration",0);
        scene.videoURL = parseString(sceneInfo, "video",null);
        scene.thumbnailURL = parseString(sceneInfo, "thumbnail",null);
        scene.silhouetteURL = parseString(sceneInfo, "silhouette",null);
        scene.focusPointX = parseDouble(sceneInfo, "focus_point_x",0.5f);
        scene.focusPointY = parseDouble(sceneInfo, "focus_point_y",0.5f);

        //at the moment, all the scenes are selfie enabled
        scene.isSelfie = true;
        //orig: scene.isSelfie = parseBool("selfie",false);
    }
}


