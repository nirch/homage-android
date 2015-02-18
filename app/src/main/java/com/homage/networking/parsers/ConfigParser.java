package com.homage.networking.parsers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.homage.app.R;
import com.homage.app.main.HomageApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConfigParser extends Parser {
    String TAG = "TAG_"+getClass().getName();


    static private int lastParseTime = 0;
    final static private int threshold = 15000;
    private SharedPreferences prefs = HomageApplication.getInstance().getSharedPreferences(HomageApplication.SETTINGS_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();

    public ConfigParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }

    // All the constants

    final public static String SHARE_LINK_PREFIX = "share_link_prefix";
    final public static String SIGNIFICANT_VIEW_PCT_THRESHOLD = "significant_view_pct_threshold";
    final public static String MIRROR_SELFIE_SILHOUETTE = "mirror_selfie_silhouette";
    final public static String MIXPANEL_TOKEN = "mixpanel_token";
    final public static String RECORDER_FORCED_BBG_POLICY = "recorder_forced_bbg_policy";
    final public static String REMAKES_SAVE_TO_DEVICE = "remakes_save_to_device";
    final public static String REMAKES_SAVE_TO_DEVICE_PREMIUM_ID = "remakes_save_to_device_premium_id";



    /**
     *
     * Example For a remake object.
     *
     {
     "share_link_prefix": "http://play-test.homage.it",
     "significant_view_pct_threshold": 0.5,
     "mirror_selfie_silhouette": true,
     "mixpanel_token": "bab0997e7171d56daf35df751f523962",
     "recorder_forced_bbg_policy": 0,
     "remakes_save_to_device": 2,
     "remakes_save_to_device_premium_id": {
     "$oid": "5492ae082b9986d480000482"
     }
     }

     */

    @Override
    public void parse() throws JSONException {

        JSONObject remakeInfo = (JSONObject)objectToParse;
        String oid = Parser.parseOID(remakeInfo);
        Log.v(TAG, String.format("Parsing config"));

        HomageApplication app = HomageApplication.getInstance();

        editor.putString(SHARE_LINK_PREFIX, parseString("SHARE_LINK_PREFIX",
                app.getResources().getString(R.string.share_link_prefix)));
        editor.putString(SIGNIFICANT_VIEW_PCT_THRESHOLD, parseString("SIGNIFICANT_VIEW_PCT_THRESHOLD",
                app.getResources().getString(R.string.significant_view_pct_threshold)));
        editor.putString(MIRROR_SELFIE_SILHOUETTE, parseString("MIRROR_SELFIE_SILHOUETTE",
                app.getResources().getString(R.string.mirror_selfie_silhouette)));
        editor.putString(MIXPANEL_TOKEN, parseString("MIXPANEL_TOKEN",
                app.getResources().getString(R.string.mixpanel_token)));
        editor.putString(RECORDER_FORCED_BBG_POLICY, parseString("RECORDER_FORCED_BBG_POLICY",
                app.getResources().getString(R.string.recorder_forced_bbg_policy)));
        editor.putString(REMAKES_SAVE_TO_DEVICE, parseString("REMAKES_SAVE_TO_DEVICE",
                app.getResources().getString(R.string.remakes_save_to_device)));

        String remakes_save_to_device_premium_id =
                Parser.parseOID(remakeInfo.getJSONObject(REMAKES_SAVE_TO_DEVICE_PREMIUM_ID));
        if (remakes_save_to_device_premium_id == null){
            remakes_save_to_device_premium_id =
                    app.getResources().getString(R.string.remakes_save_to_device_premium_id); }
        editor.putString(REMAKES_SAVE_TO_DEVICE_PREMIUM_ID, remakes_save_to_device_premium_id);

    }
}


