package com.homage.app.player;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconButton;
import android.widget.IconTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.crashlytics.android.Crashlytics;
import com.homage.CustomAdapters.GestureListener;
import com.homage.app.R;
import com.homage.app.main.MainActivity;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;

import java.util.HashMap;

public class RemakeVideoFragmentActivity extends
        FragmentActivity
        implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnInfoListener{

    static final String TAG = MainActivity.FRAGMENT_TAG_REMAKE_VIDEO;

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
        ((IconButton)aq.id(R.id.remakeVideoPlayPauseButton).getView()).setText(R.string.icon_pause);
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
    ImageView remakeThumbnail;
    RelativeLayout topLayout;
    int initialHeight, initialWidth;
    boolean intializeRemakeDimentions;

    // More settings
    boolean allowToggleFullscreen = false;
    boolean finishOnCompletion = false;
    boolean autoHideControls = false;
    boolean autoStartPlaying = false;
    boolean isEmbedded = false;
    boolean isPlayerReady = false;
    private MediaController controller = null;

    Remake remake;
    Story story;
    String userFullname;
    IconButton isLiked;
    TextView likesCount;

    // Info
    HashMap<String, Object> info;
    long initTime;

    // Views & Layout
    AQuery aq;
    VideoView videoView;
//    boolean alreadyGotSettings = false;

    //region *** lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_remake_video_view);
        aq = new AQuery(this);

        // Bind the gestureDetector to GestureListener
        mGestureDetector = new GestureDetector(this, new GestureListener(){
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                flingRemake();
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                doubletapRemake();
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent ev) {
                toggleControls();
                return super.onSingleTapUp(ev);
            }
        });

        isPlayerReady = false;

//      <-- VideoView Stuff-->
        videoView = (VideoView) aq.id(R.id.videoView).getView();
        videoView.setVisibility(View.VISIBLE);
        aq.id(R.id.remake_video_top_layout).getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = mGestureDetector.onTouchEvent(event);
                if (!result) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
