package com.homage.app.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.views.ActivityHelper;

public class WelcomeScreenActivity extends FragmentActivity {

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
        }
    };
    //endregion

}