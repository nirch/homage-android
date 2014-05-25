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

public class Footage extends SugarRecord<Remake> {
    String oid;

    //region *** Fields ***
    public Date lastUploadAttemptTime;
    public String processedVideoS3Key;
    public String rawLocalFile;
    public String rawVideoS3Key;
    public int sceneID;
    public int status;
    public String rawUploadedFile;
    public boolean currentlyUploaded;
    public int uploadsFailedCounter;

    public Remake remake;

    static public enum Status {
        OPEN,
        UPLOADING,
        PROCESSING,
        READY
    }

    static public enum ReadyState {
        READY_FOR_FIRST_RETAKE,
        READY_FOR_SECOND_RETAKE,
        STILL_LOCKED,
        STILL_UNKNOWN
    }
    //endregion


    //region *** Factories ***
    public String getOID() {
        return this.oid;
    }

    public Footage(Context context) {
        super(context);
    }

    public Footage(Remake remake, int sceneID) {
        this(HomageApplication.getContext());
        this.remake = remake;
        this.sceneID = sceneID;
    }
    //endregion


    //region *** Logic ***
    //endregion
}