/**
 * Model Entity: Story
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

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
    //endregion


    //region *** Logic ***
    //endregion
}
