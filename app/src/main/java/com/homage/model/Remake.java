/**
 * Model Entity: Remake
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;

import java.util.Date;
import java.util.List;

public class Remake extends SugarRecord<Remake> {
    String oid;


    //region *** Fields ***
    Integer grade;
    Date lastLocalUpdate;
    String shareURL;
    Integer status;
    String thumbnailURL;
    String videoURL;
    Date createdAt;
    Boolean stillPublic;
    Story story;
    User user;

    static private enum Status {
        NEW,
        IN_PROGRESS,
        RENDERING,
        DONE,
        TIMEOUT,
        DELETED
    }
    //endregion


    //region *** Factories ***
    public String getOID() {
        return this.oid;
    }

    public Remake(Context context) {
        super(context);
    }

    public Remake(String oid) {
        this(HomageApplication.getContext());
    }

    public static Remake findOrCreate(String oid) {
        Remake remake = Remake.findByOID(oid);
        if (remake != null) return remake;
        return new Remake(oid);
    }

    public static Remake findByOID(String oid) {
        List<Remake> res = Remake.find(Remake.class, "oid = ?", oid);
        if (res.size() == 1) return res.get(0);
        return null;
    }
    //endregion


    //region *** Logic ***
    //endregion
}