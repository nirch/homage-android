/**
 * Model Entity: Remake
 */
package com.homage.model;

import android.content.Context;

import com.homage.app.main.HomageApplication;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.Date;
import java.util.List;

public class Remake extends SugarRecord<Remake> {
    String oid;

    //region *** Fields ***
    public int grade;
    public String lastLocalUpdate;
    public String shareURL;
    public int status;
    public String thumbnailURL;
    public String videoURL;
    public Date createdAt;
    public boolean stillPublic;

    public Story story;
    public User user;

    @Ignore
    static public enum Status {
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

    //endregion


    //region *** Logic ***
    //endregion
}