package com.homage.networking.uploader;

import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class AWS3Client {
    String TAG = "TAG_AWS3Client";

    public static final String BUCKET_NAME = "homagetest";
    private static final String ACCESS_KEY_ID = "AKIAJJQ55763CDX5DENQ";
    private static final String SECRET_KEY = "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";

    public TransferManager tm;

    //region *** singleton pattern ***
    private static AWS3Client instance = new AWS3Client();
    public static AWS3Client sharedInstance() {
        if(instance == null) instance = new AWS3Client();
        instance.init();
        return instance;
    }
    public static AWS3Client sh() {
        return sharedInstance();
    }

    private void init() {
        AWSCredentials myCredentials = new BasicAWSCredentials(ACCESS_KEY_ID, SECRET_KEY);

        // Initialize the S3 client / transfer manager.
        tm = new TransferManager(myCredentials);
    }
    //endregion

    /**
     *  Start an Upload operation for this worker.
     *
     *
     * @param s3Worker The worker keeping track of this specific upload job and reports to the manager.
     * @return Returns the S3TransferOperation object for this upload operation.
     */
    public Upload startUploadJobForWorker(UploadS3Worker s3Worker, File sourceFile, String destination) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, destination, sourceFile);
        Upload upload;
        Log.v(TAG, String.format("Starting upload from %s to %s", sourceFile.getAbsoluteFile(), destination));
        upload = tm.upload(putObjectRequest);
        return upload;
    }
}
