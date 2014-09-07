package com.homage.app.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.androidquery.AQuery;
import com.crashlytics.android.Crashlytics;
import com.homage.app.BuildConfig;
import com.homage.app.R;
import com.homage.app.user.LoginActivity;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.views.ActivityHelper;

import org.bson.types.ObjectId;

import java.util.HashMap;

public class SplashScreenActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 1000;

    AQuery aq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String TAG = "TAG_SplashScreenActivity";

        super.onCreate(savedInstanceState);

        if (!BuildConfig.DEBUG) {
            Log.d(TAG, "Will start crashlytics");
            Crashlytics.start(this);
        } else {
            Log.d(TAG, "Will not start crashlytics");
        }

        setContentView(R.layout.activity_splash_screen);
        aq = new AQuery(this);

        // Hide the systems bars (soft navigation bar and status bar)
        ActivityHelper.hideSystemBars(this);
        HMixPanel.sh().track("AppLaunch",null);

        final User user = User.getCurrent();
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
            if (user == null) {
                Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                intent.putExtra(LoginActivity.SK_ALLOW_GUEST_LOGIN, true);
                intent.putExtra(LoginActivity.SK_START_MAIN_ACTIVITY_AFTER_LOGIN, true);
                SplashScreenActivity.this.startActivity(intent);
                SplashScreenActivity.this.finish();
                overridePendingTransition(0, 0);
            } else {
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                SplashScreenActivity.this.startActivity(intent);
                SplashScreenActivity.this.finish();
                overridePendingTransition(0, 0);
            }
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}