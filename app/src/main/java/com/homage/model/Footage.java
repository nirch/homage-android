/**
 * Model Entity: Remake
 */
package com.homage.model;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.homage.app.main.HomageApplication;
import com.orm.SugarDb;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

public class Footage extends SugarRecord<Remake> {

    //region *** Fields ***
    public String processedVideoS3Key;
    public String rawLocalFile;
    public String rawVideoS3Key;
    public int sceneID;
    public int status;
    public String takeId;
    public String rawUploadedFile;
    public boolean currentlyUploaded;
    public int uploadsFailedCounter;
    //endregion

    //region *** Relationships ***
    public Remake remake;
    public User user;
    //endregion

    @Ignore
    static public enum Status {
        OPEN(0),
        UPLOADING(1),
        PROCESSING(2),
        READY(3);

        private final int value;

        private Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    @Ignore
    public final static int NOT_FOUND = -1;

    @Ignore
    static public enum ReadyState {
        READY_FOR_FIRST_RETAKE,
        READY_FOR_SECOND_RETAKE,
        STILL_LOCKED,
        STILL_UNKNOWN
    }
    //endregion


    //region *** Factories ***
    public Footage(Context context) {
        super(context);
    }

    public Footage(Remake remake, int sceneID) {
        this(HomageApplication.getContext());
        this.remake = remake;
        this.sceneID = sceneID;
        this.user = remake.user;
    }

    public static List<Footage> findPendingFootagesForUser(User user) {
        List<Footage> footages = Footage.find(Footage.class, "user=? AND raw_local_file!=? AND status=?", user.getId().toString(), "null", "0");
          return footages;
    }
    //endregion
}

