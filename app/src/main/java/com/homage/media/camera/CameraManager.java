/*
   .-------------------.
  /--"--.------.------/|
  |  H  |__Ll__| [==] ||
  |  O  |      | """" ||
  |  M  | .--. |      ||
  |  A  |( () )|      ||
  |  G  | `--' |      ||
  |  E  |      |      |/
  `-----'------'------'
      Camera Manager

  A singleton managing the initialization and release of the camera.

  Important:
  ----------
    It is best not to implement starting and releasing the camera just from specific activities.
    If the camera is not released properly by the application, for whatever reason
    (the app crashed, it wasn't released due to a bug in the flow of the UI, etc.)
    the camera will be in a bad state **OS wide**. You can even crash other application
    or block their ability to use the camera properly.
    Yeah I know, some google engineers made some bad API decisions here.

  This singleton will be also used from outside of activities,
  for example from the HomageApplication class that extends Application.
 */
package com.homage.media.camera;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;

public class CameraManager {
    String TAG = "TAG_"+getClass().getName();

    private int recordingWidth;
    private int recordingHeight;
    private int previewWidth;
    private int previewHeight;

    Camera.Size optimalSizeForPreview;
    Camera.Size optimalSizeForRecording;

    //region *** singleton pattern ***
    private boolean alreadyInitialized;
    private static CameraManager instance = new CameraManager();
    public static CameraManager sharedInstance() {
        if(instance == null) instance = new CameraManager();
        return instance;
    }
    public static CameraManager sh() {
        return CameraManager.sharedInstance();
    }
    //endregion

    public static CameraManager initializeInstance(int width, int height) {
        assert false;
        return instance;
    }

    public void releaseMediaRecorder(){
        if (recMediaRecorder != null) {
            // clear recorder configuration
            recMediaRecorder.reset();
            // release the recorder object
            recMediaRecorder.release();
            recMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it.
            recCamera.lock();
        }
    }

    public void releaseCamera(){
        if (recCamera != null){
            // release the camera for other applications
            recCamera.release();
            recCamera = null;
        }
    }

    /**
     *  Initialization of the Camera manager.
     *
     * @param context The context of initialization. (Activity or Application)
     */
    public void init(Context context){
        // Can be initialized only once!
        if (alreadyInitialized)
            throw new AssertionError("Tried to initialize CameraManager more than once.");

        this.recordingWidth = recordingWidth;
        this.recordingHeight = recordingHeight;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;

        Log.d(TAG, String.format(
                "Initialized camera manager. Requested size for preview: %d,%d",
                previewWidth,
                previewHeight));

        Log.d(TAG, String.format(
                "Initialized camera manager. Requested size for recording: %d,%d",
                recordingWidth,
                recordingHeight));


        alreadyInitialized = true;
    }

    // Initializations.
    private CameraManager() {
        alreadyInitialized = false;
    }
    //endregion

    private Camera recCamera;
    private MediaRecorder recMediaRecorder;
    private boolean isRecording = false;
    private CameraPreview recPreview;

    public void startCameraPreviewInView(Context context, FrameLayout previewContainer) {
        recCamera = CameraHelper.getDefaultCameraInstance();
        CameraPreview cameraPreview = new CameraPreview(context, recCamera);
        previewContainer.addView(cameraPreview);
    }

    /** Camera preview class */
    private class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                Log.d(TAG, "changed device orientation. Will need to flip the preview view.");
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }
}
