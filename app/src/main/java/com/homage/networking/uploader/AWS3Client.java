package com.homage.networking.uploader;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

public class AWS3Client {
    private static final String BUCKET_NAME = "homageapp";
    private static final String ACCESS_KEY_ID = "AKIAJTPGKC25LGKJUCTA";
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
     *  Start an Upload operation for this worker and if it is implementing the AmazonServiceRequestDelegate protocol
     *   will route all related delegate method call to it.
     *
     *
     * @param s3Worker The worker keeping track of this specific upload job and reports to the manager.
     * @return Returns the S3TransferOperation object for this upload operation.
     */
    public Upload startUploadJobForWorker(UploadS3Worker s3Worker) {
        return null;
    }
}
