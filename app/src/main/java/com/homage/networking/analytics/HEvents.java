package com.homage.networking.analytics;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.homage.model.User;
import com.homage.networking.server.HomageServer;

import org.bson.types.ObjectId;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class HEvents {
    String TAG = "TAG_"+getClass().getName();

    public final static int H_EVENT_VIDEO_USER_PRESSED_PLAY             = 1001;
    public final static int H_EVENT_VIDEO_WILL_AUTO_PLAY                = 1002;
    public final static int H_EVENT_VIDEO_USER_PRESSED_PAUSE            = 1003;
    public final static int H_EVENT_VIDEO_USER_PRESSED_STOP             = 1004;
    public final static int H_EVENT_VIDEO_BUFFERING_START               = 1006;
    public final static int H_EVENT_VIDEO_BUFFERING_END                 = 1007;
    public final static int H_EVENT_VIDEO_PLAYING                       = 1008;
    public final static int H_EVENT_VIDEO_PLAYER_FINISH                 = 1009;
    public final static int H_EVENT_VIDEO_PLAYER_WILL_PLAY              = 1010;
    public final static int H_EVENT_VIDEO_FULL_STOP                     = 1011;

    public final static String HK_VIDEO_INIT_TIME          = "videoInitTime";
    public final static String HK_VIDEO_FILE_PATH          = "videoFilePath";
    public final static String HK_VIDEO_ENTITY_TYPE        = "videoEntityType";
    public final static String HK_VIDEO_ENTITY_ID          = "videoEntityID";
    public final static String HK_VIDEO_USER_ID            = "videoUserID";
    public final static String HK_VIDEO_PLAYBACK_TIME      = "videoPlaybackTime";
    public final static String HK_VIDEO_TOTAL_DURATION     = "videoTotalDuration";
    public final static String HK_VIDEO_ORIGINATING_SCREEN = "originatingScreen";


    public final static int H_STORY       = 0;
    public final static int H_REMAKE      = 1;
    public final static int H_INTRO_MOVIE = 2;
    public final static int H_SCENE       = 3;


    public final static String HK_VIDEO_FILE_URL = "videoFileURL";
    public final static String HK_VIDEO_POSITION = "videoPosition";

    private Context context;
    private String  viewID;

    //region *** singleton pattern ***
    public void init(Context context) {
        // A reference to the context
        this.context = context;

        // Do your initializations here.
        // .
        // .
        // .
    }

    private static HEvents instance = new HEvents();
    public static HEvents sharedInstance() {
        if(instance == null) instance = new HEvents();
        return instance;
    }
    public static HEvents sh() {
        return HEvents.sharedInstance();
    }
    //endregion

    //region *** Track an event ***
    public void track(int eventCode, HashMap<String, Object>info) {
        long eventTimeStamp = System.currentTimeMillis();
        Log.v(TAG, String.format("%d Event code: %d", eventTimeStamp, eventCode));

        int entityType    = Integer.parseInt(info.get(HK_VIDEO_ENTITY_TYPE).toString());
        String entityID   = info.get(HK_VIDEO_ENTITY_ID).toString();

        if (User.getCurrent() == null) return;
        String userID     = User.getCurrent().getOID().toString();
        int originatingScreen = Integer.parseInt(info.get(HEvents.HK_VIDEO_ORIGINATING_SCREEN).toString());

        int playbackTime;
        int totalDuration;
        int playbackTimeMilSeconds;
        int totalDurationMilSeconds;

        HashMap props = new HashMap<String,String>();
        props.put("entity_id", entityID);
        props.put("entity_type", Integer.toString(entityType));
        props.put("originating_screen", Integer.toString(originatingScreen));

        switch (eventCode) {
            // Add your analytics implementation here.
            // IMPORTANT: Don't do long operations on the UI thread.

            case H_EVENT_VIDEO_USER_PRESSED_PLAY:
                //viewID = new ObjectId().toString();
                //HomageServer.sh().reportVideoViewStart(viewID,entityType,entityID,userID);
                break;
            case H_EVENT_VIDEO_PLAYER_WILL_PLAY:
                viewID = new ObjectId().toString();
                HomageServer.sh().reportVideoViewStart(viewID,entityType,entityID,userID,originatingScreen);
                HMixPanel.sh().track("start_play_video",props);
                break;
            case H_EVENT_VIDEO_PLAYER_FINISH:
                if (viewID == null) return;
                playbackTimeMilSeconds = Integer.parseInt(info.get(HEvents.HK_VIDEO_PLAYBACK_TIME).toString());
                playbackTime = playbackTimeMilSeconds / 1000;

                totalDurationMilSeconds = Integer.parseInt(info.get(HEvents.HK_VIDEO_TOTAL_DURATION).toString());
                totalDuration = totalDurationMilSeconds / 1000;
                HomageServer.sh().reportVideoViewStop(viewID,entityType,entityID,userID,playbackTime,totalDuration,originatingScreen);
                props.put("playing_time",Integer.toString(playbackTime));
                props.put("total_duration",Integer.toString(totalDuration));
                HMixPanel.sh().track("finish_playing_video",props);
                viewID = null;

                break;
            case H_EVENT_VIDEO_FULL_STOP:
                if (viewID == null) return;
                playbackTimeMilSeconds = Integer.parseInt(info.get(HEvents.HK_VIDEO_PLAYBACK_TIME).toString());
                playbackTime = playbackTimeMilSeconds / 1000;

                totalDurationMilSeconds = Integer.parseInt(info.get(HEvents.HK_VIDEO_TOTAL_DURATION).toString());
                totalDuration = totalDurationMilSeconds / 1000;
                HomageServer.sh().reportVideoViewStop(viewID,entityType,entityID,userID,playbackTime,totalDuration,originatingScreen);
                props.put("playing_time",Integer.toString(playbackTime));
                props.put("total_duration",Integer.toString(totalDuration));
                HMixPanel.sh().track("stop_playing_video",props);
                viewID = null;
                break;
        }
    }
    //endregion

}
