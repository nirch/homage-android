package com.homage.networking.uploader;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UploaderService extends IntentService {
    private String TAG = "TAG_"+getClass().getName();

    public static final String CMD = "cmd";
    public static final int CMD_UNKNOWN                     = 0;
    public static final int CMD_START                       = 10;
    public static final int CMD_STOP                        = 11;
    public static final int CMD_CHECK_FOR_PENDING_UPLOADS   = 100;

    static Thread uploaderThread;
    static boolean isActive;

    public UploaderService() {
        super("UploadService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static Intent cmd(Context context, int command) {
        Intent cmdIntent = new Intent(context, UploaderService.class);
        cmdIntent.putExtra(UploaderService.CMD, command);
        return cmdIntent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int cmd = intent.getIntExtra(CMD, CMD_UNKNOWN);
        switch (cmd) {
            case CMD_START:
                start();
                break;

            case CMD_STOP:
                stop();
                break;

            case CMD_CHECK_FOR_PENDING_UPLOADS:
                checkForPendingUploads();
                break;
        }
    }

    private void start() {
        Log.d(TAG, "Started homage uploader background service.");
        isActive = true;
        checkForPendingUploads();

        /*
        if (UploaderService.uploaderThread != null) {
            Log.d(TAG, "Will not start a new uploader thread. Thread already running.");
            return;
        }
        UploaderService.uploaderThread = new Thread() {
            @Override
            public void run() {
                UploaderService.isActive = true;
                try {
                    while (isActive) {
                        Log.d(TAG, "Uploader running.");
                        checkForPendingUploads();
                        Thread.sleep(60000);
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "Uploader thread unhandled exception.", ex);
                } finally {
                    Log.d(TAG, "Uploader shutting down...");
                }
                isActive = false;
                uploaderThread = null;
            }
        };
        uploaderThread.start();
        */
    }

    private void stop() {
        Log.d(TAG, "Stopped homage uploader background service.");
        isActive = false;
    }

    private void checkForPendingUploads() {
        if (!isActive) return;
        UploadManager.sh().checkForPendingUploads();
    }
}