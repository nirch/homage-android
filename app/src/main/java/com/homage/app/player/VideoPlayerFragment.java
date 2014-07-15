package com.homage.app.player;

import android.app.Activity;
import android.app.Fragment;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.homage.app.R;

public class VideoPlayerFragment
        extends
            Fragment
        implements
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener
{
    String TAG = "TAG_VideoPlayerFragment";

    // Settings
    public static final String K_FILE_PATH = "videoFilePath";
    public static final String K_FILE_URL = "videoFileURL";
    public static final String K_ALLOW_TOGGLE_FULLSCREEN = "allowToggleFullscreen";
    public static final String K_FINISH_ON_COMPLETION = "finishOnCompletion";
    public static final String K_AUTO_START_PLAYING = "autoStartPlaying";


    // Video file path / url
    String filePath;
    String fileURL;

    // More settings
    boolean allowToggleFullscreen = false;
    boolean finishOnCompletion = false;
    boolean autoHideControls = true;
    boolean autoStartPlaying = true;

    // Views & Layout
    AQuery aq;
    View rootView;
    VideoView videoView;
    LayoutInflater inflater;

    //region *** lifecycle ***
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_homage_video_view, container, false);
        aq = new AQuery(rootView);
        videoView = (VideoView)aq.id(R.id.videoView).getView();

        // Get the the file path / url of the video.
        Bundle b = getActivity().getIntent().getExtras();
        if (b == null) return rootView;
        initializeWithArguments(b);
        return rootView;
    }

    public void initializeWithArguments(Bundle b) {
        initializePlayerSettings(b);
        initializeUIState();
        initializePlayingVideo();
        bindUIHandlers();
    }

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
            videoView.setVideoPath(filePath);
        } else if (fileURL != null) {
            videoView.setVideoURI(Uri.parse(fileURL));
        }

        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnCompletionListener(this);
    }
    //endregion

    //region *** Media player ***
    private void initializePlayerSettings(Bundle b) {
        filePath = b.getString(K_FILE_PATH);
        if (filePath == null) {
            fileURL = b.getString(K_FILE_URL);
            if (fileURL == null) {
                return;
            }
        }

        // More settings
        allowToggleFullscreen = b.getBoolean(K_ALLOW_TOGGLE_FULLSCREEN, true);
        finishOnCompletion = b.getBoolean(K_FINISH_ON_COMPLETION, false);
        autoStartPlaying = b.getBoolean(K_AUTO_START_PLAYING, true);
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
        if (finishOnCompletion) {
            getActivity().finish();
        } else {
            showThumbState();
        }
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
        aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
        if (autoStartPlaying) start();
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
            pause();
        } else {
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
        if (videoView != null) videoView.start();
    }

    void showThumbState() {
        if (videoView != null) videoView.seekTo(0);
        pause();
        aq.id(R.id.videoThumbnailImage).visibility(View.VISIBLE);
        aq.id(R.id.videoBigPlayButton).visibility(View.VISIBLE);
    }

    void hideThumb() {
        aq.id(R.id.videoThumbnailImage).visibility(View.GONE);
        aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
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
            videoView.seekTo(0);
            videoView.pause();
            if (finishOnCompletion) {
                VideoPlayerFragment.this.getActivity().finish();
                return;
            }

            // Pressed the stop button.
            showThumbState();
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
            videoView.seekTo(0);
            start();
        }
    };



    View.OnClickListener onClickedFullScreenButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };
    //endregion
}
