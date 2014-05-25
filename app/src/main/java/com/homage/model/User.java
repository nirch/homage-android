/**
 * Model Entity: User
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;

import java.util.List;

public class User extends SugarRecord<User> {
    String oid;

    
    //region *** Fields ***
    public String email;
    public Boolean isFirstUse;
    public Boolean isLoggedIn;
    public Boolean isPublic;
    public Boolean prefersToSeeScriptWhileRecording;
    public Boolean skipRecorderTutorial;
    public String userId;
    public String fbId;
    public String firstName;
    //endregion


    //region *** Factories ***
    public String getOID() {
        return this.oid;
    }

    public User(Context context) {
        super(context);
    }

    public User(String oid) {
        this(HomageApplication.getContext());
        this.oid = oid;
    }

    public static User findOrCreate(String oid) {
        User user = User.findByOID(oid);
        if (user != null) return user;
        return new User(oid);
    }

    public static User findByOID(String oid) {
        List<User> res = User.find(User.class, "oid = ?", "true");
        if (res.size()==1) return res.get(0);
        return null;
    }
    //endregion


    //region *** Logic ***
    public static void logoutAllUsers() {
        List<User> res = User.find(User.class, "is_logged_in = ?", "true");
        for (User user : res) {
            user.isLoggedIn = false;
            user.save();
        }
    }

    public static User getCurrent() {
        List<User> res = User.find(User.class, "is_logged_in = ?", "true");
        if (res.size()>0) return res.get(0);
        return null;
    }

    public void login() {
        User.logoutAllUsers();
        isLoggedIn = true;
    }
    //endregion
}