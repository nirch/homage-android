package com.homage.model;

import java.util.HashMap;
import java.util.Iterator;

public class MemCache {
    HashMap<String, Story> storiesByOID;
    HashMap<String, Scene> scenesByTag;

    public MemCache() {
        storiesByOID = new HashMap<String, Story>();
        scenesByTag = new HashMap<String, Scene>();
    }

    //region *** singleton ***
    private static MemCache instance = new MemCache();
    public static MemCache sharedInstance() {
        if(instance == null) instance = new MemCache();
        return instance;
    }
    public static MemCache sh() {
        return MemCache.sharedInstance();
    }
    //endregion

    //region *** stories cache ***
    public void refreshStories() {
        // Cache all stories info in memory
        Iterator<Story> stories = Story.findAll(Story.class);

        while (stories.hasNext()) {
            Story story = stories.next();
            putStory(story);
        }
    }

    public void persistStories() {
        // Bulk save of all stories in a single transaction.
        Story.saveInTx(storiesByOID.values());
    }

    public Story getStoryByOID(String oid) {
        return storiesByOID.get(oid);
    }

    public void putStory(Story story) {
        storiesByOID.put(story.getOID(), story);
    }
    //endregion

    //region *** scenes cache ***
    public void refreshScenes() {
        // Cache all scenes info in memory
        Iterator<Scene> scenes = Scene.findAll(Scene.class);
        while (scenes.hasNext()) {
            Scene scene = scenes.next();
            putScene(scene);
        }
    }

    public void persistScenes() {
        // Bulk save of all stories in a single transaction.
        Story.saveInTx(scenesByTag.values());
    }

    public void putScene(Scene scene) {
        scenesByTag.put(scene.tag, scene);
    }

    public Scene getSceneByTag(String tag) {
        return scenesByTag.get(tag);
    }
    //endregion
}
