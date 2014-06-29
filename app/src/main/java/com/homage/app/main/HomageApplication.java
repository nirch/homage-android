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

import com.homage.media.camera.CameraManager;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.homage.networking.uploader.UploaderService;
import com.orm.SugarApp;

import java.util.logging.Logger;


public class HomageApplication extends SugarApp {
    String TAG = "TAG_" + getClass().getName();

    public static final String SETTINGS_NAME = "homage_settings";

    public static final String SK_EMAIL = "email";
    public static final String SK_PASSWORD = "password";

    private static Context instance;

    public HomageApplication() {
        super();
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Started Homage android application.");

        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();
        Thread.setDefaultUncaughtExceptionHandler(new HomageUnhandledExceptionHandler());

        // Logout all users
        User.logoutAllUsers();

        // Start the background upload service
        Intent intent = new Intent(this, UploaderService.class);
        startService(intent);
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

    public static SharedPreferences getSettings(Activity activity) {
        return activity.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
    }
    //region *** Unhandled exceptions ***

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