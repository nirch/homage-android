package com.homage.networking.analytics;



import android.content.Context;
import android.util.Log;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class HMixPanel {
    String TAG = "TAG_"+getClass().getName();

    public static final String MIXPANEL_TOKEN = "7d575048f24cb2424cd5c9799bbb49b1";
    public MixpanelAPI mMixpanel;


    private Context context;

    //region *** singleton pattern ***
    public void init(Context context) {
        Log.d(TAG, String.format("Initializing mixpanel with token: %s", MIXPANEL_TOKEN));

        // A reference to the context
        this.context = context;

        // Do your initializations here.
        mMixpanel = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
    }

    private static HMixPanel instance = new HMixPanel();
    public static HMixPanel sharedInstance() {
        if(instance == null) instance = new HMixPanel();
        return instance;
    }
    public static HMixPanel sh() {
        return HMixPanel.sharedInstance();
    }
    //endregion

    //region *** Mixpanel helper methods ... ***
    public void track(String eventName, HashMap<String, String> mappedProps) {
        try
        {
            JSONObject props = new JSONObject();

            // Iterate the mapped props and build the JSONObject
            for (String k : mappedProps.keySet()) {
                String v = mappedProps.get(k);
                props.put(k, v);
            }

            // Send the tracking event using mixpanel.
            mMixpanel.track(eventName, props);


        } catch (JSONException e) {
            Log.e(TAG, "Mixpanel tracking JSON error.", e);
        } catch (Exception e) {
            Log.e(TAG, "Mixpanel tracking general error.", e);
        }
    }
    //endregion

    public void flush () {
        if (instance != null) mMixpanel.flush();
    }

}
