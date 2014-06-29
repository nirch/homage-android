package com.homage.app.player;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.views.ActivityHelper;

public class FullScreenVideoPlayerActivity extends Activity
{
    String TAG = "TAG_FullScreenVideoPlayerActivity";
    AQuery aq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Load the context
        setContentView(R.layout.activity_full_screen_homage_video);
        aq = new AQuery(this);
    }
}
