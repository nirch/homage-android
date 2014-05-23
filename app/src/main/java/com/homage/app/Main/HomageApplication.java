package com.homage.app.Main;

import android.app.Application;
import android.util.Log;

import com.homage.media.CameraManager;
import com.homage.networking.server.HomageServer;

public class HomageApplication extends Application
{
    String TAG = getClass().getName();

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Started Homage android application.");

        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();
    }

    protected void initSingletons()
    {
        // Camera manger
        CameraManager.sh().init(this);

        // Homage Server
        HomageServer.sh().init(this);

        // Homage Model
        // ?
    }

//    private void initUncaughtExceptionsHandler() {
////        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
////            @Override
////            public void uncaughtException(Thread thread, final Throwable ex) {
////                Log.d(TAG, ":-(");
////
////                // Release the camera.
////                try {
////                    CameraManager cm = CameraManager.getInstance();
////                    cm.releaseMediaRecorder();
////                    cm.releaseCamera();
////                }
////                catch (Exception ignoredException) {
////
////                }
////
////                // throw it again
////                throw (RuntimeException) ex;
////            }
////        });
//    }
}