package com.homage.networking.uploader;

import android.util.Log;

import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Upload;

import java.util.Dictionary;

public class UploadS3Worker implements UploadWorker {
    String TAG = "TAG_"+getClass().getName();

    private String source;
    private String destination;
    private String jobID;
    private double progress;
    private Dictionary<String, Object>userInfo;
    private AWS3Client client;

    public UploadS3Worker() {
        client = AWS3Client.sh();
    }

    @Override
    public void newJob(String jobID, String source, String destination) {
        this.jobID = jobID;
        this.source = source;
        this.destination = destination;
        this.progress = 0;
    }

    @Override
    public boolean startWorking() {
        Upload uploadTransferOperation = this.client.startUploadJobForWorker(this);
        if (uploadTransferOperation==null) {
            Log.d(TAG, String.format("Couldn't start upload job (transferOperation is null) %s", this.jobID));
            return false;
        }
        this.userInfo.put("uploadTransferOperation", uploadTransferOperation);
        return true;
    }

    @Override
    public void stopWorking() {
        //Upload uploadTransferOperation = self.userInfo.get("transferOperation");
        //uploadTransferOperation.

        //PutObjectRequest putRequest = uploadTransferOperation.
    }
}
