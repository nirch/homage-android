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
    //endregion

    //region *** Logic ***
    //endregion
}