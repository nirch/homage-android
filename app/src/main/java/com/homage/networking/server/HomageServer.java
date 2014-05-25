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

        - fetchStories :

 */
package com.homage.networking.server;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.device.Device;
import com.homage.networking.parsers.RemakeParser;
import com.homage.networking.parsers.StoriesParser;
import com.homage.networking.parsers.UserParser;

import java.util.ArrayList;
import java.util.HashMap;

public class HomageServer extends Server {
    String TAG = "TAG_"+getClass().getName();

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

    }
    //endregion

    public void loginUser(String email, String password) {
        Log.v(TAG, String.format("Login user with email: %s", email));

        HashMap<String, String> parameters = new HashMap<String, String>();

        // TODO: finish implementing this
        parameters.put("email", email);
        parameters.put("password",password);
        parameters.put("is_public","1");
        parameters.put("device[identifier_for_vendor]",
                Settings.Secure.getString(HomageApplication.getContext().getContentResolver(),Settings.Secure.ANDROID_ID)
        );
        parameters.put("device[model]", Device.getDeviceModel());
        parameters.put("device[name]","Android Phone");
        parameters.put("device[push_token]","%3C0a7b7b15%203f0fb335%200e252182%2038675505%20e739547b%2047dd8ab2%20");
        super.POST(R.string.url_new_user, parameters, INTENT_USER_CREATION, null, new UserParser());
    }

    /**
     * Refetches all stories (and their child objects) info from the server.
     */
    public void refetchStories() {
        Log.v(TAG, "Refetching stories");
        super.GET(R.string.url_stories, null, INTENT_STORIES, null, new StoriesParser());
    }

    /**
     *
     * @param storyOID the story oid the remake will be of
     * @param userOID the user that will do the remake
     */
    public void createRemake(String storyOID, String userOID) {
        Log.v(TAG, String.format("Create remake for story OID: %s user OID: %s", storyOID, userOID));

        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("story_id", storyOID);
        parameters.put("user_id", userOID);
        super.POST(R.string.url_new_remake, parameters, INTENT_REMAKE_CREATION, null, new RemakeParser());
    }
}
