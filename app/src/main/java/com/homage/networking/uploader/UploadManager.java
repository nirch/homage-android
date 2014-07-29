package com.homage.networking.uploader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.homage.app.main.HomageApplication;
import com.homage.app.main.SettingsActivity;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

public class UploadManager {
    String TAG = "TAG_"+getClass().getName();

    static final int MAX_WORKERS = 5;

    private Stack<UploadWorker> idleWorkersPool;
    private HashMap<String, UploadWorker> busyWorkersByJobID;
    private HashMap<String, UploadWorker> busyWorkersByRawLocalFile;

    //region *** singleton pattern ***
    private static UploadManager instance;
    public static UploadManager sharedInstance() {
        if (instance == null) {
            instance = new UploadManager();
            instance.initialize();
        }
        return instance;
    }
    public static UploadManager sh() {
        return UploadManager.sharedInstance();
    }
    //endregion

    public void checkUploader() {
        SharedPreferences p = HomageApplication.getSettings(HomageApplication.getContext());
        boolean uploaderActive = p.getBoolean(SettingsActivity.UPLOADER_ACTIVE, true);
        if (uploaderActive) {
            Intent intent = UploaderService.cmd(HomageApplication.getContext(), UploaderService.CMD_CHECK_FOR_PENDING_UPLOADS);
            HomageApplication.getContext().startService(intent);
        }
    }

    private void initialize() {
        // Initialize the references to idle and busy workers.
        idleWorkersPool = new Stack<UploadWorker>();
        busyWorkersByJobID = new HashMap<String, UploadWorker>();
        busyWorkersByRawLocalFile = new HashMap<String, UploadWorker>();

        // Create idle workers and put them in the idle workers pool.
        for (int i=0;i<MAX_WORKERS;i++) {
            idleWorkersPool.push(new UploadS3Worker());
        }
        Log.d(TAG, String.format("Created %d upload workers.", idleWorkersPool.size()));
    }

    private String newJobID() {
        return UUID.randomUUID().toString();
    }

    public void checkForPendingUploads() {
        User user = User.getCurrent();
        if (user == null) return;

        //
        // Bring all footages for this user, that have a rawLocalFile that is not the same as the uploaded file
        // (can happen on retakes of scenes already uploaded or if a raw file was never successfuly uploaded for this footage)
        //
        List<Footage> pendingFootages = Footage.findPendingFootagesForUser(user);
        if (pendingFootages.size() == 0) {
            Log.d(TAG, "Uploader didn't find any new footages to upload...");
            return;
        }
        Log.d(TAG, String.format("Footages to upload count: %d", pendingFootages.size()));

        int newJobsCount = Math.min(idleWorkersPool.size(), pendingFootages.size());

        for (int i = 0; i < newJobsCount; i++) {
            // Get an idle worker and footage to upload
            UploadWorker worker = idleWorkersPool.pop();
            Footage footage = pendingFootages.get(i);
            Remake remake = footage.getRemake();

            // Put the worker to work.
            String newJobID = newJobID();
            worker.newJob(newJobID, footage.rawLocalFile, footage.rawVideoS3Key);

            // Store references for the worker by job ID and rawLocalFile.
            busyWorkersByJobID.put(newJobID, worker);
            busyWorkersByRawLocalFile.put(footage.rawLocalFile, worker);

            // Tell the worker to start uploading
            if (worker.startWorking()) {
                footage.currentlyUploaded = 1;
                footage.save();
//                HomageServer.sh().putFootage(
//                        remake.getOID(),
//                        footage.sceneID,
//                        footage.getTakeID(),
//                        null);
            }
        }
    }

    public void reportSourceFileChange(String oldSource, String newSource) {
        // Find if a worker is currently uploading the old source file
        if (!busyWorkersByRawLocalFile.containsKey(oldSource)) return;
        UploadWorker worker = busyWorkersByRawLocalFile.get(oldSource);
        worker.reportSourceFileChangedDuringUpload(newSource);
    }

    public void finishedUpload(UploadWorker worker) {
        String jobID = worker.getJobID();
        String source = worker.getSource();

        Footage footage = Footage.findFootageByRawLocalFile(source);
        if (footage==null) return;

        footage.rawUploadedFile = footage.rawLocalFile;
        footage.currentlyUploaded = 0;
        footage.save();

        putWorkerToRest(worker);

        // Tell server upload successful
        Remake remake = footage.getRemake();
        HomageServer.sh().updateFootageUploadSuccess(
                remake.getOID(),
                footage.sceneID,
                footage.getTakeID(),
                null
        );
    }

    public void finishedWithIrrelevantUpload(UploadWorker worker) {
        String jobID = worker.getJobID();
        String newSource = worker.getNewSource();
        if (newSource == null) return;

        Footage footage = Footage.findFootageByRawLocalFile(newSource);
        if (footage==null) return;
        footage.rawUploadedFile = null;
        footage.currentlyUploaded = 0;
        footage.save();
        putWorkerToRest(worker);

        checkForPendingUploads();
    }



    public void failedUpload(UploadWorker worker) {
        String jobID = worker.getJobID();
        String source = worker.getSource();

        Footage footage = Footage.findFootageByRawLocalFile(source);
        if (footage==null) return;
        footage.rawUploadedFile = null;
        footage.currentlyUploaded = 0;
        footage.save();
        putWorkerToRest(worker);

        checkForPendingUploads();
    }

//    public void cancelUploadOfRawLocalFile(String rawLocalFile) {
//        // Find a worker currently busy with uploading this file
//        if (!busyWorkersByRawLocalFile.containsKey(rawLocalFile)) return;
//
//        // Take the worker
//        UploadWorker worker = busyWorkersByRawLocalFile.get(rawLocalFile);
//
//        // Cancel the upload
//        worker.stopWorking();
//
//        //// put the worker to rest.
//        //putWorkerToRest(worker);
//    }

    private void putWorkerToRest(UploadWorker worker) {
        String jobID = worker.getJobID();
        String source = worker.getSource();
        busyWorkersByJobID.remove(jobID);
        busyWorkersByRawLocalFile.remove(source);
        worker.reset();
        idleWorkersPool.push(worker);
        Log.v(TAG, String.format("Idle workers count: %d", idleWorkersPool.size()));
        checkUploader();
    }

}
