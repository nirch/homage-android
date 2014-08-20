/**
    _________       Homage
   /   _____/ ______________  __ ___________
   \_____  \_/ __ \_  __ \  \/ // __ \_  __ \
   /        \  ___/|  | \/\   /\  ___/|  | \/
  /_______  /\___  >__|    \_/  \___  >__|
          \/     \/ By: Aviv Wolf.  \/

 A singleton class wrapping requests to Homage's web service.
 Derived from the abstract class Server


    Supported methods:
    ------------------

        - GET  >> refetchStories

        - POST >> createRemake             (storyOID, userOID, resolution)
        - GET  >> refetchRemake            (remakeOID)
        - GET  >> refetchRemakesForStory   (storyOID)
        - GET  >> refetchRemakesForUser    (userOID)

        - POST >> loginUser                (email, password)


 */
package com.homage.networking.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.device.Device;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.parsers.RemakeParser;
import com.homage.networking.parsers.RemakesParser;
import com.homage.networking.parsers.StoriesParser;
import com.homage.networking.parsers.UserParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class HomageServer extends Server {
    String TAG = "TAG_"+getClass().getName();

    //region *** Intent names ***
    final static public String INTENT_USER_CREATION             = "intent user creation";
    final static public String INTENT_STORIES                   = "intent stories";
    final static public String INTENT_REMAKE_CREATION           = "intent remake creation";
    final static public String INTENT_REMAKE                    = "intent remake";
    final static public String INTENT_USER_REMAKES              = "intent user remakes";
    final static public String INTENT_REMAKE_DELETION           = "intent remake deletion";
    final static public String INTENT_REMAKES_FOR_STORY         = "intent remake for stories";
    final static public String INTENT_FOOTAGE_UPLOAD_SUCCESS    = "intent footage upload success";
    final static public String INTENT_FOOTAGE_UPLOAD_START      = "intent footage upload start";
    final static public String INTENT_TEXT                      = "intent text";
    final static public String INTENT_RENDER                    = "intent render";
    final static public String INTENT_USER_UPDATED              = "intent user updated";
    final static public String INTENT_USER_PREFERENCES_UPDATE   = "intent user preference update";
    //endregion

    //region *** info keys ***
    final static public String IK_STORY_OID         = "storyOID";
    final static public String IK_REMAKE_OID        = "remakeOID";
    final static public String IK_USER_OID          = "userOID";
    //endregion

    //region *** more settings ***
    public boolean prefetchTopRemakes = false;
    public int topRemakesCount = 10;
    //endregion

    //region *** singleton pattern ***
    private static HomageServer instance = new HomageServer();
    public static HomageServer sharedInstance() {
        if(instance == null) instance = new HomageServer();
        return instance;
    }
    public static HomageServer sh() {
        return HomageServer.sharedInstance();
    }
    public void init(Context context) {
        super.init(context);

        // Cache the required urls, according to settings in server_cfg.xml
        ArrayList<Integer> urlIDs = new ArrayList<Integer>();
        urlIDs.add(R.string.url_stories);
        urlIDs.add(R.string.url_delete_remake);
        urlIDs.add(R.string.url_existing_remake);
        urlIDs.add(R.string.url_footage);
        urlIDs.add(R.string.url_new_remake);
        urlIDs.add(R.string.url_new_user);
        urlIDs.add(R.string.url_render);
        urlIDs.add(R.string.url_story_remakes);
        urlIDs.add(R.string.url_report_inappropriate);
        urlIDs.add(R.string.url_text);
        urlIDs.add(R.string.url_update_user);
        urlIDs.add(R.string.url_user_remakes);
        super.initURLSCache(urlIDs);

        // Set the user agent.
        userAgent = String.format(
                "%s:::Homage Android App:::V%s",
                userAgent,
                HomageApplication.getInstance().getVersionName()
        );

        // More settings
        Resources res = context.getResources();
        prefetchTopRemakes = res.getBoolean(R.bool.prefetch_top_remakes);
        topRemakesCount = res.getInteger(R.integer.top_remakes_count);
    }
    //endregion

    //region *** Stories ***
    /**
     * Refetch all stories (and their child objects) info from the server.
     */
    public void refetchStories() {
        Log.v(TAG, "Refetching stories");

        HashMap<String, String> parameters;

        if (prefetchTopRemakes) {
            // Also prefetch the top remakes for each story.
            parameters = new HashMap<String, String>();
            parameters.put("remakes", "6");
        } else {
            // Fetch stories only.
            parameters = null;
        }
        super.GET(R.string.url_stories, null, parameters, INTENT_STORIES, null, new StoriesParser());
    }
    //endregion

    //region *** Remakes ***
    /**
     *
     * @param storyOID the story oid the remake will be of
     * @param userOID the user that will do the remake
     * @param resolution 360, 720 etc.
     */
    public void createRemake(String storyOID, String userOID, String resolution, HashMap<String,Object> userInfo) {
        Log.v(TAG, String.format("Create remake for story OID: %s user OID: %s", storyOID, userOID));

        // Request info
        HashMap<String,Object> info = new HashMap<String, Object>();
        info.put(IK_STORY_OID, storyOID);
        info.put(IK_USER_OID, userOID);

        // User Info
        if (userInfo != null) info.putAll(userInfo);

        // Request parameters
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("story_id", storyOID);
        parameters.put("user_id", userOID);
        parameters.put("resolution", resolution);

        // The POST request
        super.POST(
                R.string.url_new_remake, parameters,
                INTENT_REMAKE_CREATION, null, new RemakeParser()
        );
    }

    /**
     * A simple GET request to the server
     * Example URL: http://54.204.34.168:4567/remake/52d7a02edb25451630000002
     * @param remakeOID the oid of the remake object.
     */
    public void refetchRemake(String remakeOID, HashMap<String,Object> userInfo) {
        Log.v(TAG, String.format("refetch remake with OID: %s", remakeOID));

        // Request info
        HashMap<String,Object> info = new HashMap<String, Object>();
        info.put(IK_REMAKE_OID, remakeOID);

        // User Info
        if (userInfo != null) info.putAll(userInfo);

        // Suffix hack (prevent caching)
        Long tsLong = System.currentTimeMillis();
        int ts = tsLong.intValue();
        String suffix = String.format("%s?ts=%d",remakeOID, ts);

        // The request
        super.GET(
                R.string.url_existing_remake, suffix, null,
                INTENT_REMAKE, null, new RemakeParser()
        );
    }

    /**
     * Refetch all remakes for a given story.
     * @param storyOID The story object id of the story the remakes are related to.
     * @param userInfo Optional HashMap<String,Object> containing user info about the request.
     * @param limit Optional Integer indicating the max number of results.
     * @param userInfo Optional HashMap<String,Object> containing user info about the request.
     */
    public void refetchRemakesForStory(String storyOID, HashMap<String,Object> userInfo, Integer limit, Integer skip) {
        Log.v(TAG, String.format("Refetching remakes for story OID: %s", storyOID));

        // Request info
        HashMap<String,Object> info = new HashMap<String, Object>();
        info.put(IK_STORY_OID, storyOID);

        // Parameters
        HashMap<String, String> parameters = null;
        if (limit != null || skip != null) {
            parameters = new HashMap<String, String>();
            if (limit != null) parameters.put("limit", String.valueOf(limit));
            if (skip != null) parameters.put("skip", String.valueOf(skip));
        }

        // User Info
        if (userInfo != null) info.putAll(userInfo);

        // The GET request
        super.GET(
                R.string.url_story_remakes,
                storyOID,
                parameters,
                INTENT_REMAKES_FOR_STORY,
                info,
                new RemakesParser());
    }

    public void refetchRemakesForStory(String storyOID, HashMap<String,Object> userInfo) {
        refetchRemakesForStory(storyOID, userInfo, null, null);
    }

    /**
     * Refetch all remakes for a given user.
     * @param userOID The user object id of the user the remakes are related to.
     * @param userInfo Optional HashMap<String,Object> containing user info about the request.
     */
    public void refetchRemakesForUser(String userOID, HashMap<String,Object> userInfo) {
        Log.v(TAG, String.format("Refetching remakes for user OID: %s", userOID));

        // Request info
        HashMap<String,Object> info = new HashMap<String, Object>();
        info.put(IK_USER_OID, userOID);

        // User Info
        if (userInfo != null) info.putAll(userInfo);

        // The GET request
        super.GET(
                R.string.url_user_remakes, userOID, null,
                INTENT_USER_REMAKES, info, new RemakesParser()
        );
    }

    public void deleteRemake(String remakeOID, HashMap<String,Object> userInfo) {
        Log.v(TAG, String.format("Deleting remake from server: %s", remakeOID));
        super.DELETE(
                R.string.url_delete_remake,
                remakeOID,
                null,
                INTENT_REMAKE_DELETION,
                userInfo,
                new RemakeParser()
        );
    }

    //endregion

    //region *** Footages ***
    public void putFootage(String remakeID, int sceneID, String takeID, HashMap<String, Object> userInfo) {
        Log.v(TAG, String.format("Update server about upload start, take id:%s", takeID));

        // Request parameters
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("remake_id", remakeID);
        parameters.put("scene_id", String.valueOf(sceneID));
        parameters.put("take_id", takeID);

        super.PUT(R.string.url_footage, parameters,
                INTENT_FOOTAGE_UPLOAD_START, userInfo, new RemakeParser()
                );
    }

    public void updateFootageUploadSuccess(String remakeID, int sceneID, String takeID, HashMap<String,Object> userInfo) {
        Log.v(TAG, String.format("Update server about upload success, take id:%s", takeID));

        // Request parameters
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("remake_id", remakeID);
        parameters.put("scene_id", String.valueOf(sceneID));
        parameters.put("take_id", takeID);

        // TODO: get this from actual video size / camera settings
        parameters.put("resolution_width", "1280");
        parameters.put("resolution_height", "720");

        // User info
        if (userInfo==null) {
            userInfo = new HashMap<String,Object>();
        }

        userInfo.put("remakeID", remakeID);
        userInfo.put("sceneID", sceneID);
        userInfo.put("takeID", takeID);
        if (!userInfo.containsKey("attemptCount")) userInfo.put("attemptCount",1);

        super.POST(R.string.url_footage, parameters,
            INTENT_FOOTAGE_UPLOAD_SUCCESS, userInfo, new RemakeParser()
        );
    }
    //endregion

    //region *** Render ***
    public void renderRemake(String remakeOID) {
        Log.v(TAG, String.format("Render remake with OID: %s", remakeOID));

        // Request info
        HashMap<String,Object> info = new HashMap<String, Object>();
        info.put(IK_REMAKE_OID, remakeOID);

        // Request parameters
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("remake_id", remakeOID);

        // The POST request
        super.POST(R.string.url_render, parameters, INTENT_RENDER, info, new RemakeParser());
    }
    //endregion

    //region *** Users ***
    private void addFaceBookInfoToParameters(HashMap<String, String> parameters, HashMap<String, String>fbInfo) {
        if (fbInfo != null) {
            parameters.put("facebook[birthday]", fbInfo.get("birthday"));
            parameters.put("facebook[first_name]", fbInfo.get("first_name"));
            parameters.put("facebook[id]", fbInfo.get("id"));
            parameters.put("facebook[last_name]", fbInfo.get("last_name"));
            parameters.put("facebook[link]", fbInfo.get("link"));
            parameters.put("facebook[name]", fbInfo.get("name"));
            parameters.put("facebook[email]", fbInfo.get("email"));
        }
    }

    /**
     * Create user with an email and password.
     * @param email
     * @param password
     */
    public void createUser(String email, String password, HashMap<String, String>fbInfo) {
        Log.v(TAG, String.format("Create user with email: %s", email));

        HashMap<String, String> parameters = new HashMap<String, String>();

        boolean isPublic;

        if (email!=null) {
            // User
            parameters.put("email", email);
            // TODO: get from settings.
            isPublic = true;
        } else {
            // Guest user
            isPublic = false;
        }
        if (password != null) parameters.put("password",password);

        if (isPublic) {
            parameters.put("is_public","1");
        } else {
            parameters.put("is_public","0");
        }
        parameters.put("device[identifier_for_vendor]",
            Settings.Secure.getString(HomageApplication.getContext().getContentResolver(),Settings.Secure.ANDROID_ID)
        );
        parameters.put("device[model]", Device.getDeviceModel());
        parameters.put("device[name]","Android Phone");

        // TODO: implement GCM push identifier
        parameters.put("device[push_token]","xxx");

        // Facebook
        if (fbInfo != null) addFaceBookInfoToParameters(parameters, fbInfo);

        // The user parser
        UserParser userParser = new UserParser();
        userParser.loginParsedUser = true;

        // Post the request
        super.POST(R.string.url_new_user, parameters, INTENT_USER_CREATION, null, userParser);
    }




    public void createGuest() {
        createUser(null, null, null);
    }

    public void updateUserUponJoin(User user, String email, String password, HashMap<String, String>fbInfo) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("user_id", user.getOID());
        parameters.put("email", email);
        if (password != null) parameters.put("password",password);
        parameters.put("is_public", "1");
        parameters.put("device[identifier_for_vendor]",
            Settings.Secure.getString(HomageApplication.getContext().getContentResolver(),Settings.Secure.ANDROID_ID)
        );
        parameters.put("device[model]", Device.getDeviceModel());
        parameters.put("device[name]","Android Phone");

        // Facebook
        if (fbInfo != null) addFaceBookInfoToParameters(parameters, fbInfo);

        // The user parser
        UserParser userParser = new UserParser();
        userParser.loginParsedUser = true;

        // Post the request
        super.PUT(R.string.url_update_user, parameters, INTENT_USER_UPDATED, null, userParser);
    }

    public void updateUserPreferences(HashMap<String, String> parameters) {
        UserParser userParser = new UserParser();
        super.PUT(R.string.url_update_user, parameters, INTENT_USER_PREFERENCES_UPDATE, null, userParser);
    }


    public void reportRemakeAsInappropriate(String remakeID)
    {
        Log.v(TAG, String.format("reporting remake: %s" , remakeID));
        HashMap<String, String> parameters = new HashMap<String, String>();

        parameters.put("remake_id",remakeID);
        String userId = User.getCurrent().getOID();
        parameters.put("user_id",userId);
        super.POST(R.string.url_report_inappropriate, parameters, null, null, null);
    }


    //endregion
}
