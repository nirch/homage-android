/**
 * Model Entity: User
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;

import java.util.Date;
import java.util.List;

public class User extends SugarRecord<User> {

    //region *** Fields ***
    String oid;
    public String email;
    public boolean isFirstUse;
    public boolean isLoggedIn;
    public boolean isPublic;
    public boolean prefersToSeeScriptWhileRecording;
    public boolean skipRecorderTutorial;
    public String userId;
    public String fbId;
    public String firstName;
    public long createAt;
    public long lastTimeUpdatedStories;
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
        List<User> res = User.find(User.class, "oid=?", oid);
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

    public Remake unfinishedRemakeForStory(Story story) {
        // Query for an unfinished remake.
        List<Remake> res = Remake.find(
                Remake.class,
                "user = ? and story = ?",
                getId().toString(),
                story.getId().toString());
        if (res.size()>0) return res.get(0);

        /*
         and (status = ? or status = ? or status = ?)

                         Remake.Status.NEW.toString(),
                Remake.Status.IN_PROGRESS.toString(),
                Remake.Status.TIMEOUT.toString()
         */

        return null;
    }

    public long timePassedSinceUpdatedStories() {
        return Integer.MAX_VALUE;
        /*
        if (lastTimeUpdatedStories == null) return Integer.MAX_VALUE;
        return new Date().getTime() - lastTimeUpdatedStories.getTime();
        */
    }

    public String getTag() {
        if (firstName != null) return firstName;
        if (email != null) {
            return email.split("@")[0];
        }
        return "Guest";
    }

    public boolean isGuest() {
        if (email == null) return true;
        return false;
    }
    //endregion
}