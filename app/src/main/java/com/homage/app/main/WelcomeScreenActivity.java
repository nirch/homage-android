package com.homage.app.main;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.views.ActivityHelper;
import com.homage.networking.mixpanel.HMixPanel;

import java.util.HashMap;

public class WelcomeScreenActivity extends FragmentActivity {
    static final String TAG = "TAG_WelcomeScreenActivity";

    AQuery aq;

    //region *** Lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.goFullScreen(this);
        setContentView(R.layout.activity_welcome);

        aq = new AQuery(this);

        Uri videoURL = Uri.parse("android.resource://com.homage.app/raw/intro_video");
        Bundle b = new Bundle();
        b.putBoolean(VideoPlayerFragment.K_AUTO_START_PLAYING, true);
        b.putBoolean(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
        b.putString(VideoPlayerFragment.K_FILE_URL, videoURL.toString());

        FragmentManager fm = getSupportFragmentManager();
        VideoPlayerFragment videoPlayerFragment = (VideoPlayerFragment)fm.findFragmentById(R.id.videoPlayerFragment);
        videoPlayerFragment.initializeWithArguments(b);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.letsCreateButton).clicked(onClickedLetsCreateButton);
        //endregion
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleEmbeddedVideoConfiguration(newConfig);
    }

    //region *** handle embedded video orientation change ***
    private void handleEmbeddedVideoConfiguration(Configuration cfg) {
        int orientation = cfg.orientation;

        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                enterFullScreen();
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                exitFullScreen();
                break;
        }
    }

    void enterFullScreen() {
        Log.v(TAG, "Video, change to full screen");

        aq.id(R.id.letsCreateButton).visibility(View.INVISIBLE);

        View container = aq.id(R.id.welcomeVidoeContainer).getView();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) container.getLayoutParams();
        params.height = metrics.heightPixels;
        container.setLayoutParams(params);
    }

    void exitFullScreen() {
        Log.v(TAG, "Video, exit full screen");

        aq.id(R.id.letsCreateButton).visibility(View.VISIBLE);

        View container = aq.id(R.id.welcomeVidoeContainer).getView();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) container.getLayoutParams();
        int m = (int)(216.0f*metrics.density);
        params.height = m;
        container.setLayoutParams(params);
    }
    //endregion

    //region *** UI event handlers ***
    /**
     * ==========================
     * UI event handlers.
     * ==========================
     */
    private View.OnClickListener onClickedLetsCreateButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent startIntent = new Intent(WelcomeScreenActivity.this, MainActivity.class);
            WelcomeScreenActivity.this.startActivity(startIntent);
            overridePendingTransition(0, 0);
            WelcomeScreenActivity.this.finish();
            HMixPanel.sh().track("pushed lets create");
        }
    };
    //endregion

}