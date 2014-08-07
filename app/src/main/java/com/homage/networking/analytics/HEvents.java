package com.homage.networking.analytics;


import android.content.Context;
import android.util.Log;

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

    public final static String HK_VIDEO_INIT_TIME = "videoInitTime";
    public final static String HK_VIDEO_FILE_PATH = "videoFilePath";
    public final static String HK_VIDEO_FILE_URL = "videoFileURL";
    public final static String HK_VIDEO_POSITION = "videoPosition";

    private Context context;

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

        switch (eventCode) {
            // Add your analytics implementation here.
            // IMPORTANT: Don't do long operations on the UI thread.
        }
    }
    //endregion

}
