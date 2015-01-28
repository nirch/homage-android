/**
 * Model Entity: User
 */
package com.homage.model;

import android.content.Context;
import android.util.Log;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

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

    public String firstName;
    public String lastName;
    public String birthDay;
    public String link;
    public String fbId;

    public long createAt;
    public long lastTimeUpdatedStories;
    //endregion

    @Ignore
    static private User currentUser = null;

    //region *** Factories ***
    public String getOID() {
        return this.oid;
    }

    public User() {
        super();
    }

    public User(String oid) {
        this();
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

    public static User findByEmail(String email) {
        if (email==null) return null;
        List<User> res = User.find(User.class, "email=?", email);
        if (res.size()==1) return res.get(0);
        return null;
    }
    //endregion


    //region *** Logic ***
    public static void logoutAllUsers() {
        List<User> res = User.find(User.class, "is_logged_in = ?", "1");
        for (User user : res) {
            user.isLoggedIn = false;
            user.save();
        }
        currentUser = null;
    }

    public static User getCurrent() {
        if (currentUser != null) return currentUser;
        List<User> res = User.find(User.class, "is_logged_in = ?", "1");
        if (res.size()<1) return null;
        currentUser = res.get(0);
        return currentUser;
    }

    public static User getCurrent(boolean refreshCache) {
        if (refreshCache) currentUser = null;
        return getCurrent();
    }

    public static void cleanDeprecatedLocalUsersForUser(User user) {
    }

    public void login() {
        User.logoutAllUsers();
        isLoggedIn = true;
        currentUser = this;
    }

    public Remake unfinishedRemakeForStory(Story story) {
        // Query for an unfinished remake.
        List<Remake> unfinishedRemakes = Remake.find(
                Remake.class,
                "user = ? AND story=? AND (status=? OR status=? OR status=?)",
                getId().toString(),
                story.getId().toString(),
                String.valueOf(Remake.Status.NEW.getValue()),
                String.valueOf(Remake.Status.IN_PROGRESS.getValue()),
                String.valueOf(Remake.Status.TIMEOUT.getValue())
                );
        if (unfinishedRemakes.size()>0) return unfinishedRemakes.get(0);
        return null;
    }

    public void deleteAllUnfinishedRemakesForStory(Story story) {
        // Query for an unfinished remake.
        List<Remake> unfinishedRemakes = Remake.find(
                Remake.class,
                "user = ? AND story=? AND (status=? or status=? or status=?)",
                getId().toString(),
                story.getId().toString(),
                String.valueOf(Remake.Status.NEW.getValue()),
                String.valueOf(Remake.Status.IN_PROGRESS.getValue()),
                String.valueOf(Remake.Status.TIMEOUT.getValue())
        );
        if (unfinishedRemakes.size()==0) return;

        for (Remake remake : unfinishedRemakes) {
            remake.delete();
        }
    }

    public List<Remake> allAvailableRemakes() {
        // Query for ready made remakes
        List<Remake> remakes = Remake.find(
                Remake.class,
                "user=? AND (status=? OR status=? OR status=?)",
                getId().toString(),
                String.valueOf(Remake.Status.IN_PROGRESS.getValue()),
                String.valueOf(Remake.Status.DONE.getValue()),
                String.valueOf(Remake.Status.TIMEOUT.getValue())
        );
        return remakes;
    }

    public List<Remake> allAvailableRemakesLatestOnTop() {
        // Query for ready made remakes
        List<Remake> remakes = Remake.find(
                Remake.class,            // Entity class
                "user=? AND (status=? OR status=? OR status=?)",               // where clause
                new String[]{
                    getId().toString(),
                    String.valueOf(Remake.Status.IN_PROGRESS.getValue()),
                    String.valueOf(Remake.Status.DONE.getValue()),
                    String.valueOf(Remake.Status.TIMEOUT.getValue())
                },                                                             // where args
                "",                                                            // group by
                "created_at DESC,status DESC",                                 // order by
                "");                                                           // limit
        return remakes;
    }

    public List<Remake> topTenAvailableRemakesLatestOnTop() {
        // Query for ready made remakes
        List<Remake> remakes = Remake.find(
                Remake.class,            // Entity class
                "user=? AND (status=? OR status=? OR status=?)",               // where clause
                new String[]{
                        getId().toString(),
                        String.valueOf(Remake.Status.IN_PROGRESS.getValue()),
                        String.valueOf(Remake.Status.DONE.getValue()),
                        String.valueOf(Remake.Status.TIMEOUT.getValue())
                },                                                             // where args
                "",                                                            // group by
                "created_at DESC,status DESC",                                 // order by
                "10");                                                         // limit
        return remakes;
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

    public boolean isFacebookUser() {
        return (fbId != null && fbId.length()>0);
    }

    public String getFacebookProfilePictureURL() {
        return String.format("http://graph.facebook.com/%s/picture", fbId);
    }
    //endregion
}