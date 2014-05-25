/**
 * Model Entity: Story
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;

import java.util.List;

public class Story extends SugarRecord<Story> {
    String oid;

    //region *** Fields ***
    public String description;
    public String name;
    public boolean active;
    public int thumbnailRip;
    public int orderId;
    public String video;
    public int remakesNum;
    public String thumbnail;
    public int level;
    //endregion

    //region *** Factories ***
    public Story(Context context) {
        super(context);
    }

    public String getOID() {
        return this.oid;
    }

    public Story(String oid){
        this(HomageApplication.getContext());
        this.oid = oid;
    }

    public static Story findOrCreate(String oid) {
        Story story = Story.findByOID(oid);
        if (story != null) return story;
        return new Story(oid);
    }

    public static Story findByOID(String oid) {
        List<Story> res = Story.find(Story.class, "oid = ?", oid);
        if (res.size()==1) return res.get(0);
        return null;
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
    //endregion

    //region *** Logic ***
    //endregion
}
