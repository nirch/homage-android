/**
 * Model Entity: Scene
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;


public class Scene extends SugarRecord<Scene> {
    //region *** Fields ***
    int sceneID;
    public String context;
    public int duration;
    public boolean isSelfie;
    public String script;
    public String silhouetteURL;
    public String thumbnailURL;
    public String videoURL;
    public double focusPointX;
    public double focusPointY;

    Story story;
    //endregion


    //region *** Factories ***
    public Scene(Context context) {
        super(context);
    }

    public Scene(Story story, int sceneID) {
        this(HomageApplication.getContext());
        this.story = story;
        this.sceneID = sceneID;
    }
    //endregion

    //region *** Logic ***
    //endregion
}