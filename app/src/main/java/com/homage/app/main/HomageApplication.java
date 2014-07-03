/**

 $$\   $$\  $$$$$$\  $$\      $$\  $$$$$$\   $$$$$$\  $$$$$$$$\
 $$ |  $$ |$$  __$$\ $$$\    $$$ |$$  __$$\ $$  __$$\ $$  _____|
 $$ |  $$ |$$ /  $$ |$$$$\  $$$$ |$$ /  $$ |$$ /  \__|$$ |
 $$$$$$$$ |$$ |  $$ |$$\$$\$$ $$ |$$$$$$$$ |$$ |$$$$\ $$$$$\
 $$  __$$ |$$ |  $$ |$$ \$$$  $$ |$$  __$$ |$$ |\_$$ |$$  __|
 $$ |  $$ |$$ |  $$ |$$ |\$  /$$ |$$ |  $$ |$$ |  $$ |$$ |
 $$ |  $$ | $$$$$$  |$$ | \_/ $$ |$$ |  $$ |\$$$$$$  |$$$$$$$$\
 \__|  \__| \______/ \__|     \__|\__|  \__| \______/ \________|

                   Android Application Class
 */
package com.homage.app.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.util.Log;

import com.amazonaws.services.s3.transfer.Upload;
import com.homage.media.camera.CameraManager;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.homage.networking.uploader.UploadManager;
import com.homage.networking.uploader.UploaderService;
import com.orm.SugarApp;

import java.util.logging.Logger;


public class HomageApplication extends SugarApp {
    String TAG = "TAG_" + getClass().getName();

    public static final String SETTINGS_NAME = "homage_settings";

    public static final String SK_EMAIL = "email";
    public static final String SK_PASSWORD = "password";

    private static Context instance;

    private UploadManager uploadManager;

    public HomageApplication() {
        super();
        instance = this;
        uploadManager = UploadManager.sh();
    }

    public static HomageApplication getInstance() {
        return (HomageApplication)instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Started Homage android application.");

        // Get preferences
        SharedPreferences p = getSettings(this);

        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();
        Thread.setDefaultUncaughtExceptionHandler(new HomageUnhandledExceptionHandler());

        // Upload service
        UploadManager.sh().checkUploader();
    }

    protected void initSingletons() {
        // Camera manger
        CameraManager.sh().init(this);

        // Homage Server
        HomageServer.sh().init(this);
    }

    public static Context getContext() {
        return instance;
    }

    public static SharedPreferences getSettings(Context context) {
        return context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
    }
    //region *** Unhandled exceptions ***

    public String getVersionName() {
        String versionName;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "?";
        }
        return versionName;
    }

    private class HomageUnhandledExceptionHandler implements Thread.UncaughtExceptionHandler {
        /*
         * Unhandled exceptions custom handler.
         * This is used for debugging and for making sure the camera is released.
         * The app will still crash with the default expected OS behaviour.
         * IMPORTANT: Please don't add ANY error handling hacks here!
         */
        private Thread.UncaughtExceptionHandler mDefaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // Log the top of the stack trace
            try {
                StackTraceElement element = ex.getStackTrace()[0];
                Log.e(TAG, String.format("App crashed. %s %s %d", ex.getMessage(), element.getClassName(), element.getLineNumber()));
            } catch(Exception uex) {}

            CameraManager.sh().releaseMediaRecorder();
            CameraManager.sh().releaseCamera();
            mDefaultHandler.uncaughtException(thread, ex);
        }

    }

    //endregion
}