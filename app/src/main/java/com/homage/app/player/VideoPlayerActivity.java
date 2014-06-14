package com.homage.app.player;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.views.ActivityHelper;

public class VideoPlayerActivity extends Activity
        implements
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener

{
    String TAG = "TAG_" + getClass().getName();
    AQuery aq;

    String filePath;
    String fileURL;

    VideoView videoView;
    MyMediaController mediaController;

    //region *** Activity lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the video player activity.");

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Set the content layout
        setContentView(R.layout.activity_video_player);

        aq = new AQuery(this);

        // Get the name of the file to play.
        Bundle b = getIntent().getExtras();
        filePath = b.getString("videoFilePath");
        if (filePath == null) {
            fileURL = b.getString("videoFileURL");
            if (fileURL == null) {
                finish();
                return;
            }
        }

        // Init video
        initVideoView();

        // Handle UI
        aq.id(R.id.videoPlayerDismissButton).clicked(onClickedDismissButton);
    }

    private void initVideoView() {
        mediaController = new MyMediaController(this, false);

        videoView = (VideoView)aq.id(R.id.videoView).getView();

        if (filePath != null) {
            videoView.setVideoPath(filePath);
        } else if (fileURL != null) {
            videoView.setVideoURI(Uri.parse(fileURL));
        }

        videoView.setMediaController(mediaController);
        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnCompletionListener(this);
    }

    private class MyMediaController extends MediaController {
        public MyMediaController(Context context) {
            super(context);
        }

        public MyMediaController(Context context, boolean showNextPrev) {
            super(context, showNextPrev);
        }

        @Override
        public void hide() {
            super.hide();
            aq.id(R.id.videoPlayerDismissButton).visibility(View.GONE);
        }

        @Override
        public void show() {
            this.show(0);
            aq.id(R.id.videoPlayerDismissButton).visibility(View.VISIBLE);
        }

        @Override
        public void show(int timeOut) {
            super.show(timeOut);
            aq.id(R.id.videoPlayerDismissButton).visibility(View.VISIBLE);
        }
    }

    //region *** Video events handlers ***
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, String.format("Finished playing video: %s", filePath));
        videoView.seekTo(0);
        videoView.pause();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Log.d(TAG, String.format("Error playing video: %s", filePath));
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, String.format("Video is prepared for playing: %s", filePath));
        aq.id(R.id.loadingVideoPprogress).visibility(View.GONE);
        aq.id(R.id.videoCurtain).visibility(View.GONE);
        videoView.start();
        mediaController.hide();
    }
    //endregion

    private View.OnClickListener onClickedDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            videoView.pause();
            finish();
        }
    };


}
