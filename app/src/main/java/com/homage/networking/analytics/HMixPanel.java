package com.homage.networking.analytics;



import android.content.Context;
import android.util.Log;

import com.homage.app.R;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class HMixPanel {
    String TAG = "TAG_" + getClass().getName();

    public static String MIXPANEL_TOKEN;
    public MixpanelAPI mMixpanel;


    private Context context;

    //region *** singleton pattern ***
    public void init(Context context) {
        // A reference to the context
        this.context = context;
        MIXPANEL_TOKEN = context.getResources().getString(R.string.mixpanel_token);


        // Will initialize mixpanel in one of two scenarios
        // 1. App configured to work with production servers
        // 2. App configured to work with test servers and configured not to block mix panel events (blocked by default)
        if (!HomageServer.sh().shouldBlockMPAnalytics()) {
            // Should initialize mixpanel analytics.
            mMixpanel = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
            Log.d(TAG, String.format("Initializing mixpanel with token: %s", MIXPANEL_TOKEN));
        } else {
            // Should block mix panel analytics.
            Log.d(TAG, "Configured not to initialize mixpanel");
        }
    }

    private static HMixPanel instance = new HMixPanel();

    public static HMixPanel sharedInstance() {
        if (instance == null) instance = new HMixPanel();
        return instance;
    }

    public static HMixPanel sh() {
        return HMixPanel.sharedInstance();
    }
    //endregion

    //region *** Mixpanel helper methods ... ***
    public void track(String eventName, HashMap<String, String> mappedProps) {
        try {
            JSONObject props = new JSONObject();

            // Iterate the mapped props and build the JSONObject

            if (mappedProps != null) {
                for (String k : mappedProps.keySet()) {
                    String v = mappedProps.get(k);
                    props.put(k, v);
                }
            }

            // Send the tracking event using mixpanel.
            if (mMixpanel != null) mMixpanel.track(eventName, props);
        } catch (JSONException e) {
            Log.e(TAG, "Mixpanel tracking JSON error.", e);
        } catch (Exception e) {
            Log.e(TAG, "Mixpanel tracking general error.", e);
        }
    }

    public void registerSuperProperties(HashMap<String, String> mappedProps) {
        try {
            JSONObject props = new JSONObject();

            // Iterate the mapped props and build the JSONObject
            for (String k : mappedProps.keySet()) {
                String v = mappedProps.get(k);
                props.put(k, v);
            }

            // Send the tracking event using mixpanel.
            if (mMixpanel != null) mMixpanel.registerSuperProperties(props);


        } catch (JSONException e) {
            Log.e(TAG, "Mixpanel registerSuperProperties JSON error.", e);
        } catch (Exception e) {
            Log.e(TAG, "Mixpanel registerSuperProperties general error.", e);
        }
    }

    public void flush() {
        if (instance != null && mMixpanel != null) mMixpanel.flush();
    }

    public void identify(String userID)
    {
        if (mMixpanel != null) mMixpanel.identify(userID);
    }

    public void setPeople(HashMap<String,String> mappedProps)
    {
        try {
            JSONObject props = new JSONObject();

            // Iterate the mapped props and build the JSONObject
            for (String k : mappedProps.keySet()) {
                String v = mappedProps.get(k);
                props.put(k, v);
            }

            if (mMixpanel != null) mMixpanel.getPeople().set(props);

        } catch (JSONException e) {
            Log.e(TAG, "Mixpanel set people JSON error.", e);
        } catch (Exception e) {
            Log.e(TAG, "Mixpanel set people general error.", e);
        }
    }

    public void createAliasForUser(User user, String alias)
    {
        if (mMixpanel != null) mMixpanel.alias(alias,null);
    }
    //endregion

}
