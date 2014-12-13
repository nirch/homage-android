package com.homage.app.player;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.crashlytics.android.Crashlytics;
import com.homage.CustomAdapters.GestureListener;
import com.homage.app.R;
import com.homage.model.Remake;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;

import java.util.HashMap;

public class RemakeVideoActivity extends
        Activity
        implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnInfoListener, View.OnTouchListener{
    static final String TAG = "TAG_RemakeVideoFragment";

    // Settings
    public static final String K_FILE_PATH = "videoFilePath";
    public static final String K_FILE_URL = "videoFileURL";
    public static final String K_ALLOW_TOGGLE_FULLSCREEN = "allowToggleFullscreen";
    public static final String K_FINISH_ON_COMPLETION = "finishOnCompletion";
    public static final String K_AUTO_START_PLAYING = "autoStartPlaying";
    public static final String K_IS_EMBEDDED = "isEmbedded";
    public static final String K_THUMB_URL = "thumbURL";
    public static final String K_THUMB_DRAWABLE_ID = "thumbDrawableId";

    //    Gesture stuff
    private GestureDetector mGestureDetector;

    private Runnable onFinishedPlayback;
    private Runnable onStartedPlayback;

    public void setOnFinishedPlayback(Runnable r) {
        this.onFinishedPlayback = r;
    }

    public void setOnStartedPlayback(Runnable r) {
        this.onStartedPlayback = r;
    }

    // Video file path / url
    String filePath;
    String fileURL;
    String thumbURL;
    String entityID;
    int entityType;
    int originatingScreen;
    int thumbDrawableId;
    Remake remake;
    ImageButton isLiked;
    TextView viewsCount;

    // More settings
    boolean allowToggleFullscreen = false;
    boolean finishOnCompletion = false;
    boolean autoHideControls = true;
    boolean autoStartPlaying = true;
    boolean isEmbedded = false;

    // Info
    HashMap<String, Object> info;
    long initTime;

    // Views & Layout
    AQuery aq;
    View rootView;
    VideoView videoView;
    LayoutInflater inflater;
    boolean alreadyGotSettings = false;

    //region *** lifecycle ***
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_remake_video_view);
        aq = new AQuery(this);

        // Bind the gestureDetector to GestureListener
        mGestureDetector = new GestureDetector(this, new GestureListener());

//      <-- VideoView Stuff-->
        videoView = (VideoView) aq.id(R.id.videoView).getView();
        videoView.setOnTouchListener(this);
        isLiked = (ImageButton)aq.id(R.id.likedButton).getView();


        // Get the the file path / url of the video.
        if (alreadyGotSettings) {
            // Settings already set. Initalize.
            initialize();
//            return rootView;
        }
        else
        {
            initializeWithArguments();
        }

        //        <--Liked&Views-->

        isLiked.setSelected(remake.isLiked);
        aq.id(R.id.views_count).getTextView().setText(String.valueOf(remake.viewsCount));


        // Try to get settings arguments from activity intent extras.
//        Activity parent = this;
//        Bundle b = this.getIntent().getExtras();
//        if (b == null) return rootView;

        // We have the arguments. Initialize.

//        return rootView;

    }

    //region *** initializations ***
    private void initializeUIState() {
        if (!allowToggleFullscreen) {
            disableButton(R.id.videoFullScreenButton);
        }

        if (autoStartPlaying) {
            aq.id(R.id.videoThumbnailImage).visibility(View.GONE);
            aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
        }

        if (thumbURL != null) {
            aq.id(R.id.videoThumbnailImage).image(
                    thumbURL,
                    true,
                    true,
                    200,
                    R.drawable.glass_dark,
                    null, AQuery.FADE_IN);
        } else if (thumbDrawableId > 0) {
            aq.id(R.id.videoThumbnailImage).image(thumbDrawableId);
        } else {
            aq.id(R.id.videoThumbnailImage).visibility(View.GONE);
        }

        showThumbWhileLoading();
        showControls();
    }

