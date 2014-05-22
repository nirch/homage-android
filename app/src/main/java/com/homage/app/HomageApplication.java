package com.homage.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.homage.media.CameraManager;

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
        // The camera manager singleton
        DisplayMetrics dm = getResources().getDisplayMetrics();

        Resources res = getResources();
        CameraManager.getInstance().init(
                res.getInteger(R.integer.cameraPreferredRecodingWidth),
                res.getInteger(R.integer.cameraPreferredRecodingHeight),
                dm.widthPixels,
                dm.heightPixels);
    }

    public void customAppMethod()
    {
        // Custom application method
    }
}