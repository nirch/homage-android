package com.homage.app.recorder;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Handles camera operation requests from other threads.  Necessary because the Camera
 * must only be accessed from one thread.
 * <p>
 * The object is created on the UI thread, and all handlers run there.  Messages are
 * sent from other threads, using sendMessage().
 */
class CameraHandler extends Handler {
    private String TAG = "TAG_"+getClass().getName();

    public static final int MSG_SET_SURFACE_TEXTURE = 0;
    public static final int MSG_OUTPUTFILE_SET      = 1;
    public static final int MSG_STOP_RECORDING      = 2;

    // Weak reference to the Activity; only access this from the UI thread.
    private WeakReference<RecorderActivity> mWeakActivity;

    public CameraHandler(RecorderActivity activity) {
        mWeakActivity = new WeakReference<RecorderActivity>(activity);
    }

    /**
     * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
     * attempts to access a stale Activity through a handler are caught.
     */
    public void invalidateHandler() {
        mWeakActivity.clear();
    }

    @Override  // runs on UI thread
    public void handleMessage(Message inputMessage) {
        int what = inputMessage.what;
        Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

        RecorderActivity activity = mWeakActivity.get();
        if (activity == null) {
            Log.w(TAG, "CameraHandler.handleMessage: activity is null");
            return;
        }

        switch (what) {
            case MSG_OUTPUTFILE_SET:
                //String outputFileName = (String)inputMessage.obj;
                //activity.showOutputFileName(outputFileName);
                break;
            default:
                throw new RuntimeException("unknown msg " + what);
        }
    }
}