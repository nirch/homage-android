package com.homage.networking.uploader;

import android.content.Context;
import android.util.Log;

import com.homage.model.Footage;
import com.homage.model.User;

import java.util.List;

public class UploadManager {
    String TAG = "TAG_"+getClass().getName();

    //region *** singleton pattern ***
    private static UploadManager instance = new UploadManager();
    public static UploadManager sharedInstance() {
        if(instance == null) instance = new UploadManager();
        return instance;
    }
    public static UploadManager sh() {
        return UploadManager.sharedInstance();
    }
    //endregion

    public void checkForPendingUploads() {
        User user = User.getCurrent();
        if (user == null) return;

        //
        // Bring all footages for this user, that have a rawLocalFile that is not the same as the uploaded file
        // (can happen on retakes of scenes already uploaded or if a raw file was never successfuly uploaded for this footage)
        //
        List<Footage> pendingFootages = Footage.findPendingFootagesForUser(user);
        if (pendingFootages.size()==0) {
            Log.d(TAG, "Uploader didn't find any new footages to upload...");
            return;
        }
        Log.d(TAG, String.format("Footages to upload count: %d", pendingFootages.size()));
    }


}
