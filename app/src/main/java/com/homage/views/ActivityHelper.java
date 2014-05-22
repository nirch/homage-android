package com.homage.views;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

public class ActivityHelper {
    public static void goFullScreen(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }
}
