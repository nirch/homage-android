package com.homage.networking.analytics;



import android.content.Context;
import android.util.Log;

import com.homage.app.R;
import com.homage.model.User;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class HMixPanel {
    String TAG = "TAG_" + getClass().getName();

    public static final String MIXPANEL_TOKEN = "7d575048f24cb2424cd5c9799bbb49b1";
    public MixpanelAPI mMixpanel;


    private Context context;

    //region *** singleton pattern ***
    public void init(Context context) {
        Log.d(TAG, String.format("Initializing mixpanel with token: %s", MIXPANEL_TOKEN));

        // A reference to the context
        this.context = context;

        // Do your initializations here.
        boolean isProductionServer = context.getResources().getBoolean(R.bool.is_production_server);
        if (isProductionServer) {
            mMixpanel = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
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
