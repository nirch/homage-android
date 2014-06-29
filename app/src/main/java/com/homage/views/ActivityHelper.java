package com.homage.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.Window;
import android.view.WindowManager;

import com.homage.app.R;

public class ActivityHelper {
    public static void goFullScreen(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }

    public static void unimplementedMessage(Context context) {
        ActivityHelper.message(
                context,
                R.string.debug_unimplemented_title,
                R.string.debug_unimplemented_description
        );
    }

    public static void message(Context context, int titleId, int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        builder.setNegativeButton(R.string.debug_ok, null);
        builder.create().show();
    }



}
