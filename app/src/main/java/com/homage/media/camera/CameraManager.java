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
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.android.grafika.CameraUtils;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;

import java.io.IOException;

public class CameraManager {
    String TAG = "TAG_"+getClass().getName();

    int defaultBackWidth;
    int defaultBackHeight;

    int defaultFrontWidth;
    int defaultFrontHeight;

    public int mCameraPreviewWidth;
    public int mCameraPreviewHeight;


    int[] fpsRange;

    //public CameraPreview cameraPreview;
    Camera.Size recSize;

    //region *** singleton pattern ***
    private static CameraManager instance = new CameraManager();
    public static CameraManager sharedInstance() {
        if(instance == null) {
            instance = new CameraManager();
        }
        return instance;
    }
    public static CameraManager sh() {
        return CameraManager.sharedInstance();
    }

    public CameraManager() {
        init();
    }
    //endregion

    private void init() {
        Resources res = HomageApplication.getContext().getResources();

        // Get defaults set in camera.xml
        defaultBackWidth = res.getInteger(R.integer.cameraBackPreferredRecodingWidth);
        defaultBackHeight = res.getInteger(R.integer.cameraBackPreferredRecodingHeight);
        defaultFrontWidth = res.getInteger(R.integer.cameraFrontPreferredRecodingWidth);
        defaultFrontHeight = res.getInteger(R.integer.cameraFrontPreferredRecodingHeight);

    }

    private int currentCameraId;
    private boolean selfie = false;
    Context context;

    Camera mCamera;

    //region *** Camera ***
    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */

    public void openCamera() {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        int numCameras = Camera.getNumberOfCameras();
        int desiredWidth, desiredHeight;
        if (selfie) {
            desiredWidth = defaultFrontWidth;
            desiredHeight = defaultFrontHeight;
        } else {
            desiredWidth = defaultBackWidth;
            desiredHeight = defaultBackHeight;
        }

        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (!selfie && info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(i);

                break;
            }
            if (selfie && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }

        if (mCamera == null) {
            // Open some camera if none opened.
            mCamera = Camera.open();
        }

        if (mCamera == null) {
            // Camera should have opened. If not, this is a critical error.
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);

        // leave the frame rate set to default
        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    public void flipCamera() {
        releaseCamera();
        selfie = !selfie;
    }
    //endregion

    public void releaseRecordingManagers() {
        // Release the used media recorder (MediaRecorder or MediaCodec).
        Log.d(TAG, "Release the recorder used.");
    }

    public void setCameraPreviewTexture(SurfaceTexture st) {
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

}
