package com.homage.app.player;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.model.Remake;
import com.homage.networking.analytics.HEvents;
import com.homage.views.ActivityHelper;

public class FullScreenRemakePlayerActivity extends FragmentActivity
{
    String TAG = "TAG_FullScreenVideoPlayerActivity";
    AQuery aq;
    Remake remake;
    TextView views_count;
    TextView likes_count;

    static public void openFullScreenVideoForURL(Activity activity, String url, String thumbURL, int entityType, String entityID, int originatingScreen, boolean finishOnCompletion) {
        try {
            Intent myIntent = new Intent(activity, FullScreenRemakePlayerActivity.class);
            Uri videoURL = Uri.parse(url);
            myIntent.putExtra(VideoPlayerFragment.K_FILE_URL, videoURL.toString());
            myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
            myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, finishOnCompletion);
            myIntent.putExtra(VideoPlayerFragment.K_THUMB_URL, thumbURL);

            if (entityID == null) {
                myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_ID, "");
            } else {
                myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_ID, entityID);
            }

            myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_TYPE, entityType);
            myIntent.putExtra(HEvents.HK_VIDEO_ORIGINATING_SCREEN, originatingScreen);

            activity.startActivity(myIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Video unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    static public void openFullScreenVideoForFile(Activity activity, String filePath, int entityType, String entityID, int originatingScreen, boolean finishOnCompletion) {
        try {
            Intent myIntent = new Intent(activity, FullScreenRemakePlayerActivity.class);
            myIntent.putExtra(VideoPlayerFragment.K_FILE_PATH, filePath);
            myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
            myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, finishOnCompletion);

            if (entityID == null) {
                myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_ID, "");
            } else {
                myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_ID, entityID);
            }

            myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_TYPE, entityType);
            myIntent.putExtra(HEvents.HK_VIDEO_ORIGINATING_SCREEN, originatingScreen);

            activity.startActivity(myIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Video unavailable.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        String remakeID = b.getString(HEvents.HK_VIDEO_ENTITY_ID);
        remake =  Remake.findByOID(remakeID);

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Hide the systems bars (soft navigation bar and status bar)
        ActivityHelper.hideSystemBars(this);

        // Load the context
        setContentView(R.layout.activity_full_screen_remake_video);
        aq = new AQuery(this);

        views_count = aq.id(R.id.views_count).getTextView();
        likes_count = aq.id(R.id.likes_count).getTextView();

        views_count.setText(remake.viewsCount);
        likes_count.setText(remake.likesCount);


    }
}
