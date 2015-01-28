/**
 * Model Entity: Story
 */
package com.homage.model;

import com.orm.SugarRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Story extends SugarRecord<Story> {

    //region *** Fields ***
    String oid;
    public String description;
    public String name;
    public int active;
    public int thumbnailRip;
    public int orderId;
    public String video;
    public int remakesNum;
    public String thumbnail;
    public String shareMessage;
    public int level;
    public int sharingVideoAllowed;
    //endregion

    //region *** Factories ***
    public Story() {
        super();
    }

    public String getOID() {
        return this.oid;
    }

    public Story(String oid){
        this();
        this.oid = oid;
    }

    public static Story findOrCreate(String oid) {
        Story story = Story.findByOID(oid);
        if (story!=null) return story;
        return new Story(oid);
    }

    public static Story findOrCreate(String oid, boolean useMemCache) {
        // If not using memory cache, just use the regular findOrCreate method.
        if (!useMemCache) return findOrCreate(oid);

        // If using the memory cache, check if the object is in the cache first.
        MemCache cache = MemCache.sh();
        Story story = cache.getStoryByOID(oid);
        if (story != null) return story;

        // Not found in mem cache.
        // Find or create using local storage.
        // And cache the object in memory.
        story = findOrCreate(oid);
        cache.putStory(story);

        // Return story
        return story;
    }

    public static Story findByOID(String oid) {
        List<Story> res = Story.find(Story.class, "oid=?", oid);
        if (res.size()==1) return res.get(0);
        return null;
    }

    public static List<Story> allActiveStories() {
        List<Story> res = Story.find(
                Story.class,            // Entity class
                "active=?",             // where clause
                new String[]{"1"},      // where args
                "",                     // group by
                "order_id",             // order by
                "");                    // limit
        return res;
    }

    public Scene findScene(int sceneID) {
        List<Scene> res = Scene.find(
                Scene.class,
                "story = ? and scene_id = ?",
                getId().toString(),
                String.valueOf(sceneID));
        if (res.size() > 0) return res.get(0);
        return null;
    }

    public List<Scene> getScenesOrdered() {
        List<Scene> res = Scene.find(
                Scene.class,
                "story = ?",
                new String[]{getId().toString()},
                "",
                "scene_id",
                "");
        res = validatedScenesList(res);
        return res;
    }

    public List<Scene> getScenesReversedOrder() {
        List<Scene> res = Scene.find(
                Scene.class,
                "story = ?",
                new String[]{getId().toString()},
                "",
                "scene_id DESC",
                "");
        res = validatedScenesList(res);
        return res;
    }

    public List<Remake> getRemakes() {
        List<Remake> res = Remake.find(
                Remake.class,
                "story = ?",
                this.getId().toString()
        );
        return res;
    }

    public List<Remake> getRemakes(User excludedUser) {
        List<Remake> res = Remake.find(
                Remake.class,                                                               // Entity class
                "story=? AND user<>? AND grade<>? AND still_public=?",                                         // where clause
                new String[]{getId().toString(), excludedUser.getId().toString(), "-1", "1"},    // where args
                "",                                                                         // group by
                "grade DESC",                                                               // order by
                "");
        return res;
    }

    public boolean deleteRemakes(User excludedUser, long pulledAt) {
        Remake.deleteAll(Remake.class,                                                               // Entity class
                "story=? AND user<>? AND grade<>? AND pulled_at<>?",                                         // where clause
                new String[]{getId().toString(), excludedUser.getId().toString(), "-1", String.valueOf(pulledAt)});
        return true;
    }
    //endregion

    //region *** Logic ***
    private List<Scene> validatedScenesList(List<Scene> scenes) {
        ArrayList<Scene> validatedScenes = new ArrayList<Scene>();
        HashMap<Integer, Boolean>m = new HashMap<Integer, Boolean>();

        // Ensure no duplicate scenes in list
        for (Scene scene : scenes) {
            if (!m.containsKey(scene.sceneID)) {
                m.put(scene.sceneID, true);
                validatedScenes.add(scene);
            }
        }
        return validatedScenes;
    }

    //endregion
}
