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
package com.homage.app.recorder;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.grafika.CameraUtils;
import com.crashlytics.android.Crashlytics;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class CameraManager {
    String TAG = "TAG_HomageCameraManager";


    // Camera
    Camera.Size recSize;
    private boolean selfie = false;
    int defaultBackWidth;
    int defaultBackHeight;
    int defaultFrontWidth;
    int defaultFrontHeight;
    int defaultOutputWidth;
    int defaultOutputBitRate;
    private boolean cameraPreviewUsesGLSurfaceView;
    public int mCameraPreviewWidth;
    public int mCameraPreviewHeight;
    int defaultEveryNthPreviewFrame = 10;
    int everyNthPreviewFrame = 10;
    Camera mCamera;
    BackgroundDetection backgroundDetection;
    Context context;
    // Recording
    MediaRecorder mediaRecorder;
    boolean recorderUseMediaCodecWhenAvailable;
    Context recorderContext;
    RecorderActivity recorderActivity;


    // Preview
    FrameLayout previewContainer;
    public CameraPreview mPreview;
    boolean previewInitialized;
    boolean previewPaused;

    public void SetContext(Context context){
        recorderContext = context;
        recorderActivity = ((RecorderActivity)context);
    }

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

    //region *** initializations ***
    private void init() {
        Resources res = HomageApplication.getContext().getResources();
        // Get defaults set in camera.xml
        defaultBackWidth = res.getInteger(R.integer.cameraBackPreferredRecodingWidth);
        defaultBackHeight = res.getInteger(R.integer.cameraBackPreferredRecodingHeight);
        defaultFrontWidth = res.getInteger(R.integer.cameraFrontPreferredRecodingWidth);
        defaultFrontHeight = res.getInteger(R.integer.cameraFrontPreferredRecodingHeight);

        recorderUseMediaCodecWhenAvailable = res.getBoolean(R.bool.recorderUseMediaCodecWhenAvailable);

        defaultOutputWidth = res.getInteger(R.integer.recorderMediaCodecPreferredOutputWidth);
        defaultOutputBitRate = res.getInteger(R.integer.recorderMediaCodecPreferredOutputBitRate);

        // Currently only false is supported. GL Surface view support deprecated.
        cameraPreviewUsesGLSurfaceView = res.getBoolean(R.bool.cameraPreviewUsesGLSurfaceView);
        if (cameraPreviewUsesGLSurfaceView) {
            throw new RuntimeException("cameraPreviewUsesGLSurfaceView support deprecated");
        }

        previewInitialized = false;
        previewPaused = false;
    }

    public int getOutputPreferredWidth() {
        return defaultOutputWidth;
    }

    public int getOutputPreferredBitRate() {
        return defaultOutputBitRate;
    }

    public boolean previewUsesGLSurfaceView() {
        return cameraPreviewUsesGLSurfaceView;
    }

    public CameraPreview startCameraPreviewInView(Context context, FrameLayout previewContainer) {
        // The preview container
        this.previewContainer = previewContainer;

        // Create a new CameraPreview
        CameraPreview cameraPreview = new CameraPreview(context);
        cameraPreview.setZOrderOnTop(false);
        mPreview = cameraPreview;

        // Add the Preview as a subview of the Preview container and return it.
        previewContainer.addView(mPreview);
        previewInitialized = true;
        return cameraPreview;
    }

    public void cleanCameraPreview() {
        try {
            mPreview.stop();
        } catch (Exception e) {}

        try {
            previewContainer.removeAllViewsInLayout();
        } catch (Exception e) {}

        mPreview = null;
        previewContainer = null;
        previewInitialized = false;
        previewPaused = false;
    }

    public void pauseCameraPreviewIfInitialized() {
        if (mPreview == null || !previewInitialized || previewPaused) return;
        mPreview.stop();
        previewContainer.removeAllViewsInLayout();
        previewPaused = true;
    }

    public void resumeCameraPreviewIfInitialized() {
        if (mPreview == null || !previewInitialized || !previewPaused) return;
        previewPaused = false;
        openCamera();
        startCameraPreviewInView(mPreview.getContext(), previewContainer);
    }
    //endregion

    //region *** Camera & And Preview ***
    /**
     * Opens a camera, and attempts to establish Preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the Preview.
     */

    public void openCamera() {
        Crashlytics.log("openCamera");

        if (mCamera != null) {
            return;
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

        Crashlytics.setInt("Num of cameras", numCameras);

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

        // Try default camera if single camera device (Nexus 7 1st gen fix)
        if (numCameras == 1 && mCamera == null) {
            Crashlytics.log("Last resort: attempting default Camera.open()");
            mCamera = Camera.open(0);
        }

        // Last try if none opened.
        if (mCamera == null) {
            Crashlytics.log("Last resort: attempting default Camera.open()");
            mCamera = Camera.open();
        }

        if (mCamera == null) {
            // Camera should have opened. If not, this is a critical error.
            Crashlytics.log("Failed to open camera :-(");
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        // Preview size
        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // FPS Range
        CameraUtils.chooseBestFPSRange(parms, 25000);

        // More camera settings
        whileUserInteractionCameraSettings(parms);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);

//        parms.setPreviewFormat(ImageFormat.NV21);
        // leave the frame rate set to default
        mCamera.setParameters(parms);


        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;
        String resLog = String.format("Selected camera with preview res: %dx%d", mCameraPreviewWidth, mCameraPreviewHeight);
        Log.d(TAG, resLog);
        Crashlytics.log(resLog);
    }

    public boolean isSelfie() {
        return selfie;
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            if(mPreview != null){
                mPreview.getHolder().removeCallback(mPreview);
            }
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

    public void toastSelectedCamera(Context context) {
        Resources res = context.getResources();
        String cameraTitle;

        if (isSelfie()) {
            cameraTitle = res.getString(R.string.front_camera_title);
        } else {
            cameraTitle = res.getString(R.string.back_camera_title);
        }
        Toast t = Toast.makeText(context, cameraTitle, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.TOP, 0,0);
        t.show();
    }
    //endregion



    /** Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context) {
            super(context);
            mCamera = CameraManager.this.mCamera;
//            Camera.Parameters cameraparams = mCamera.getParameters(); // WORKS OK
//            cameraparams.setPreviewFormat(ImageFormat.NV21); // WORKS OK
//            mCamera.setParameters(cameraparams); // WORKS OK

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            //Start Background Detection
            if(!recorderActivity.isRecording) {
//                backgroundDetection = new BackgroundDetection(recorderContext, mCamera);
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

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //catch only tenth frame
            if(!recorderActivity.isRecording && recorderActivity.itsPreviewTime) {
                if (!recorderActivity.isBackgroundDetectionRunning) {
                    if (everyNthPreviewFrame > 0) {
                        everyNthPreviewFrame--;
                    } else {
                        everyNthPreviewFrame = defaultEveryNthPreviewFrame;
//                        backgroundDetection.RunTestOnFrame(data, camera);
                        recorderActivity.isBackgroundDetectionRunning = true;
                    }
                }
            }
            else{
                recorderActivity.HideWarningButton();
            }

        }

        private void fixPreviewAspectRatio(int containerWidth, int containerHeight) {
            // Crop the video to get the correct aspect ratio + cropping effect
            // (Homage calls it the 16/9 wysiwyg feature)
            Camera.Parameters parameters = mCamera.getParameters();


            Camera.Size previewSize = parameters.getPreviewSize();
            int videoWidth = previewSize.width;
            int videoHeight = previewSize.height;

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
                mCamera.setPreviewCallback(this);
                mCamera.startPreview();
            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

    }







    //region *** Recording ***

/*
                RECORD
               --------

        _____________ _I-I__ __
       /       ____  "-|_|-.\  \
      /  __,--'    `--._ _  \\  \
     /,-'      ,-"-.      `-.\\  \
    /(        (  ^  )       ) \\  \  hjw
   /  `m.__    `-.-'   __.m'  _))  \
  / ____`""mm.______,mm""'   /_/-'  \
 / /___/ === `""""""'    II  `w      \
/_____________________________________\
i_____________________________________i

This camera manager implements two types of video recording.
One uses the limited MediaRecorder (that is also supported by older APIs)
The other is using MediaCodec+MediaMuxer that is supported starting API 18.
The Camera Manager will decide on which method to use based on API and app configuration.
*/

    public static final int RECORDING_METHOD_UNSET = -1;
    public static final int RECORDING_METHOD_MEDIA_RECORDER = 1;
    public static final int RECORDING_METHOD_MEDIA_CODEC = 2;

    public static final int RECORDING_STARTED = 100;
    public static final int RECORDING_FINISHED = 200;
    public static final int RECORDING_SUCCESS = 300;
    public static final int RECORDING_FAILED_TO_START = 400;
    public static final int RECORDING_FAILED = 500;
    public static final int RECORDING_CANCELED = 666;

    private boolean isRecording;
    private RecordingListener recordingListener;
    int duration;

    File outputFile;


    int chosenRecordingMethod = RECORDING_METHOD_UNSET;

    public interface RecordingListener {
        void recordingInfo(int what, File outputFile, HashMap<String, Object> info);
    }

    public boolean isRecording() {
        return isRecording;
    }

    /**
     * chooses recording method and initialize required resources.
     */
    public void chooseRecordingMethod() {
        // Ignore if already recording.
        if (isRecording) {
            Log.e(TAG, "chooseRecordingMethod called in wrong state. Already recording. Ignored.");
            return;
        }

        // Do the selection of the recording method.
        if (recorderUseMediaCodecWhenAvailable) {
            // Choose recording method to be media recorder.
            if (Build.VERSION.SDK_INT >= 18) {
                chosenRecordingMethod = RECORDING_METHOD_MEDIA_CODEC;
            } else {
                chosenRecordingMethod = RECORDING_METHOD_MEDIA_RECORDER;
            }
        } else {
            chosenRecordingMethod = RECORDING_METHOD_MEDIA_RECORDER;
        }
        Log.d(TAG, String.format("Chosen recording method: %s", chosenRecordingMethodDescription()));
    }

    public String chosenRecordingMethodDescription() {
        if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_CODEC) {
            return "MediaCodec+MediaMuxer (API 18+ only)";
        } else if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_RECORDER) {
            return "MediaRecorder";
        } else {
            return "Unsupported method :-(";
        }
    }

    // Cleanup
    public void releaseRecordingManagers() {
        // Release the used media recorder (MediaRecorder or MediaCodec).
        Log.d(TAG, "Release the recorder used.");
    }

    //  Recording
    public void startRecording(int duration, RecordingListener recordingListener) {
        if (isRecording) {
            Log.e(TAG, "startRecording called in wrong state. Already recording. Ignored.");
            return;
        }

        // Init
        this.duration = duration;
        this.recordingListener = recordingListener;

        // Start recording with the chosen method.
        if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_RECORDER) {
            startRecordingWithMediaRecorder();
        } else if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_CODEC) {
            throw new RuntimeException("Unimplemented recording method");
        } else {
            recordingListener.recordingInfo(RECORDING_FAILED_TO_START, null, null);
        }
    }


    public void sendStopRecordingMessageToMainThread() {
        Context context = mPreview.getContext();
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                stopRecording();
            }
        });
    }

    public void stopRecording() {
        if (!isRecording) {
            Log.e(TAG, "stopRecording called in wrong state. Not currently recording. Ignored.");
            return;
        }

        // Stop recording with the chosen method.
        if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_RECORDER) {
            stopRecordingWithMediaRecorder();
        } else if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_CODEC) {
            throw new RuntimeException("Unimplemented recording method");
        } else {
            recordingListener.recordingInfo(RECORDING_FAILED_TO_START, null, null);
        }
    }

    public void cancelRecording() {
        if (!isRecording) {
            Log.e(TAG, "cancelRecording called in wrong state. Not currently recording. Ignored.");
            return;
        }

        // Stop recording with the chosen method.
        if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_RECORDER) {
            cancelRecordingWithMediaRecorder();
        } else if (chosenRecordingMethod == RECORDING_METHOD_MEDIA_CODEC) {
            throw new UnsupportedOperationException("Unimplemented: cancel recording with media codec");
        } else {
            recordingListener.recordingInfo(RECORDING_FAILED, null, null);
        }
    }

    public void sendFinishedRecordingMessageToMainThread(final File outputFile) {
        Context context = mPreview.getContext();
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                recordingListener.recordingInfo(RECORDING_FINISHED, outputFile, null);
                checkFinishedRecording(outputFile);
            }
        });
    }

    public void sendErrorInRecordingMessageToMainThread() {
        Context context = mPreview.getContext();
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                recordingListener.recordingInfo(RECORDING_FAILED, null, null);
            }
        });
    }

    public void checkFinishedRecording(File outputFile) {
        try {
            isRecording = false;

            // Check the output file
            if (!outputFile.exists()) {
                throw new IOException(String.format("Failed saving output file: %s", outputFile));
            }

            String outputFilePath = outputFile.toString();

            Log.d(TAG, String.format("Output file exists: %s", outputFilePath));
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(outputFilePath);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMilliseconds = Long.parseLong( time );
            Log.d(TAG, String.format("Output file length: %d", timeInMilliseconds));

            // Success
            recordingListener.recordingInfo(RECORDING_SUCCESS, outputFile, null);

        } catch (Exception ex) {
            Log.e(TAG, "error in checkFinishedRecording.", ex);
            recordingListener.recordingInfo(RECORDING_FAILED, null, null);
        }
    }
    //endregion

    //region *** MediaRecorder specific ***
    private void startRecordingWithMediaRecorder() {
        if (isRecording) {
            Log.e(TAG, "startRecordingWithMediaRecorder called in wrong state. Already recording. Ignored.");
            return;
        }
        outputFile = null;

        recordingListener.recordingInfo(RECORDING_STARTED, null, null);
        Log.d(TAG, "Will start recording using MediaRecorder");
        isRecording = true;

        //
        // Async listener that is notified while recording (and end of recording).
        //
        final MediaRecorder.OnInfoListener onRecordingInfoListener = new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
            if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED || what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
                sendErrorInRecordingMessageToMainThread();
                return;
            }

            if (what != MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) return;

            Log.d(TAG, String.format("finished (%d %d) recording duration %d", what, extra, duration));
            stopRecordingWithMediaRecorder();
            if (outputFile != null) {
                sendFinishedRecordingMessageToMainThread(outputFile);
            } else {
                recordingListener.recordingInfo(RECORDING_FAILED, null, null);
                Log.e(TAG, "Why missing outputFile path is missing when finishing recording?");
            }
            }
        };

        //
        // Start recording using Media Recorder.
        //
        new AsyncTask<Void, Integer, Void>(){
            @Override
            protected Void doInBackground(Void... arg0) {
                try {
                    outputFile = mediaRecorderRecordToOutputFile(duration, onRecordingInfoListener);
                    if (outputFile == null) {
                        return null;
                    }
                    Log.d(TAG, String.format("Started recording to local file: %s", outputFile));
                } catch (Exception ex) {sendErrorInRecordingMessageToMainThread();}
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
            }
        }.execute((Void)null);
    }

    private File mediaRecorderRecordToOutputFile(int videoDuration, MediaRecorder.OnInfoListener onRecordingInfoListener) {
        //
        // Get high quality camera for the used camera.
        //
        CamcorderProfile profile;
        if (selfie) {
            profile = CamcorderProfile.get(Camera.CameraInfo.CAMERA_FACING_FRONT, CamcorderProfile.QUALITY_HIGH);
        } else {
            profile = CamcorderProfile.get(Camera.CameraInfo.CAMERA_FACING_BACK, CamcorderProfile.QUALITY_HIGH);
        }

        //
        // Camera settings while recording
        //
        Camera.Parameters parameters = mCamera.getParameters();

//        int[] cameraFPSRange = new int[2];
//        parameters.getPreviewFpsRange(cameraFPSRange);

        recSize = parameters.getPreviewSize();
        profile.videoFrameWidth = recSize.width;
        profile.videoFrameHeight = recSize.height;
        profile.videoBitRate = defaultOutputBitRate;

        //whileRecordingCameraSettings(parameters);
        //parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);

        //
        // Unlock the camera so it can be used by the media recorder.
        //
        mCamera.unlock();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(mCamera);

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
        outputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        mediaRecorder.setOutputFile(outputFile.toString());
        // mediaRecorder.setPreviewDisplay(mPreview.mHolder.getSurface());

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

    private void stopRecordingWithMediaRecorder() {
        if (mediaRecorder == null) return;

        try {
            mediaRecorder.stop();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException stopping MediaRecorder", e);
        } catch (RuntimeException e) {
            Log.e(TAG, "failed stopping recording with media recorder.");
        }
        releaseMediaRecorder();
        mCamera.lock();
        isRecording = false;

        Camera.Parameters parameters = mCamera.getParameters();
        whileUserInteractionCameraSettings(parameters);
//        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);
    }

    private void cancelRecordingWithMediaRecorder() {
        if (mediaRecorder == null) return;

        try {
            mediaRecorder.stop();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException stopping MediaRecorder", e);
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to cancel recording with media recorder.");
        }
        releaseMediaRecorder();
        mCamera.lock();
        isRecording = false;

        try {
            Camera.Parameters parameters = mCamera.getParameters();
            whileUserInteractionCameraSettings(parameters);
//            parameters.setPreviewFormat(ImageFormat.NV21);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "failed releasing camera");
        }

        recordingListener.recordingInfo(RECORDING_CANCELED, null, null);
    }

    private void whileUserInteractionCameraSettings(Camera.Parameters parameters) {
        //
        // Camera settings
        //

        // Unlock exposure
        parameters.setAutoExposureLock(false);

        // Unlock focus mode
        if (parameters.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        // Unlock the white balance
        parameters.setAutoWhiteBalanceLock(false);
    }

    public void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            // clear recorder configuration
            try {
                mediaRecorder.stop();
            } catch (Exception e) {}

            mediaRecorder.reset();
            // release the recorder object
            mediaRecorder.release();
            mediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it.
        }

        if (mCamera != null) {
            mCamera.lock();
        }
    }
    //endregion
}
