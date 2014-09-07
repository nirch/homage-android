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
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.android.grafika.CameraUtils;

import java.io.IOException;

public class CameraManager {
    String TAG = "TAG_"+getClass().getName();

    final int defaultBackWidth = 1280;
    final int defaultBackHeight = 720;
    final int defaultFrontWidth = 640;
    final int defaultFrontHeight = 480;

    public int mCameraPreviewWidth;
    public int mCameraPreviewHeight;


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

    Camera mCamera;

    //region *** Camera ***
    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    public void reopenCamera() {
        if (mCamera != null) return;
        openCamera(mCameraPreviewWidth, mCameraPreviewHeight);
    }

    public void openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(i);
                break;
            }
        }

        if (mCamera == null) {
            mCamera = Camera.open();
        }

        if (mCamera == null) {
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
    //endregion


    public void setCameraPreviewTexture(SurfaceTexture st) {
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

}
