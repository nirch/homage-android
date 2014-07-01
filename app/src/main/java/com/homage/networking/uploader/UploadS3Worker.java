package com.homage.networking.uploader;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;

public class UploadS3Worker implements ProgressListener, UploadWorker {
    String TAG = "TAG_"+getClass().getName();

    private String source;
    private String destination;
    private String jobID;
    private double progress;
    private long totalBytesUploaded;
    private long fileSize;
    private HashMap<String, Object> userInfo;
    private AWS3Client client;

    public UploadS3Worker() {
        client = AWS3Client.sh();
    }

    @Override
    public String getJobID() {
        return jobID;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void newJob(String jobID, String source, String destination) {
        reset();
        this.jobID = jobID;
        this.source = source;
        this.destination = destination;
    }

    @Override
    public boolean startWorking() {
        File sourceFile = new File(source);
        if (sourceFile.exists()) {
            this.fileSize = sourceFile.length();
        } else {
            // File doesn't exist.
            Log.e(TAG, String.format("Can't upload missing file: %s", source));
            return false;
        }

        Upload uploadTransferOperation = this.client.startUploadJobForWorker(this, sourceFile, destination);

        if (uploadTransferOperation==null) {
            Log.d(TAG, String.format("Couldn't start upload job (transferOperation is null) %s", this.jobID));
            return false;
        }
        this.userInfo.put("uploadTransferOperation", uploadTransferOperation);
        uploadTransferOperation.addProgressListener(this);
        return true;
    }

    @Override
    public void stopWorking() {

    }

    @Override
    public void reset() {
        this.jobID = null;
        this.source = null;
        this.destination = null;
        this.progress = 0;
        this.totalBytesUploaded = 0;
        this.userInfo = new HashMap<String, Object>();
        this.fileSize = 0;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        totalBytesUploaded += progressEvent.getBytesTransferred();
        progress = (double)totalBytesUploaded/(double)fileSize;

        switch (progressEvent.getEventCode()) {
            case ProgressEvent.STARTED_EVENT_CODE:
                Log.v(TAG, String.format("started upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                break;
            case ProgressEvent.COMPLETED_EVENT_CODE:
                Log.v(TAG, String.format("Completed upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                UploadManager.sh().finishedUpload(this);
                break;
            case ProgressEvent.FAILED_EVENT_CODE:
                Log.v(TAG, String.format("Failed upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                progress = 0;
                totalBytesUploaded = 0;
                UploadManager.sh().failedUpload(this);
                break;
            case ProgressEvent.CANCELED_EVENT_CODE:
                Log.v(TAG, String.format("Canceled upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                progress = 0;
                totalBytesUploaded = 0;
                UploadManager.sh().failedUpload(this);
                break;
            case ProgressEvent.RESET_EVENT_CODE:
                Log.v(TAG, String.format("Reset upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                progress = 0;
                totalBytesUploaded = 0;
                break;
            default:
                Log.v(TAG, String.format("Progress upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
        }
    }
}