//    public void setArguments() {
//        initializePlayerSettings();
//    }

    public void initializeWithArguments() {
        initObservers();
        initializePlayerSettings();
        initializeUIState();
        initializePlayingVideo();
        bindUIHandlers();
    }

    public void initialize() {
        initializeUIState();
        initializePlayingVideo();
        bindUIHandlers();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            fullStop();
            removeObservers();
        } catch (Exception ex) {
        }
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onRemakeLiked);
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        if (isEmbedded) handleEmbeddedVideoConfiguration(newConfig);
//    }

    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onRemakeLiked, new IntentFilter((HomageServer.INTENT_REMAKE_LIKED)));
    }

    private void initializePlayingVideo() {
        // Initialize playing the video
        pause();
        initializeVideoPlayer();
    }


    private void disableButton(int buttonId) {
        ImageButton ib = (ImageButton) aq.id(buttonId).getView();
        ib.setAlpha(0.2f);
        ib.setImageResource(R.drawable.icon_small_player_disabled);
    }

    private void initializeVideoPlayer() {

        if (filePath != null) {

            // A local video file.
            videoView.setVideoPath(filePath);

        } else if (fileURL != null) {

            // A remote video with a given URL.
            videoView.setVideoURI(Uri.parse(fileURL));
            videoView.start();
        }


        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnCompletionListener(this);
        if (Build.VERSION.SDK_INT > 16) {
            // Only available in API 17 and up.
            videoView.setOnInfoListener(this);
        }
    }
    //endregion

    //region *** Media player ***
    private void initializePlayerSettings() {
        Intent b = getIntent();
        info = new HashMap<String, Object>();
        initTime = System.currentTimeMillis();
        filePath = b.getStringExtra(K_FILE_PATH);
        entityType = b.getIntExtra(HEvents.HK_VIDEO_ENTITY_TYPE, 0);
        entityID = b.getStringExtra(HEvents.HK_VIDEO_ENTITY_ID);
        remake = Remake.findByOID(entityID);
        originatingScreen = b.getIntExtra(HEvents.HK_VIDEO_ORIGINATING_SCREEN, 0);

        if (filePath == null) {
            fileURL = b.getStringExtra(K_FILE_URL);
            if (fileURL == null) return;
            info.put(HEvents.HK_VIDEO_FILE_URL, fileURL);
        } else {
            info.put(HEvents.HK_VIDEO_FILE_PATH, filePath);
        }
        info.put(HEvents.HK_VIDEO_INIT_TIME, initTime);
        info.put(HEvents.HK_VIDEO_ENTITY_TYPE, entityType);
        info.put(HEvents.HK_VIDEO_ENTITY_ID, entityID);

        info.put(HEvents.HK_VIDEO_ORIGINATING_SCREEN, originatingScreen);

        // More settings
        allowToggleFullscreen = b.getBooleanExtra(K_ALLOW_TOGGLE_FULLSCREEN, true);
        finishOnCompletion = b.getBooleanExtra(K_FINISH_ON_COMPLETION, false);
        autoStartPlaying = b.getBooleanExtra(K_AUTO_START_PLAYING, true);
        isEmbedded = b.getBooleanExtra(K_IS_EMBEDDED, false);
        thumbURL = b.getStringExtra(K_THUMB_URL);
        thumbDrawableId = b.getIntExtra(K_THUMB_DRAWABLE_ID, 0);

        Log.d(TAG, String.format("Will play video in fragment: %s %s", filePath, fileURL));
        alreadyGotSettings = true;

        if (autoStartPlaying) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_WILL_AUTO_PLAY, info);
        }
    }



    // Bind to UI events
    private void bindUIHandlers() {
//        TODO create new touch handler
        aq.id(R.id.touchVideoButton).clicked(onClickedToggleControlsButton);
        aq.id(R.id.videoStopButton).clicked(onClickedStopButton);
        aq.id(R.id.videoPlayPauseButton).clicked(onClickedPlayPauseButton);
        aq.id(R.id.videoFullScreenButton).clicked(onClickedFullScreenButton);
        aq.id(R.id.videoBigPlayButton).clicked(onClickedBigPlayButton);
        aq.id(R.id.likedButton).clicked(onClickedLikeButton);
    }

    // get Like refresh
    private BroadcastReceiver onRemakeLiked = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Crashlytics.log("onRemakeLiked");

            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);
            if(success) {
                HashMap<String, Object> responseInfo = (HashMap<String, Object>) intent.getSerializableExtra(Server.SR_RESPONSE_INFO);

                if (responseInfo != null) {
                    String remakeOID = (String) responseInfo.get("remakeOID");
                    String statuscode = responseInfo.get("status_code").toString();
                    if (remakeOID.equals(remake.getOID()) && statuscode.equals("200")) {
                        if (remakeOID == null) return;
                        isLiked.setSelected(!isLiked.isSelected());
                        remake.isLiked = !remake.isLiked;
                        remake.save();
                    }
                }
            }
        }
    };

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, String.format("Finished playing video: %s", filePath));
        videoView.pause();
        if (onFinishedPlayback != null) {
            info.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
            info.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_FINISH, info);
            new Handler().post(onFinishedPlayback);

        }
        if (finishOnCompletion) {
            this.finish();
        } else {
            showThumbState();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Log.d(TAG, String.format("Error playing video: %d %d %s %s", i, i2, filePath, fileURL));

        Toast.makeText(
                this,
                String.format("Video playing error %d %d", i, i2),
                Toast.LENGTH_SHORT).show();

        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, String.format("Video is prepared for playing: %s %s", filePath, fileURL));
        aq.id(R.id.videoCurtain).visibility(View.GONE);
        if (autoStartPlaying) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_WILL_PLAY, info);
            aq.id(R.id.loadingVideoPprogress).visibility(View.GONE);
            aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
            start();
        }
    }
    //endregion

    //region *** Controls ***
    void showControls() {
        aq.id(R.id.videoControls).visibility(View.VISIBLE);

        if (autoHideControls) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideControls();
                }
            }, 5000);
        }
    }

    void hideControls() {
        aq.id(R.id.videoControls).visibility(View.GONE);
    }

    void toggleControls() {
        if (aq.id(R.id.videoControls).getView().getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    void togglePlayPause() {
        if (videoView.isPlaying()) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_PAUSE, info);
            pause();
        } else {
            start();
        }
    }

    void pause() {
        ImageButton ib = (ImageButton) aq.id(R.id.videoPlayPauseButton).getView();
        ib.setImageResource(R.drawable.selector_video_button_play);
        if (videoView != null) videoView.pause();
    }

    void start() {
        ImageButton ib = (ImageButton) aq.id(R.id.videoPlayPauseButton).getView();
        ib.setImageResource(R.drawable.selector_video_button_pause);
        aq.id(R.id.videoView).visibility(View.VISIBLE);
        if (videoView != null) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYING, info);
            videoView.seekTo(1);
            videoView.start();
            aq.id(R.id.videoThumbnailImage).visibility(View.INVISIBLE);
        }
        if (onStartedPlayback != null) {
            new Handler().post(onStartedPlayback);
        }
    }



    void showThumbState() {
        if (videoView != null) videoView.seekTo(0);
        pause();
        aq.id(R.id.videoThumbnailImage).visibility(View.VISIBLE);
        aq.id(R.id.videoBigPlayButton).visibility(View.VISIBLE);
        aq.id(R.id.videoView).visibility(View.INVISIBLE);
        aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
    }

    void showThumbWhileLoading() {
        aq.id(R.id.videoThumbnailImage).visibility(View.VISIBLE);
    }

    void hideThumb() {
        aq.id(R.id.videoThumbnailImage).visibility(View.INVISIBLE);
        aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
    }
    //endregion

    //region *** video commands ***
    public void fullStop() {
        HashMap<String, Object> eInfo = new HashMap<String, Object>(info);
        eInfo.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
        eInfo.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());

        videoView.seekTo(0);
        videoView.pause();

        HEvents.sh().track(HEvents.H_EVENT_VIDEO_FULL_STOP, eInfo);
        if (onFinishedPlayback != null) {
            new Handler().post(onFinishedPlayback);
        }
        if (finishOnCompletion) {
            RemakeVideoActivity.this.finish();
            return;
        }
        showThumbState();
    }
    //endregion

    //region *** UI event handlers ***
    /**
     * ==========================
     * UI event handlers.
     * ==========================
     */

    View.OnClickListener onClickedToggleControlsButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RemakeVideoActivity.this.toggleControls();
        }
    };

    View.OnClickListener onClickedStopButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            HashMap<String, Object> eInfo = new HashMap<String, Object>(info);
            eInfo.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
            eInfo.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_STOP, eInfo);

            fullStop();
        }
    };

    View.OnClickListener onClickedPlayPauseButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            togglePlayPause();
        }
    };

    View.OnClickListener onClickedBigPlayButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideThumb();
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_PLAY, info);
            aq.id(R.id.loadingVideoPprogress).visibility(View.VISIBLE);
            videoView.seekTo(0);
            start();
        }
    };

    View.OnClickListener onClickedLikeButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            HomageServer.sh().reportUserLikedRemake(remake.getOID(),!remake.isLiked);
        }
    };




    View.OnClickListener onClickedFullScreenButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                Log.v(TAG, "media track lagging");
                break;

            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.v(TAG, "media buffering start");
                HEvents.sh().track(HEvents.H_EVENT_VIDEO_BUFFERING_START, info);
                aq.id(R.id.videoFragmentLoading).visibility(View.VISIBLE);
                break;

            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                Log.v(TAG, "media rendering start");
                aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
                break;

            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.v(TAG, "media buffering end");
                HEvents.sh().track(HEvents.H_EVENT_VIDEO_BUFFERING_END, info);
                aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
                break;

            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                Log.v(TAG, "media bad interleaving");
                break;

            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                Log.v(TAG, "media not seekable");
                break;

            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                Log.v(TAG, "media meta data update");
                break;

            case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                Log.v(TAG, "media unsupported subtitle");
                break;

            case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                Log.v(TAG, "media subtitle timed out");
                break;
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }


//endregion
}