//                        stopScrolling();
                        result = true;
                    }
                }
                return result;
            }
        });

        topLayout = (RelativeLayout)aq.id(R.id.remake_video_top_layout).getView();
        isLiked = (IconButton)aq.id(R.id.liked_button).getView();
        remakeThumbnail = aq.id(R.id.remakeThumbnailImage).getImageView();

        initialize();

        Log.d(TAG, String.format("StartBlackScreen"));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onRemakeLiked);
    }

    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onRemakeLiked, new IntentFilter((HomageServer.INTENT_REMAKE_LIKED)));
    }

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
                        if (remakeOID.equals(remake.getOID())) {
                            if (remakeOID == null) return;
                            isLiked.setSelected(!isLiked.isSelected());
                            remake.isLiked = !remake.isLiked;
                            remake.save();
                            if(remake.isLiked) {
                                ((IconButton)aq.id(R.id.liked_button).getView()).setText(R.string.icon_heart_unlike);
                            }
                            else{
                                ((IconButton)aq.id(R.id.liked_button).getView()).setText(R.string.icon_heart_like);
                            }
//                            aq.id(R.id.liked_flash).visibility(View.GONE);
//                            aq.id(R.id.liked_button).visibility(View.VISIBLE);
                            aq.id(R.id.liked_button).clickable(true);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
        else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }

        setupScreen();
    }

    private void setupScreen() {
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            showHeaders();
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams)((aq.id(R.id.remakeWrapper).getView())).getLayoutParams();
            p.addRule(RelativeLayout.BELOW, R.id.remakeHeaders);
            aq.id(R.id.remakeWrapper).getView().setLayoutParams(p);
            (aq.id(R.id.remakeWrapper).getView()).getLayoutParams().height = initialHeight;
            (aq.id(R.id.remakeWrapper).getView()).getLayoutParams().width = initialWidth;
            videoView.getLayoutParams().height = initialHeight;
            videoView.getLayoutParams().width = initialWidth;
        }else{
            hideHeaders();
            (aq.id(R.id.remakeWrapper).getView()).setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            videoView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

//    private void flipScreen() {
//        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        }
//        else{
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
//    }


    public void flingRemake()
    {
        finish();
    }

    public void doubletapRemake()
    {
//        flipScreen();
    }


    public void initialize() {
        initObservers();
        initializePlayerSettings();
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

    @Override
    public void onResume() {
        super.onResume();
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            showHeaders();
        }
    }

    private void showHeaders() {
        aq.id(R.id.remakeHeaders).getView().setVisibility(View.VISIBLE);
    }

    private void hideHeaders() {
        aq.id(R.id.remakeHeaders).getView().setVisibility(View.GONE);
    }

    //endregion

//        aq.id(R.id.remakeVideoFragmentLoading).getView().setAlpha(1.0f);

    //region *** initializations ***
    private void initializeUIState() {
//        if (!allowToggleFullscreen) {
//            disableButton(R.id.remakeVideoFullScreenButton);
//        }

        if (autoStartPlaying) {
            remakeThumbnail.setVisibility(View.GONE);
//            aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
        }

        if (thumbURL != null) {
            aq.id(R.id.remakeThumbnailImage).image(
                    thumbURL,
                    true,
                    true,
                    videoView.getWidth(),
                    R.drawable.glass_dark,
                    null, AQuery.FADE_IN);
        } else if (thumbDrawableId > 0) {
            aq.id(R.id.remakeThumbnailImage).image(thumbDrawableId);


        } else {
            aq.id(R.id.remakeThumbnailImage).visibility(View.GONE);
        }

        isLiked.setSelected(remake.isLiked);
        String viewsCount = String.valueOf(remake.viewsCount);
        getLikesCount();
        ((IconTextView)aq.id(R.id.views_count).getView()).setText(viewsCount);
        if(remake.isLiked) {
            ((IconButton)aq.id(R.id.liked_button).getView()).setText(R.string.icon_heart_unlike);
        }
        else{
            ((IconButton)aq.id(R.id.liked_button).getView()).setText(R.string.icon_heart_like);
        }

//        if(user != null) {
//            if (user.isFacebookUser()) {
//
//                // Facebook profile picture
//                aq.id(R.id.user_image).image(
//                        user.getFacebookProfilePictureURL(),
//                        true,
//                        false,
//                        100,
//                        R.drawable.com_facebook_profile_picture_blank_portrait);
//            } else {
//                aq.id(R.id.user_image).image(R.drawable.guest);
//                aq.id(R.id.user_name).getTextView().setText("Guest");
//            }
//        }
//        else{
        if(userFullname != null) {
            aq.id(R.id.user_name).getTextView().setText(userFullname);
        }
        else{
            aq.id(R.id.user_name).getTextView().setText("Guest");
        }
        aq.id(R.id.user_image).image(R.drawable.guest);

//        }
        if(story != null){
            aq.id(R.id.remakeStoryName).getTextView().setText(story.name);
            aq.id(R.id.storyRemakeDescription).getTextView().setText(story.description);
        }

        showThumbWhileLoading();
    }

    private void getLikesCount() {
        likesCount = aq.id(R.id.likes_count).getTextView();
        int likesCountdb = remake.likesCount;
        if(likesCountdb < 0)
            likesCountdb = 0;
        likesCount.setText(Integer.toString(likesCountdb));
    }

    private void initializePlayingVideo() {
        // Initialize playing the video

        pause();
        initializeVideoPlayer();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        (aq.id(R.id.remakeWrapper).getView()).getLayoutParams().height = remakeThumbnail.getHeight();
        (aq.id(R.id.remakeWrapper).getView()).getLayoutParams().width = remakeThumbnail.getWidth();
        if(!intializeRemakeDimentions) {
            initialHeight = remakeThumbnail.getHeight();
            initialWidth = remakeThumbnail.getWidth();
            intializeRemakeDimentions = true;
        }
    }

//    private void resizeRemakeWrapper() {
//        final ImageView iv = aq.id(R.id.remakeThumbnailImage).getImageView();
//        ViewTreeObserver vto = iv.getViewTreeObserver();
//        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            public boolean onPreDraw() {
//                iv.getViewTreeObserver().removeOnPreDrawListener(this);
//                finalHeight = iv.getMeasuredHeight();
//                finalWidth = iv.getMeasuredWidth();
//                (aq.id(R.id.remakeWrapper).getView()).getLayoutParams().height = finalHeight;
//                (aq.id(R.id.remakeWrapper).getView()).getLayoutParams().width = finalWidth;
//                return true;
//            }
//        });
//    }


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
//            videoView.start();
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
        story = remake.getStory();
        userFullname = remake.userFullname;
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
//        alreadyGotSettings = true;

        if (autoStartPlaying) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_WILL_AUTO_PLAY, info);
        }
    }



    // Bind to UI events
    private void bindUIHandlers() {
//        aq.id(R.id.touchVideoButton).clicked(onClickedToggleControlsButton);
//        aq.id(R.id.videoStopButton).clicked(onClickedStopButton);
        aq.id(R.id.shareButton).clicked(onClickedShareButton);
        aq.id(R.id.remakeVideoPlayPauseButton).clicked(onClickedPlayPauseButton);
//        aq.id(R.id.remakeVideoFullScreenButton).clicked(onClickedFullScreenButton);
//        aq.id(R.id.videoBigPlayButton).clicked(onClickedBigPlayButton);
        aq.id(R.id.reportButton).clicked(onClickedReportButton);

            aq.id(R.id.liked_button).clicked(onClickedLikeButton);
//        aq.id(R.id.liked_text).clicked(onClickedLikeButton);
        aq.id(R.id.remake_bottom_layer).clicked(onClickedBottomLayer);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, String.format("Finished playing video: %s", filePath));
        videoView.pause();
        ((IconButton)aq.id(R.id.remakeVideoPlayPauseButton).getView()).setText(R.string.icon_play);
        if (onFinishedPlayback != null) {
            info.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
            info.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_FINISH, info);
            new Handler().post(onFinishedPlayback);

        }
        if (finishOnCompletion) {
            finish();
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
//        initialHeight = videoView.getHeight();
//        initialWidth = videoView.getWidth();
        Log.d(TAG, String.format("EndBlackScreen"));
        Log.d(TAG, String.format("Video is prepared for playing: %s %s", filePath, fileURL));
        aq.id(R.id.remakeThumbnailImage).visibility(View.GONE);
        aq.id(R.id.loadingVideoPprogress).visibility(View.GONE);
        aq.id(R.id.remakeVideoFragmentLoading).visibility(View.GONE);
        isPlayerReady = true;
        showControls();
        videoView.seekTo(100);
        if (autoStartPlaying) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_WILL_PLAY, info);
            start();
        }
    }


    //endregion

    //region *** Controls ***
    void showControls() {
        aq.id(R.id.user_image).visibility(View.VISIBLE);
        aq.id(R.id.user_name).visibility(View.VISIBLE);
        aq.id(R.id.views_count).visibility(View.VISIBLE);
        aq.id(R.id.views_icon).visibility(View.VISIBLE);
        aq.id(R.id.likes_count).visibility(View.VISIBLE);
        aq.id(R.id.likes_icon).visibility(View.VISIBLE);
        aq.id(R.id.shareButton).visibility(View.VISIBLE);
        aq.id(R.id.liked_button).visibility(View.VISIBLE);
        aq.id(R.id.remakeVideoPlayPauseButton).visibility(View.VISIBLE);
        aq.id(R.id.reportButton).visibility(View.VISIBLE);
//        aq.id(R.id.remakeVideoFullScreenButton).visibility(View.VISIBLE);

    }

    void hideControls() {
        aq.id(R.id.user_name).visibility(View.GONE);
        aq.id(R.id.user_image).visibility(View.GONE);
        aq.id(R.id.views_count).visibility(View.GONE);
        aq.id(R.id.views_icon).visibility(View.GONE);
        aq.id(R.id.likes_count).visibility(View.GONE);
        aq.id(R.id.likes_icon).visibility(View.GONE);
        aq.id(R.id.shareButton).visibility(View.GONE);
        aq.id(R.id.remakeVideoPlayPauseButton).visibility(View.GONE);
        aq.id(R.id.liked_button).visibility(View.GONE);
        aq.id(R.id.reportButton).visibility(View.GONE);
//        aq.id(R.id.remakeVideoFullScreenButton).visibility(View.GONE);
    }

    void toggleControls() {
        if (aq.id(R.id.remakeVideoPlayPauseButton).getView().getVisibility() == View.VISIBLE) {
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
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideControls();
                }
            }, 1000);
            start();
        }
    }

    void pause() {
//        ImageButton ib = (ImageButton) aq.id(R.id.videoPlayPauseButton).getView();
//        ib.setImageResource(R.drawable.selector_video_button_play);
        if (videoView != null) videoView.pause();
        ((IconButton)aq.id(R.id.remakeVideoPlayPauseButton).getView()).setText(R.string.icon_play);
    }

    void start() {
        aq.id(R.id.remakeThumbnailImage).visibility(View.GONE);
//        ImageButton ib = (ImageButton) aq.id(R.id.videoPlayPauseButton).getView();
//        ib.setImageResource(R.drawable.selector_video_button_pause);
//        aq.id(R.id.videoView).visibility(View.VISIBLE);
        if (videoView != null) {
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYING, info);
            videoView.start();
            ((IconButton)aq.id(R.id.remakeVideoPlayPauseButton).getView()).setText(R.string.icon_pause);
        }
        if (onStartedPlayback != null) {
            new Handler().post(onStartedPlayback);
        }

    }

    void showThumbState() {
        if (videoView != null) videoView.seekTo(100);
        pause();
        if(!isPlayerReady){
            aq.id(R.id.remakeThumbnailImage).visibility(View.VISIBLE);
        }
//        aq.id(R.id.videoBigPlayButton).visibility(View.VISIBLE);
        aq.id(R.id.remakeVideoFragmentLoading).visibility(View.GONE);
    }

    void showThumbWhileLoading() {
        if(!isPlayerReady) {
            aq.id(R.id.remakeThumbnailImage).getImageView().setVisibility(View.VISIBLE);
        }
        aq.id(R.id.loadingVideoPprogress).visibility(View.VISIBLE);
        aq.id(R.id.remakeVideoFragmentLoading).visibility(View.VISIBLE);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    void hideThumb() {
        aq.id(R.id.remakeThumbnailImage).visibility(View.INVISIBLE);
//        aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
    }
    //endregion

    //region *** video commands ***
    public void fullStop() {
        HashMap<String, Object> eInfo = new HashMap<String, Object>(info);
        eInfo.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
        eInfo.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());

        videoView.seekTo(100);
        videoView.pause();

        HEvents.sh().track(HEvents.H_EVENT_VIDEO_FULL_STOP, eInfo);
        if (onFinishedPlayback != null) {
            new Handler().post(onFinishedPlayback);
        }
        if (finishOnCompletion) {
            RemakeVideoFragmentActivity.this.finish();
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
            RemakeVideoFragmentActivity.this.toggleControls();
        }
    };

