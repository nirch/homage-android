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

    public ConfigParser() {
        super();
        expectedObjectClass = JSONArray.class;
    }

    // All the constants

    final public static String SIGNIFICANT_VIEW_PCT_THRESHOLD = "significant_view_pct_threshold";

    final public static String SHARE_LINK_PREFIX = "share_link_prefix";

    final public static String SHARE_APP_BUTTON = "share_app_button";

    final public static String DOWNLOAD_APP_IOS_URL = "download_app_ios_url";

    final public static String DOWNLOAD_APP_ANDROID_URL = "download_app_android_url";

    final public static String SIGNUP = "signup";

    final public static String LOGIN_FLOW_TYPE = "login_flow_type";

    final public static String LOGIN_FLOW_SKIP_INTRO_VIDEO = "login_flow_skip_intro_video";

    final public static String GUEST_PUBLIC_AS_DEFAULT = "guest_public_as_default";

    final public static String GUEST_ALLOW_PUBLIC = "guest_allow_public";

    final public static String GUEST_ALLOW_SHARE = "guest_allow_share";

    final public static String CACHE_STORIES_VIDEOS = "cache_stories_videos";

    final public static String CACHE_AUDIO = "cache_audio";

    final public static String CACHE_USER_REMAKES_VIDEOS = "cache_user_remakes_videos";

    final public static String CACHE_STORIES_VIDEOS_MAX_COUNT = "cache_stories_videos_max_count";

    final public static String USER_SAVE_REMAKES_POLICY = "user_save_remakes_policy";

    final public static String IN_APP_PURCHASES = "in_app_purchases";

    final public static String IN_APP_PURCHASES_HIDE_PREMIUM_STORIES = "in_app_purchases_hide_premium_stories";

    final public static String REMAKES_PER_PAGE = "remakes_per_page";

    final public static String RECORDER_FIRST_SCENE_CONTEXT_MESSAGE = "recorder_first_scene_context_message";

    final public static String UPLOADER_REPORTS_UPLOADS = "uploader_reports_uploads";

    final public static String RECORDER_SKIP_ARE_YOU_SURE_IN_GOOD_JOB_MESSAGE = "recorder_skip_are_you_sure_in_good_job_message";

    final public static String RECORDER_DISCLAIMER = "recorder_disclaimer";

    final public static String MIXPANEL_TOKEN = "mixpanel_token";

    final public static String RECORDER_FORCED_BBG_POLICY = "recorder_forced_bbg_policy";



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

        SharedPreferences prefs = HomageApplication.getInstance().getSharedPreferences(HomageApplication.SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(SIGNIFICANT_VIEW_PCT_THRESHOLD, parseString(SIGNIFICANT_VIEW_PCT_THRESHOLD,
                app.getResources().getString(R.string.significant_view_pct_threshold)));
        editor.putString(SHARE_LINK_PREFIX, parseString(SHARE_LINK_PREFIX,
                app.getResources().getString(R.string.share_link_prefix)));
        editor.putBoolean(SHARE_APP_BUTTON, parseBool(SHARE_APP_BUTTON,
                app.getResources().getBoolean(R.bool.share_app_button)));
        editor.putString(DOWNLOAD_APP_IOS_URL, parseString(DOWNLOAD_APP_IOS_URL,
                app.getResources().getString(R.string.download_app_ios_url)));
        editor.putString(DOWNLOAD_APP_ANDROID_URL, parseString(DOWNLOAD_APP_ANDROID_URL,
                app.getResources().getString(R.string.download_app_android_url)));
        editor.putString(SIGNUP, parseString(SIGNUP,
                app.getResources().getString(R.string.signup)));
        editor.putString(LOGIN_FLOW_TYPE, parseString(LOGIN_FLOW_TYPE,
                app.getResources().getString(R.string.login_flow_type)));
        editor.putBoolean(LOGIN_FLOW_SKIP_INTRO_VIDEO, parseBool(LOGIN_FLOW_SKIP_INTRO_VIDEO,
                app.getResources().getBoolean(R.bool.login_flow_skip_intro_video)));
        editor.putBoolean(GUEST_PUBLIC_AS_DEFAULT, parseBool(GUEST_PUBLIC_AS_DEFAULT,
                app.getResources().getBoolean(R.bool.guest_public_as_default)));
        editor.putBoolean(GUEST_ALLOW_PUBLIC, parseBool(GUEST_ALLOW_PUBLIC,
                app.getResources().getBoolean(R.bool.guest_allow_public)));
        editor.putBoolean(GUEST_ALLOW_SHARE, parseBool(GUEST_ALLOW_SHARE,
                app.getResources().getBoolean(R.bool.guest_allow_share)));
        editor.putBoolean(CACHE_STORIES_VIDEOS, parseBool(CACHE_STORIES_VIDEOS,
                app.getResources().getBoolean(R.bool.cache_stories_videos)));
        editor.putBoolean(CACHE_AUDIO, parseBool(CACHE_AUDIO,
                app.getResources().getBoolean(R.bool.cache_audio)));
        editor.putBoolean(CACHE_USER_REMAKES_VIDEOS, parseBool(CACHE_USER_REMAKES_VIDEOS,
                app.getResources().getBoolean(R.bool.cache_user_remakes_videos)));
        editor.putString(CACHE_STORIES_VIDEOS_MAX_COUNT, parseString(CACHE_STORIES_VIDEOS_MAX_COUNT,
                app.getResources().getString(R.string.cache_stories_videos_max_count)));
        editor.putString(USER_SAVE_REMAKES_POLICY, parseString(USER_SAVE_REMAKES_POLICY,
                app.getResources().getString(R.string.user_save_remakes_policy)));
        editor.putBoolean(IN_APP_PURCHASES, parseBool(IN_APP_PURCHASES,
                app.getResources().getBoolean(R.bool.in_app_purchases)));
        editor.putBoolean(IN_APP_PURCHASES_HIDE_PREMIUM_STORIES, parseBool(IN_APP_PURCHASES_HIDE_PREMIUM_STORIES,
                app.getResources().getBoolean(R.bool.in_app_purchases_hide_premium_stories)));
        editor.putString(REMAKES_PER_PAGE, parseString(REMAKES_PER_PAGE,
                app.getResources().getString(R.string.remakes_per_page)));
        editor.putBoolean(RECORDER_FIRST_SCENE_CONTEXT_MESSAGE, parseBool(RECORDER_FIRST_SCENE_CONTEXT_MESSAGE,
                app.getResources().getBoolean(R.bool.recorder_first_scene_context_message)));
        editor.putBoolean(UPLOADER_REPORTS_UPLOADS, parseBool(UPLOADER_REPORTS_UPLOADS,
                app.getResources().getBoolean(R.bool.uploader_reports_uploads)));
        editor.putBoolean(RECORDER_SKIP_ARE_YOU_SURE_IN_GOOD_JOB_MESSAGE, parseBool(RECORDER_SKIP_ARE_YOU_SURE_IN_GOOD_JOB_MESSAGE,
                app.getResources().getBoolean(R.bool.recorder_skip_are_you_sure_in_good_job_message)));
        editor.putBoolean(RECORDER_DISCLAIMER, parseBool(RECORDER_DISCLAIMER,
                app.getResources().getBoolean(R.bool.recorder_disclaimer)));
        editor.putString(MIXPANEL_TOKEN, parseString(MIXPANEL_TOKEN,
                app.getResources().getString(R.string.mixpanel_token)));
        editor.putString(RECORDER_FORCED_BBG_POLICY, parseString(RECORDER_FORCED_BBG_POLICY,
                app.getResources().getString(R.string.recorder_forced_bbg_policy)));
        editor.commit();

    }
}


