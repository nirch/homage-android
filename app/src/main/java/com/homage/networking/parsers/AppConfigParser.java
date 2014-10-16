package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.User;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yoavcaspin on 10/15/14.
 */
public class AppConfigParser extends Parser {
      String TAG = "TAG_"+getClass().getName();

    @Override
    public void parse() throws JSONException {
        JSONObject userInfo = (JSONObject) objectToParse;
        String share_link_prefix = parseString(userInfo, "share_link_prefix", "");
        responseInfo.put("share_link_prefix", share_link_prefix);
    }

}
