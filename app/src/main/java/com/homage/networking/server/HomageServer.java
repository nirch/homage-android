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

import android.content.Context;
import android.provider.Settings;
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
    }
    //endregion


    //region *** Stories ***
    /**
     * Refetch all stories (and their child objects) info from the server.
     */
    public void refetchStories() {
        Log.v(TAG, "Refetching stories");
        super.GET(R.string.url_stories, null, INTENT_STORIES, null, new StoriesParser());
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

        // The request
        super.GET(
                R.string.url_existing_remake, remakeOID, null,
                INTENT_REMAKE, null, new RemakeParser()
        );
    }

    /**
     * Refetch all remakes for a given story.
     * @param storyOID The story object id of the story the remakes are related to.
     * @param userInfo Optional HashMap<String,Object> containing user info about the request.
     */
    public void refetchRemakesForStory(String storyOID, HashMap<String,Object> userInfo) {
        Log.v(TAG, String.format("Refetching remakes for story OID: %s", storyOID));

        // Request info
        HashMap<String,Object> info = new HashMap<String, Object>();
        info.put(IK_STORY_OID, storyOID);

        // User Info
        if (userInfo != null) info.putAll(userInfo);

        // The GET request
        super.GET(
                R.string.url_story_remakes, storyOID, null,
                INTENT_REMAKES_FOR_STORY, info, new RemakesParser()
        );
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
    //endregion

    //region *** Footages ***
    public void updateFootageUploadStart(String remakeID, int sceneID, String takeID, HashMap<String,Object> userInfo) {
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
        parameters.put("resolution_width", "1280");
        parameters.put("resolution_height", "720");

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
    /**
     * Login user with an email and password.
     * @param email
     * @param password
     */
    public void loginUser(String email, String password) {
        Log.v(TAG, String.format("Login user with email: %s", email));

        HashMap<String, String> parameters = new HashMap<String, String>();

        boolean isPublic;

        if (email!=null) {
            // User
            parameters.put("email", email);
            parameters.put("password",password);

            // TODO: get from settings.
            isPublic = true;
        } else {
            // Guest user
            isPublic = false;
        }

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

        // TODO: implement GCM
        parameters.put("device[push_token]","%3C0a7b7b15%203f0fb335%200e252182%2038675505%20e739547b%2047dd8ab2%20");

        // The user parser
        UserParser userParser = new UserParser();
        userParser.loginParsedUser = true;

        // Post the request
        super.POST(R.string.url_new_user, parameters, INTENT_USER_CREATION, null, userParser);
    }

    public void loginGuest() {
        loginUser(null, null);
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
