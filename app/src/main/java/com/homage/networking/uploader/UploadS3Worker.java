package com.homage.networking.uploader;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.transfer.Upload;
import com.homage.networking.analytics.HMixPanel;

import java.io.File;
import java.util.HashMap;

public class UploadS3Worker implements ProgressListener, UploadWorker {
    String TAG = "TAG_"+getClass().getName();

//    Constants for info
    public static final String SOURCE = "source";
    public static final String DESTINATION = "destination";
    public static final String DURATION = "duration";
    public static final String KBPERSECONDSPEED = "spd";
    public static final String TOTALBYTESUPLOADED = "totalBytesUploaded";
    public static final String TOTAL_BYTES_EXPECTED_TO_WRITE = "total_bytes_expected_to_write";
//    public static final String NETWORK_TYPE = "network_type";
    public static final String ERROR = "error";
    public static final String IS_CANCELED = "is_canceled";

    private String source;
    private String destination;
    private String jobID;
    private double progress;
    private long totalBytesUploaded;
    private long fileSize;
    private HashMap<String, Object> userInfo;
    private AWS3Client client;
    private Upload uploadTransferOperation;
    private long duration;
    private long startTime;
    private long kbPerSecondSpeed;

    private boolean sourceFileChangedDuringUpload;
    private String newSource;

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
    public String getNewSource() { return newSource; }

    @Override
    public void newJob(String jobID, String source, String destination) {
        reset();
        this.jobID = jobID;
        this.source = source;
        this.destination = destination;
        this.sourceFileChangedDuringUpload = false;
        this.newSource = null;
    }

    @Override
    public boolean startWorking() {
        HashMap props = new HashMap<String,String>();

        File sourceFile = new File(source);
        if (sourceFile.exists()) {
            this.fileSize = sourceFile.length();
        } else {
            // File doesn't exist.
            Log.e(TAG, String.format("Can't upload missing file: %s", source));
            props.put("reason" , "Can't upload missing file");
            props.put("more_info" , String.format("Can't upload missing file: %s", source));
            HMixPanel.sh().track("UploadJobStartFailed",props);
            HMixPanel mp = HMixPanel.sh();
            return false;
        }

        uploadTransferOperation = this.client.startUploadJobForWorker(this, sourceFile, destination);

        if (uploadTransferOperation==null) {
            Log.d(TAG, String.format("Couldn't start upload job (transferOperation is null) %s", this.jobID));
            props.put("reason", "transferOperation is null");
            props.put("more_info" , String.format("Couldn't start upload job (transferOperation is null) %s", this.jobID));
            HMixPanel.sh().track("UploadJobStartFailed",props);
            HMixPanel mp = HMixPanel.sh();
            return false;
        }
        this.userInfo.put("uploadTransferOperation", uploadTransferOperation);
        uploadTransferOperation.addProgressListener(this);
        HMixPanel mp = HMixPanel.sh();
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
        this.duration = 0;
        this.kbPerSecondSpeed = 0;
        this.startTime = System.currentTimeMillis();
        this.userInfo = new HashMap<String, Object>();
        this.fileSize = 0;
        this.uploadTransferOperation = null;
        this.sourceFileChangedDuringUpload = false;
        this.newSource = null;
    }


    public void reportSourceFileChangedDuringUpload(String newSource) {
        this.sourceFileChangedDuringUpload = true;
        this.newSource = newSource;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        totalBytesUploaded += progressEvent.getBytesTransferred();
        progress = (double)totalBytesUploaded/(double)fileSize;
        duration += System.currentTimeMillis() - startTime;
        kbPerSecondSpeed = totalBytesUploaded/duration;

        switch (progressEvent.getEventCode()) {

            case ProgressEvent.STARTED_EVENT_CODE: {
                Log.v(TAG, String.format("started upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                break;
            }
            case ProgressEvent.COMPLETED_EVENT_CODE: {

                String log = String.format("Completed upload %d %.02f %s %s", progressEvent.getEventCode(), progress, source, destination);
                Log.v(TAG, log);

                HashMap props = new HashMap<String, String>();
                props.put(SOURCE, source);
                props.put(DESTINATION, destination);
                props.put(DURATION, String.valueOf(duration));
                props.put(KBPERSECONDSPEED, String.valueOf(kbPerSecondSpeed));
                props.put(TOTALBYTESUPLOADED, String.valueOf(totalBytesUploaded));
                props.put(TOTAL_BYTES_EXPECTED_TO_WRITE, String.valueOf(fileSize));
//                props.put(NETWORK_TYPE , network_type); could not find this

                if (sourceFileChangedDuringUpload) {
                    UploadManager.sh().finishedWithIrrelevantUpload(this);
                    props.put(ERROR, "finishedWithIrrelevantUpload " + log);
                    HMixPanel.sh().track("UploadFailed", props);
                    HMixPanel mp = HMixPanel.sh();
                    if (mp != null) mp.flush();
                    break;
                }

                HMixPanel.sh().track("UploadSuccess", props);
                HMixPanel mp = HMixPanel.sh();

                UploadManager.sh().finishedUpload(this);
                break;
            }
            case ProgressEvent.FAILED_EVENT_CODE:{

                String error = String.format("Failed upload %d %.02f %s", progressEvent.getEventCode(), progress, source);
                Log.v(TAG, error);

                HashMap props = new HashMap<String,String>();
                props.put(SOURCE , source);
                props.put(DESTINATION , destination);
                props.put(DURATION , String.valueOf(duration));
                props.put(KBPERSECONDSPEED , String.valueOf(kbPerSecondSpeed));
                props.put(TOTALBYTESUPLOADED , String.valueOf(totalBytesUploaded));
                props.put(TOTAL_BYTES_EXPECTED_TO_WRITE , String.valueOf(fileSize));
//                props.put(NETWORK_TYPE , network_type); could not find this
                props.put(ERROR, error);
                props.put(IS_CANCELED, "false");
                HMixPanel.sh().track("UploadFailed", props);
                HMixPanel mp = HMixPanel.sh();

                progress = 0;
                totalBytesUploaded = 0;
                UploadManager.sh().failedUpload(this);
                break;
            }
            case ProgressEvent.CANCELED_EVENT_CODE: {
                String error = String.format("Canceled upload %d %.02f %s", progressEvent.getEventCode(), progress, source);
                Log.v(TAG, error);
                HashMap props = new HashMap<String,String>();
                props.put(SOURCE , source);
                props.put(DESTINATION , destination);
                props.put(DURATION , String.valueOf(duration));
                props.put(KBPERSECONDSPEED , String.valueOf(kbPerSecondSpeed));
                props.put(TOTALBYTESUPLOADED , String.valueOf(totalBytesUploaded));
                props.put(TOTAL_BYTES_EXPECTED_TO_WRITE , String.valueOf(fileSize));
//                props.put(NETWORK_TYPE , network_type); could not find this
                props.put(ERROR, error);
                HMixPanel.sh().track("UploadCanceled", props);
                HMixPanel mp = HMixPanel.sh();

                progress = 0;
                totalBytesUploaded = 0;
                UploadManager.sh().failedUpload(this);
                break;
            }
            case ProgressEvent.RESET_EVENT_CODE: {
                Log.v(TAG, String.format("Reset upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                progress = 0;
                totalBytesUploaded = 0;
                break;
            }
            default:
                //Log.v(TAG, String.format("Progress upload %d %.02f %s", progressEvent.getEventCode(), progress, source));
                break;
        }
    }
}