//    View.OnClickListener onClickedStopButton = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//            HashMap<String, Object> eInfo = new HashMap<String, Object>(info);
//            eInfo.put(HEvents.HK_VIDEO_PLAYBACK_TIME, videoView.getCurrentPosition());
//            eInfo.put(HEvents.HK_VIDEO_TOTAL_DURATION, videoView.getDuration());
//            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_STOP, eInfo);
//
//            fullStop();
//        }
//    };

    View.OnClickListener onClickedShareButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            fullStop();
            startShareIntent();
        }
    };

    private void startShareIntent(){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        if(userFullname != null && story != null) {
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Remake of " + story.name);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out " + userFullname + "'s Remake of " + story.name + "\n" +
                    "http://play.homage.it/" + remake.getOID());
        }else if(userFullname == null && story != null) {
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Remake of " + story.name);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this Remake of " + story.name + "\n" +
                    "http://play.homage.it/" + remake.getOID());
        }else {
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Remake from Homage");
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this Remake\n" +
                    "http://play.homage.it/" + remake.getOID());
        }
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
    }

    View.OnClickListener onClickedPlayPauseButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            togglePlayPause();
        }
    };

//    View.OnClickListener onClickedBigPlayButton = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            hideThumb();
//            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_PLAY, info);
//            aq.id(R.id.loadingVideoPprogress).visibility(View.VISIBLE);
//            videoView.seekTo(0);
//            start();
//        }
//    };

    View.OnClickListener onClickedLikeButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            aq.id(R.id.liked_button).visibility(View.GONE);
            aq.id(R.id.liked_button).clickable(false);
