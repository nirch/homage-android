package com.homage.networking.uploader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.homage.app.main.HomageApplication;
import com.homage.app.main.SettingsActivity;

public class NetworkStateReceiver extends BroadcastReceiver {
    String TAG = "TAG_"+getClass().getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Network connectivity change: " + intent.getAction());
        UploadManager.sh().checkUploader();
    }
}