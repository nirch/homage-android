/**
 * Model Entity: Story
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.HashMap;
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
    public int level;
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

    public Scene findSceneOrCreate(int sceneID) {
        Scene scene = findScene(sceneID);
        if (scene != null) return scene;
        return new Scene(this, sceneID);
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