//            aq.id(R.id.liked_text).visibility(View.GONE);
//            aq.id(R.id.liked_text).clickable(false);
//            ImageView whiteCircle = aq.id(R.id.liked_flash).getImageView();
//            whiteCircle.setVisibility(View.VISIBLE);
//            Animation flash = AnimationUtils.loadAnimation(aq.id(R.id.liked_flash).getView(), R.anim.animation_flash_button);
//            whiteCircle.startAnimation(flash);
            HomageServer.sh().reportUserLikedRemake(remake.getOID(), !remake.isLiked);
        }
    };

    View.OnClickListener onClickedReportButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showReportDialogForRemake(remake.getOID().toString());
        }
    };

    private void showReportDialogForRemake(final String remakeID)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.report_abusive_remake_title);
        builder.setItems(
                new CharSequence[] {"yes" , "no"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                reportAsInappropriate(remakeID);
                                break;
                            case 1:
                                break;

                        }
                    }
                });
        builder.create().show();
    }

    private void reportAsInappropriate(String remakeID)
    {
        HomageServer.sh().reportRemakeAsInappropriate(remakeID);
    }

    View.OnClickListener onClickedBottomLayer = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };


    View.OnClickListener onClickedFullScreenButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//           flipScreen();
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
                aq.id(R.id.remakeVideoFragmentLoading).visibility(View.VISIBLE);
                break;

            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                Log.v(TAG, "media rendering start");
                aq.id(R.id.remakeVideoFragmentLoading).visibility(View.GONE);
                break;

            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.v(TAG, "media buffering end");
                HEvents.sh().track(HEvents.H_EVENT_VIDEO_BUFFERING_END, info);
                aq.id(R.id.remakeVideoFragmentLoading).visibility(View.GONE);
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