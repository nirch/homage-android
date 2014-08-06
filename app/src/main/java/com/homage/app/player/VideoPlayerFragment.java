package com.homage.app.player;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.networking.analytics.HEvents;

import java.lang.reflect.Field;
import java.util.HashMap;

public class VideoPlayerFragment
        extends
            Fragment
        implements
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnInfoListener
{
    static final String TAG = "TAG_VideoPlayerFragment";

    // Settings
    public static final String K_FILE_PATH = "videoFilePath";
    public static final String K_FILE_URL = "videoFileURL";
    public static final String K_ALLOW_TOGGLE_FULLSCREEN = "allowToggleFullscreen";
    public static final String K_FINISH_ON_COMPLETION = "finishOnCompletion";
    public static final String K_AUTO_START_PLAYING = "autoStartPlaying";
    public static final String K_IS_EMBEDDED = "isEmbedded";
    public static final String K_THUMB_URL = "thumbURL";
    public static final String K_THUMB_DRAWABLE_ID = "thumbDrawableId";

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
    int thumbDrawableId;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_homage_video_view, container, false);
        aq = new AQuery(rootView);
        videoView = (VideoView)aq.id(R.id.videoView).getView();


        // Get the the file path / url of the video.
        if (alreadyGotSettings) {
            // Settings already set. Initalize.
            initialize();
            return rootView;
        }

        // Try to get settings arguments from activity intent extras.
        Bundle b = getActivity().getIntent().getExtras();
        if (b == null) return rootView;

        // We have the arguments. Initialize.
        initializeWithArguments(b);
        return rootView;
    }

    public void setArguments(Bundle b) {
        initializePlayerSettings(b);
    }

    public void initializeWithArguments(Bundle b) {
        initializePlayerSettings(b);
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
        fullStop();
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        if (isEmbedded) handleEmbeddedVideoConfiguration(newConfig);
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    //endregion

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

    private void initializePlayingVideo() {
        // Initialize playing the video
        pause();
        initializeVideoPlayer();
    }


    private void disableButton(int buttonId) {
        ImageButton ib = (ImageButton)aq.id(buttonId).getView();
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
    private void initializePlayerSettings(Bundle b) {
        info = new HashMap<String, Object>();
        initTime = System.currentTimeMillis();
        filePath = b.getString(K_FILE_PATH);
        if (filePath == null) {
            fileURL = b.getString(K_FILE_URL);
            if (fileURL == null) return;
            info.put(HEvents.HK_VIDEO_FILE_URL, fileURL);
        } else {
            info.put(HEvents.HK_VIDEO_FILE_PATH,filePath);
        }
        info.put(HEvents.HK_VIDEO_INIT_TIME, initTime);

        // More settings
        allowToggleFullscreen = b.getBoolean(K_ALLOW_TOGGLE_FULLSCREEN, true);
        finishOnCompletion = b.getBoolean(K_FINISH_ON_COMPLETION, false);
        autoStartPlaying = b.getBoolean(K_AUTO_START_PLAYING, true);
        isEmbedded = b.getBoolean(K_IS_EMBEDDED, false);
        thumbURL = b.getString(K_THUMB_URL, null);
        thumbDrawableId = b.getInt(K_THUMB_DRAWABLE_ID, 0);

        Log.d(TAG, String.format("Will play video in fragment: %s %s", filePath, fileURL));
        alreadyGotSettings = true;

        if (autoStartPlaying) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_WILL_AUTO_PLAY, info);
        }
    }

    // Bind to UI events
    private void bindUIHandlers() {
        aq.id(R.id.touchVideoButton).clicked(onClickedToggleControlsButton);
        aq.id(R.id.videoStopButton).clicked(onClickedStopButton);
        aq.id(R.id.videoPlayPauseButton).clicked(onClickedPlayPauseButton);
        aq.id(R.id.videoFullScreenButton).clicked(onClickedFullScreenButton);
        aq.id(R.id.videoBigPlayButton).clicked(onClickedBigPlayButton);

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, String.format("Finished playing video: %s", filePath));
        videoView.pause();
        if (onFinishedPlayback != null) {
            new Handler().post(onFinishedPlayback);
        }
        if (finishOnCompletion) {
            getActivity().finish();
        } else {
            showThumbState();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Log.d(TAG, String.format("Error playing video: %d %d %s %s", i, i2, filePath, fileURL));

        Toast.makeText(
                getActivity(),
                String.format("Video playing error %d %d", i, i2),
                Toast.LENGTH_SHORT).show();

        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, String.format("Video is prepared for playing: %s %s", filePath, fileURL));
        aq.id(R.id.videoCurtain).visibility(View.GONE);
        aq.id(R.id.videoThumbnailImage).visibility(View.INVISIBLE);
        if (autoStartPlaying) {
            start();
        }

        //aq.id(R.id.loadingVideoPprogress).visibility(View.GONE);
        //aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
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
            }, 2000);
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
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_PLAY, info);
            start();
        }
    }

    void pause() {
        ImageButton ib = (ImageButton)aq.id(R.id.videoPlayPauseButton).getView();
        ib.setImageResource(R.drawable.selector_video_button_play);
        if (videoView != null) videoView.pause();
    }

    void start() {
        ImageButton ib = (ImageButton)aq.id(R.id.videoPlayPauseButton).getView();
        ib.setImageResource(R.drawable.selector_video_button_pause);
        aq.id(R.id.videoView).visibility(View.VISIBLE);
        if (videoView != null) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYING, info);
            videoView.start();
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
    void fullStop() {
        videoView.seekTo(0);
        videoView.pause();
        if (onFinishedPlayback != null) {
            new Handler().post(onFinishedPlayback);
        }
        if (finishOnCompletion) {
            VideoPlayerFragment.this.getActivity().finish();
            return;
        }
        showThumbState();
    }
    //endregion

    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */

    View.OnClickListener onClickedToggleControlsButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            VideoPlayerFragment.this.toggleControls();
        }
    };

    View.OnClickListener onClickedStopButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            HashMap<String, Object> eInfo = new HashMap<String, Object>(info);
            eInfo.put(HEvents.HK_VIDEO_POSITION, videoView.getCurrentPosition());
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
    //endregion
}
