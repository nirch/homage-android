/**
 * Model Entity: Remake
 */
package com.homage.model;

import android.content.Context;
import android.util.Log;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Remake extends SugarRecord<Remake> {

    //region *** Fields ***
    String oid;
    public int grade;
    public String lastLocalUpdate;
    public String shareURL;
    public int status;
    public String thumbnailURL;
    public String videoURL;
    public long createdAt;
    public boolean stillPublic;
    //endregion

    //region *** Relationships ***
    Story story;
    User user;
    //endregion


    @Ignore
    static public final String DEFAULT_RENDER_OUTPUT_HEIGHT = "360";

    @Ignore
    static public enum Status {
        NEW(0),
        IN_PROGRESS(1),
        RENDERING(2),
        DONE(3),
        TIMEOUT(4),
        DELETED(5);

        private final int value;

        private Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }
    //endregion

    //region *** Factories ***
    public String getOID() {
        return this.oid;
    }

    public Remake() {
        super();
    }

    public Remake(String oid, Story story, User user) {
        this();
        this.oid = oid;
        this.story = story;
        this.user = user;
    }

    public static Remake findOrCreate(String oid, Story story, User user) {
        Remake remake = Remake.findByOID(oid);
        if (remake != null) return remake;
        return new Remake(oid, story, user);
    }

    public static Remake findByOID(String oid) {
        List<Remake> res = Remake.find(Remake.class, "oid = ?", oid);
        if (res.size() == 1) return res.get(0);
        return null;
    }

    public Footage findFootageOrCreate(int sceneID) {
        Footage footage = findFootage(sceneID);
        if (footage != null) return footage;
        return new Footage(this, sceneID);
    }

    public Footage findFootage(int sceneID) {
        List<Footage> res = Footage.find(
                Footage.class,
                "remake = ? and scene_id = ?",
                getId().toString(),
                String.valueOf(sceneID));
        if (res.size() > 0) return res.get(0);
        return null;
    }

    public Story getStory() {
        return Story.findById(Story.class, this.story.getId());
    }
    //endregion


    //region *** Logic ***
    public int nextReadyForFirstRetakeSceneID() {
        List<Footage.ReadyState> readyStates = footagesReadyStates();
        for (int i=0;i<readyStates.size();i++) {
            Footage.ReadyState state = readyStates.get(i);
            if (state == Footage.ReadyState.READY_FOR_FIRST_RETAKE) {
                Scene scene = getStory().getScenesOrdered().get(i);
                return scene.sceneID;
            }
        }
        return Footage.NOT_FOUND;
    }

    public int lastSceneID() {
        List<Scene> scenes = getStory().getScenesOrdered();
        Scene scene = scenes.get(scenes.size()-1);
        return scene.sceneID;
    }

    public List<Footage> getFootagesOrdered() {
        List<Footage> res = Footage.find(
                Footage.class,
                "remake = ?",
                new String[]{getId().toString()},
                "",
                "scene_id",
                "");
        return res;
    }

    public List<Footage.ReadyState> footagesReadyStates() {
        List<Footage.ReadyState> states = new ArrayList<Footage.ReadyState>();
        Footage.ReadyState readyState = Footage.ReadyState.READY_FOR_FIRST_RETAKE;
        List<Footage> footages = getFootagesOrdered();
        for (Footage footage : footages) {
            if (footage.rawLocalFile == null && footage.status == Footage.Status.OPEN.getValue()) {
                states.add(readyState);
                readyState = Footage.ReadyState.STILL_LOCKED;
            } else {
                states.add(Footage.ReadyState.READY_FOR_SECOND_RETAKE);
            }
        }
        return states;
    }

    public List<Footage.ReadyState> footagesReadyStatesReversedOrder() {
        List<Footage.ReadyState> states = footagesReadyStates();
        Collections.reverse(states);
        return states;
    }

    public boolean allScenesTaken() {
        int nextReadySceneID = nextReadyForFirstRetakeSceneID();
        if (nextReadySceneID == Footage.NOT_FOUND) return true;
        return false;
    }
     //endregion
}