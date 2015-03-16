package com.homage.app.player;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconButton;
import android.widget.ImageButton;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.homage.CustomViews.VideoViewInternal;
import com.homage.app.R;
import com.homage.networking.analytics.HEvents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    String entityID;
    int entityType;
    int originatingScreen;
    int thumbDrawableId;

    // More settings
    boolean allowToggleFullscreen = false;
    boolean finishOnCompletion = false;
    boolean autoHideControls = true;
    boolean autoStartPlaying = false;
    boolean isEmbedded = false;

    // Info
    HashMap<String, Object> info;
    long initTime;

    // Views & Layout
    AQuery aq;
    View rootView;
    public VideoViewInternal videoView;
    LayoutInflater inflater;
    boolean alreadyGotSettings = false;

    // booleans for not playing video
    public boolean remakePlaying = false;
    public boolean videoShouldNotPlay = false;
    public boolean storyDetailsPaused = false;

    MediaPlayer mediaPlayer;


    //region *** lifecycle ***
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setRetainInstance(true);
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_video_player, container, false);
        aq = new AQuery(rootView);
        videoView = (VideoViewInternal)aq.id(R.id.videoView).getView();

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

    public void setOrientation(boolean portOrLand){
        if(portOrLand){
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        else{
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            fullStop();
        } catch(Exception ex) {}
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    //endregion

    //region *** initializations ***
    private void initializeUIState() {

        if (autoStartPlaying) {
            aq.id(R.id.videoThumbnailImage).visibility(View.GONE);
            hideControls();
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
    }

    private void initializePlayingVideo() {
        // Initialize playing the video
        pause();
        loadVideoFromFileOrUrl(true);
    }


    private void disableButton(int buttonId) {
        ImageButton ib = (ImageButton)aq.id(buttonId).getView();
        ib.setAlpha(0.2f);
        ib.setImageResource(R.drawable.icon_small_player_disabled);
    }

    public void loadVideoFromFileOrUrl(boolean startPlaying) {

        if (videoView != null){

            if (startPlaying) {
                autoStartPlaying = true;
            } else {
                autoStartPlaying = false;
                showThumbState();
            }

            if (filePath != null) {

                try {

                    FileInputStream fis = new FileInputStream(new File(filePath));

                    try {
                        // play from file
                        boolean result = videoView.setVideoFD(fis.getFD());
                        if(!result){
                            videoView.setVideoURI(Uri.parse(fileURL));
                        }
                    } catch (IOException e) {
                        // if it doesn't work play from url
                        videoView.setVideoURI(Uri.parse(fileURL));
                        e.printStackTrace();
                    }catch (SecurityException se) {
                        // if it doesn't work play from url
                        videoView.setVideoURI(Uri.parse(fileURL));
                        se.printStackTrace();
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (fileURL != null) {

                // A remote video with a given URL.
                videoView.setVideoURI(Uri.parse(fileURL));
            }

            videoView.setOnPreparedListener(this);
            videoView.setOnErrorListener(this);
            videoView.setOnCompletionListener(this);
        }
    }
    //endregion

    //region *** Media player ***
    private void initializePlayerSettings(Bundle b) {
        info = new HashMap<String, Object>();
        initTime = System.currentTimeMillis();
        filePath = b.getString(K_FILE_PATH);

        entityType = b.getInt(HEvents.HK_VIDEO_ENTITY_TYPE);
        entityID = b.getString(HEvents.HK_VIDEO_ENTITY_ID);
        originatingScreen = b.getInt(HEvents.HK_VIDEO_ORIGINATING_SCREEN);
        fileURL = b.getString(K_FILE_URL);
        if(fileURL != null) {
            info.put(HEvents.HK_VIDEO_FILE_URL, fileURL);
        }
        if(filePath != null) {
            info.put(HEvents.HK_VIDEO_FILE_PATH, filePath);
        }
        info.put(HEvents.HK_VIDEO_INIT_TIME, initTime);
        info.put(HEvents.HK_VIDEO_ENTITY_TYPE, entityType);
        info.put(HEvents.HK_VIDEO_ENTITY_ID, entityID);
        info.put(HEvents.HK_VIDEO_ORIGINATING_SCREEN, originatingScreen);

        // More settings
        allowToggleFullscreen = b.getBoolean(K_ALLOW_TOGGLE_FULLSCREEN, true);
        finishOnCompletion = b.getBoolean(K_FINISH_ON_COMPLETION, false);
        autoStartPlaying = b.getBoolean(K_AUTO_START_PLAYING, !videoShouldNotPlay);
        isEmbedded = b.getBoolean(K_IS_EMBEDDED, false);
        thumbURL = b.getString(K_THUMB_URL, null);
        if (filePath != null){thumbURL = null;}
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
        aq.id(R.id.videoBigStopButton).clicked(onClickedStopButton);
        aq.id(R.id.videoBigPlayButton).clicked(onClickedBigPlayButton);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
//        pause();
        fullStop();
        Log.d(TAG, String.format("Finished playing video: %s", filePath));
        videoView.pause();
        if (onFinishedPlayback != null) {
            info.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
            info.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_FINISH, info);
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
        pause();
        showThumbState();
        Log.d(TAG, String.format("Video is prepared for playing: %s %s", filePath, fileURL));
        aq.id(R.id.videoCurtain).visibility(View.GONE);
        if (autoStartPlaying) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_WILL_PLAY , info);

            togglePlayPause();
        }
    }


    //endregion

    //region *** Controls ***
    void showControls() {
        aq.id(R.id.videoBigPlayButton).visibility(View.VISIBLE);
        aq.id(R.id.videoBigStopButton).visibility(View.VISIBLE);

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
        aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
        aq.id(R.id.videoBigStopButton).visibility(View.GONE);
    }

    void toggleControls() {
        if (aq.id(R.id.videoBigPlayButton).getView().getVisibility() == View.VISIBLE ||
                aq.id(R.id.videoBigStopButton).getView().getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    void togglePlayPause() {
        if (videoView.isPlaying()) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_PAUSE, info);
            pause();
            ((IconButton)aq.id(R.id.videoBigPlayButton).getView()).setText(R.string.icon_play);
        } else {
            if(!remakePlaying && !videoShouldNotPlay) {
                start();
                ((IconButton)aq.id(R.id.videoBigPlayButton).getView()).setText(R.string.icon_pause);
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        }
    }

    void pause() {
        if (videoView != null) videoView.pause();
    }

    void start() {
        if(!videoShouldNotPlay) {
            if (thumbURL != null) {
                aq.id(R.id.videoThumbnailImage).visibility(View.INVISIBLE);
            }
            aq.id(R.id.videoView).visibility(View.VISIBLE);
            if (videoView != null) {
                HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYING, info);
                videoView.start();
                autoStartPlaying = false;
            }
            if (onStartedPlayback != null) {
                new Handler().post(onStartedPlayback);
            }
        }
    }

    void showThumbState() {
        if(aq != null) {
            if (videoView != null) videoView.seekTo(100);
//        pause();
            if (thumbURL != null) {
                aq.id(R.id.videoThumbnailImage).visibility(View.VISIBLE);
            }
            aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
            aq.id(R.id.videoBigStopButton).visibility(View.GONE);
            aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
        }
    }

    void showThumbWhileLoading() {
        if(thumbURL != null) {aq.id(R.id.videoThumbnailImage).visibility(View.VISIBLE);}
    }

    void hideThumb() {
        if(thumbURL != null) {
            aq.id(R.id.videoThumbnailImage).visibility(View.INVISIBLE);
        }
        aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
        aq.id(R.id.videoBigStopButton).visibility(View.GONE);

    }
    //endregion

    //region *** video commands ***
    public void fullStop() {
        if(videoView != null) {

            ((IconButton)aq.id(R.id.videoBigPlayButton).getView()).setText(R.string.icon_play);

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
                VideoPlayerFragment.this.getActivity().finish();
                return;
            }
            showThumbState();

        }
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
            eInfo.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
            eInfo.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_STOP, eInfo);

            fullStop();
        }
    };

    View.OnClickListener onClickedBigPlayButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_PLAY, info);
            togglePlayPause();
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
