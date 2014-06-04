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
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.List;

public class CameraManager {
    String TAG = "TAG_"+getClass().getName();

    final int defaultWidth = 640;
    final int defaultHeight = 480;

    public CameraPreview cameraPreview;
    Camera.Size recSize;

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

    public void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            // clear recorder configuration
            mediaRecorder.reset();
            // release the recorder object
            mediaRecorder.release();
            mediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it.
        }

        if (camera != null) {
            camera.lock();
        }
    }

    public void releaseCamera(){
        if (camera != null){
            // release the camera for other applications
            camera.release();
            camera = null;
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

        camera = CameraHelper.getDefaultCameraInstance();
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        recSize = CameraHelper.getOptimalPreviewSize(
                supportedPreviewSizes,
                defaultWidth,
                defaultHeight);

        Log.d(TAG, String.format(
                "Initialized camera manager. Requested size for recording: %d,%d",
                recSize.width,
                recSize.height));

        alreadyInitialized = true;
    }

    // Initializations.
    private CameraManager() {
        alreadyInitialized = false;
    }
    //endregion

    public CameraPreview preview;

    private Camera camera;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    public void startCameraPreviewInView(Context context, FrameLayout previewContainer) {
        if (camera == null) camera = CameraHelper.getDefaultCameraInstance();
        CameraPreview cameraPreview = new CameraPreview(context, camera);
        cameraPreview.setZOrderOnTop(false);
        previewContainer.addView(cameraPreview);
        preview = cameraPreview;
    }

    /** Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
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

        public void hide() {
            this.setVisibility(CameraPreview.INVISIBLE);
        }

        public void show() {
            this.setVisibility(CameraPreview.VISIBLE);
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

    public String startRecording() {
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = recSize.width;
        profile.videoFrameHeight = recSize.height;

        camera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setVideoSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setProfile(profile);

        String outputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO).toString();
        mediaRecorder.setOutputFile(outputFile);

        // Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return null;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return null;
        }
        return outputFile;
    }

    public void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException stopping MediaRecorder", e);
            }
            releaseMediaRecorder();
            camera.lock();
            isRecording = false;
        }
    }
}
