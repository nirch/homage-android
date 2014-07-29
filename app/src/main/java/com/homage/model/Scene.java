/**
 * Model Entity: Scene
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;


public class Scene extends SugarRecord<Scene> {

    //region *** Fields ***
    int sceneID;
    public String tag;
    public String context;
    public int duration;
    public boolean isSelfie;
    public String script;
    public String silhouetteURL;
    public String thumbnailURL;
    public String videoURL;
    public double focusPointX;
    public double focusPointY;
    //endregion

    //region *** Relationships ***
    Story story;
    //endregion

    @Ignore
    static NumberFormat nf;

    static {
        nf = new DecimalFormat();
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        nf.setRoundingMode(RoundingMode.HALF_UP);
    }

    //region *** Factories ***
    public Scene() {
        super();
    }

    public Scene(Story story, int sceneID) {
        this();
        this.story = story;
        this.sceneID = sceneID;
    }

    public int getSceneID() {
        return sceneID;
    }

    public String getTitle() {
        return String.format("SCENE %d", sceneID);
    }

    public String getTimeString() {
        String secondsRounded = nf.format(duration/1000.0f);
        return String.format("%s SEC", secondsRounded);
    }

    public static Scene findOrCreate(Story story, int sceneID) {
        Scene scene = story.findScene(sceneID);
        if (scene != null) return scene;
        return new Scene(story, sceneID);
    }

    public static Scene findOrCreate(Story story, int sceneID, boolean useMemCache) {
        // If not using memory cache, just use the regular findOrCreate method.
        if (!useMemCache) return findOrCreate(story, sceneID);

        // If using the memory cache, check if the object is in the cache first.
        MemCache cache = MemCache.sh();
        String tag = Scene.sTag(story.getOID(), sceneID);
        Scene scene = cache.getSceneByTag(tag);
        if (scene != null) return scene;

        // Not found in mem cache.
        // Find or create using local storage.
        // And cache the object in memory.
        scene = findOrCreate(story, sceneID);
        scene.tag = tag;
        cache.putScene(scene);

        // Return story
        return scene;
    }
    //endregion

    //region *** Logic ***
    static public String sTag(String storyOID, long sceneID) {
        return String.format("%s_%d", storyOID, sceneID);
    }
    //endregion
}