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
package com.homage.media;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.Display;

import java.io.IOException;
import java.util.List;

public class CameraManager {
    String TAG = getClass().getName();

    private int recordingWidth;
    private int recordingHeight;
    private int previewWidth;
    private int previewHeight;

    Camera.Size optimalSizeForPreview;
    Camera.Size optimalSizeForRecording;

    private boolean alreadyInitialized;

    //region *** singleton pattern ***
    private static CameraManager instance = new CameraManager();
    public static CameraManager getInstance() {
        if(instance == null) instance = new CameraManager();
        return instance;
    }

    public static CameraManager initializeInstance(int width, int height) {
        assert false;
        return instance;
    }

    /**
     *  Initialization of the Camera manager.
     *
     * @param recordingWidth The preferred width of the video output of the recording.
     * @param recordingHeight The preferred height of the video output of the recording.
     * @param previewWidth The preview layer width (usually the width of the device in landscape)
     * @param previewHeight The preview layer height (usually the width of the device in landscape)
     */
    public void init(int recordingWidth, int recordingHeight, int previewWidth, int previewHeight){
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

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder}
     * since it's a long blocking operation.
     */
    public void prepareVideoAsync() {
        new MediaPrepareTask().execute(null, null, null);
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            return prepareVideoRecorder();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
            }
            Log.d(TAG, String.format("Camera post execute result %b", result));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private boolean prepareVideoRecorder() {
        recCamera = CameraHelper.getDefaultCameraInstance();

        // Choose optimal sizes for preview and recording.
        chooseOptimalSizes();

        return true;
    }


    private void chooseOptimalSizes() {
        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions passed to the camera manager on initialization.
        Camera.Parameters parameters = recCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();

        // Optimal size for preview.
        optimalSizeForPreview = CameraHelper.getOptimalPreviewSize(
                mSupportedPreviewSizes,
                previewWidth,
                previewHeight);

        Log.d(TAG, String.format(
                "Camera optimal size for recording:%d,%d",
                optimalSizeForPreview.width,
                optimalSizeForPreview.height));

        // Optimal size for recording.
        optimalSizeForRecording = CameraHelper.getOptimalPreviewSize(
                mSupportedPreviewSizes,
                recordingWidth,
                recordingHeight);

        Log.d(TAG, String.format(
                "Camera optimal size for recording:%d,%d",
                optimalSizeForRecording.width,
                optimalSizeForRecording.height));

    }

//
//
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    private boolean prepareVideoRecorder(){
//        recCamera = CameraHelper.getDefaultCameraInstance();
//
//        // We need to make sure that our preview and recording video size are supported by the
//        // camera. Query camera to find all the sizes and choose the optimal size given the
//        // dimensions of our preview surface.
//        Camera.Parameters parameters = recCamera.getParameters();
//        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
//        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
//                recPreview.getWidth(), recPreview.getHeight());
//
//        // Use the same size for recording profile.
//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//        profile.videoFrameWidth = optimalSize.width;
//        profile.videoFrameHeight = optimalSize.height;
//
//        // likewise for the camera object itself.
//        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        recCamera.setParameters(parameters);
//        try {
//            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
//            // with {@link SurfaceView}
//            recCamera.setPreviewTexture(recPreview.getSurfaceTexture());
//        } catch (IOException e) {
//            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
//            return false;
//        }
//        // END_INCLUDE (configure_preview)
//
//
//        // BEGIN_INCLUDE (configure_media_recorder)
//        recMediaRecorder = new MediaRecorder();
//
//        // Step 1: Unlock and set camera to MediaRecorder
//        recCamera.unlock();
//        recMediaRecorder.setCamera(recCamera);
//
//        // Step 2: Set sources
//        recMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
//        recMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
//        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//        recMediaRecorder.setProfile(profile);
//
//        // Step 4: Set output file
//        recMediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(
//                CameraHelper.MEDIA_TYPE_VIDEO).toString());
//        // END_INCLUDE (configure_media_recorder)
//
//        // Step 5: Prepare configured MediaRecorder
//        try {
//            recMediaRecorder.prepare();
//        } catch (IllegalStateException e) {
//            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        } catch (IOException e) {
//            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        }
//        return true;
//    }
}
