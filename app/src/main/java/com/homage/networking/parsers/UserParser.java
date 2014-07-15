package com.homage.networking.parsers;

import android.util.Log;

import com.homage.model.Story;
import com.homage.model.User;

import org.json.JSONException;
import org.json.JSONObject;

public class UserParser extends Parser {
    String TAG = "TAG_"+getClass().getName();

    public boolean loginParsedUser = false;

     /**
     *
     * Example For a user object.
     *
        {
          "created_at": "2014-05-25 03:44:06 UTC",
          "first_use": false,
          "devices": [
            {
              "identifier_for_vendor": "D6F26923-9321-4A3A-9C3D-D189F6437F97",
              "model": "iPhone",
              "name": "Aviv%27s%20iPhone",
              "push_token": "%3C0a7b7b15%203f0fb335%200e252182%2038675505%20e739547b%2047dd8ab2%20"
            }
          ],
          "_id": {
            "$oid": "5381670670b35d082f00000b"
          },
          "email": "android%40test.com",
          "is_public": true,
          "password_hash": "2df39b5e6cee8fa0c8215dc5ceed89ad125f8b29cb10d6815160ccc1183fa03fc0f06685832934bcbd8061db7a4ef165fb8504d3591f4736e42121de8fc62c93BUAUTwogtrKUerK0ysogSOlYb45VAuFPyithwwzA4vZthqztJDFPjFfnGg4oY4Dd"
        }
     */

    @Override
    public void parse() throws JSONException {
        JSONObject userInfo = (JSONObject)objectToParse;
        String oid = Parser.parseOID(userInfo);

        String email = parseString("email", null);

        if (email != null) {
            Log.v(TAG, String.format("Parsing a user with email:%s oid:%s", userInfo.getString("email"), oid));
        } else {
            Log.v(TAG, String.format("Parsing guest user oid:%s", oid));
        }


        User user = User.findOrCreate(Parser.parseOID(userInfo));
        user.email =        email;
        user.isFirstUse =   parseBool("first_use", false);
        user.isPublic =     parseBool("is_public", false);
        user.createAt =     parseDateAsTimestamp("created_at", -1);

        // Login the user, if the parser was set to login the user after finished parsing.
        // Will also mark all other users in local storage as logged out.
        if (loginParsedUser) user.login();
        responseInfo.put("userOID", oid);

        user.save();
    }
}