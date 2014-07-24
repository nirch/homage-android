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
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.List;

public class CameraManager {
    String TAG = "TAG_"+getClass().getName();

    final int defaultBackWidth = 1280;
    final int defaultBackHeight = 720;

    final int defaultFrontWidth = 640;
    final int defaultFrontHeight = 480;

    int[] fpsRange;

    //public CameraPreview cameraPreview;
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

    private int currentCameraId;

    private boolean selfie = false;

    Context context;

    public void reconnect() throws IOException {
        if (camera != null) {
            camera.reconnect();
        } else {
            newCamera();
        }
    }

    public void unlock() throws IOException {
        if (camera != null) {
            camera.unlock();
        }
    }

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
            try {
                camera.release();
                camera = null;
                preview.getHolder().removeCallback(preview);
            } catch (Exception e) {
            }
        }
    }

    public boolean isCameraAvailable() {
        if (camera == null) return false;
        return true;
    }

    /**
     *  Initialization of the Camera manager.
     *
     * @param context The context of initialization. (Activity or Application)
     */
    public void init(Context context){
        // Can be initialized only once!
        this.context = context;
        newCamera();
    }

    public boolean isInitialized() {
        if (this.context == null) return false;
        return true;
    }

    public void newCamera() {
        if (camera!=null) {
            try {
                camera.release();
            } catch (Exception e) {
            }
        }

        int defaultWidth, defaultHeight;

        try {
            if (selfie) {
                camera = CameraHelper.getDefaultFrontFacingCameraInstance();
                defaultWidth = defaultFrontWidth;
                defaultHeight = defaultFrontHeight;
            } else {
                camera = CameraHelper.getDefaultBackFacingCameraInstance();
                defaultWidth = defaultBackWidth;
                defaultHeight = defaultBackHeight;
            }
        } catch (Exception e) {
            Log.d(TAG, "Hmmm...", e);
            camera = CameraHelper.getDefaultBackFacingCameraInstance();
            defaultWidth = defaultBackWidth;
            defaultHeight = defaultBackHeight;
        }

        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size supportedSize : supportedPreviewSizes) {
            Log.v(TAG, String.format("Supported size: %d, %d", supportedSize.width, supportedSize.height));
        }

        recSize = CameraHelper.getOptimalPreviewSize(
                supportedPreviewSizes,
                defaultWidth,
                defaultHeight);

        Log.d(TAG, String.format(
                "Initialized camera manager. Requested size for recording: %d,%d",
                recSize.width,
                recSize.height));

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
    FrameLayout previewContainer;

    public void startCameraPreviewInView(Context context, FrameLayout previewContainer) {
        if (camera == null) {
            newCamera();
        }

        this.previewContainer = previewContainer;

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

                //
                // Set some default parameters for the preview
                //
                Camera.Parameters parameters = mCamera.getParameters();

                // FPS
                fpsRange = CameraHelper.getPrefferedFPSRangeFromParameters(parameters);
                parameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);

                // Auto video focus mode (but only if available)
                if (parameters.getSupportedFocusModes()
                        .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }

                // Update the params
                mCamera.setParameters(parameters);


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

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            Log.d(TAG, String.format("Surface of the cam preview changed. %d %d", w, h));
            fixPreviewAspectRatio(w, h);
            restartPreview();
        }

        private void fixPreviewAspectRatio(int containerWidth, int containerHeight) {
            // Crop the video to get the correct aspect ratio + cropping effect
            // (Homage calls it the 16/9 wysiwyg feature)
            Camera.Parameters parameters = camera.getParameters();


            Camera.Size previewSize = parameters.getPreviewSize();
            int videoWidth = previewSize.width;
            int videoHeight = previewSize.height;

            //float videoAspectRatio = (float)videoWidth/(float)videoHeight;
            //float containerAspectRatio = (float)containerWidth/(float)containerHeight;
            float dx = (float)containerWidth / (float)videoWidth;
            float dy = (float)containerHeight / (float)videoHeight;

            int fixX = (int)((containerHeight/dx)*dy);
            int fixY = (int)((containerWidth/dy)*dx);

            int paddingX = -(containerWidth - fixY) / 2;
            int paddingY = -(containerHeight - fixX) / 2;

            Log.d(TAG, String.format("Padding X Y: %d %d", paddingX, paddingY));

            if (paddingX<0) {
                previewContainer.setPadding(paddingX,0,paddingX,0);
            } else if (paddingY<0) {
                previewContainer.setPadding(0,paddingY,0,paddingY);
            } else {
                previewContainer.setPadding(0,0,0,0);
            }

        }

        public void stop() {
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
        }

        public void restartPreview() {
            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            stop();

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

    }


    public void resetToPreferBackCamera() {
        selfie = false;
    }

    public void flipCamera() {
        // Stop
        preview.stop();
        previewContainer.removeAllViews();
        try {
            camera.release();
        } catch (Exception e) {
        }

        // Toggle back / front
        selfie = !selfie;

        // Restart
        newCamera();
        CameraPreview cameraPreview = new CameraPreview(context, camera);
        cameraPreview.setZOrderOnTop(false);
        previewContainer.addView(cameraPreview);
        preview = cameraPreview;
    }

    public void restartCamera() {
        // Stop
        preview.stop();
        previewContainer.removeAllViews();
        try {
            camera.release();
        } catch (Exception e) {
        }

        // Restart
        newCamera();
        CameraPreview cameraPreview = new CameraPreview(context, camera);
        cameraPreview.setZOrderOnTop(false);
        previewContainer.addView(cameraPreview);
        preview = cameraPreview;
    }

    public String startRecording(int videoDuration, MediaRecorder.OnInfoListener onRecordingInfoListener) {
        //
        // Get high quality camera for the used camera.
        //
        CamcorderProfile profile;
        if (selfie) {
            profile = CamcorderProfile.get(Camera.CameraInfo.CAMERA_FACING_FRONT, CamcorderProfile.QUALITY_HIGH);
        } else {
            profile = CamcorderProfile.get(Camera.CameraInfo.CAMERA_FACING_BACK, CamcorderProfile.QUALITY_HIGH);
        }
        profile.videoFrameWidth = recSize.width;
        profile.videoFrameHeight = recSize.height;

        //
        // Camera settings
        //
        Camera.Parameters parameters = camera.getParameters();
        // Lock exposure
        parameters.setAutoExposureLock(true);
        //Lock focus mode
        if (parameters.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        }
        camera.setParameters(parameters);

        //
        // Unlock the camera so it can be used by the media recorder.
        //
        camera.unlock();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        if (Build.VERSION.SDK_INT > 16) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        }
        else {
            // Added because of problem on Galaxy S2 with android 4.1.2
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        }

        //
        // Set the profile
        //
        mediaRecorder.setProfile(profile);

        //
        // Set the output file.
        //
        String outputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO).toString();
        mediaRecorder.setOutputFile(outputFile);
        mediaRecorder.setPreviewDisplay(preview.mHolder.getSurface());

        //
        // Set the duration of the captured video
        // And set the end recording listener
        //
        mediaRecorder.setMaxDuration(videoDuration);
        mediaRecorder.setOnInfoListener(onRecordingInfoListener);

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

            //
            // Camera settings
            //
            Camera.Parameters parameters = camera.getParameters();

            // Unlock exposure
            parameters.setAutoExposureLock(false);

            // Unlockock focus mode
            if (parameters.getSupportedFocusModes()
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            camera.setParameters(parameters);

        }
    }
}
