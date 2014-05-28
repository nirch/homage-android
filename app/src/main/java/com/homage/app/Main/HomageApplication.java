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

import android.content.Context;
import android.util.Log;

import com.homage.media.camera.CameraManager;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.orm.SugarApp;


public class HomageApplication extends SugarApp {
    private static Context instance;
    String TAG = "TAG_" + getClass().getName();

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

        // DEBUG user
        User.logoutAllUsers();
        HomageServer.sh().loginUser("android@test.com","123456");



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

    //region *** Unhandled exceptions ***

    /**
     * Unhandled exceptions custom handler.
     * This is used for debugging and for making sure the camera is released.
     * The app will still crash with the default expected OS behaviour.
     * IMPORTANT: Please don't add ANY error handling hacks here!
     */
    private class HomageUnhandledExceptionHandler implements Thread.UncaughtExceptionHandler {
        private Thread.UncaughtExceptionHandler mDefaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // Log the top of the stack trace
            try {
                StackTraceElement element = ex.getStackTrace()[0];
                Log.e(TAG, String.format("App crashed. %s %s %d", ex.getMessage(), element.getClassName(), element.getLineNumber()));
            } catch(Exception uex) {}

            // TODO: release camera here.

            mDefaultHandler.uncaughtException(thread, ex);
        }

    }
}