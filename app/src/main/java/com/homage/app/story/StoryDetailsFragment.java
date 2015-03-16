package com.homage.app.story;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.IconButton;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.homage.CustomViews.ExpandableHeightGridView;
import com.homage.CustomViews.SwipeRefreshLayoutBottom;
import com.homage.CustomViews.VideoViewInternal;
import com.homage.app.R;
import com.homage.app.Utils.conversions;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.player.RemakeVideoFragmentActivity;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.app.player.FullScreenVideoPlayerActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class StoryDetailsFragment extends Fragment implements
        com.homage.CustomViews.SwipeRefreshLayoutBottom.OnRefreshListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnInfoListener
{
    public String TAG = "TAG_StoryDetailsFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    AQuery aq;
    ExpandableHeightGridView remakesGridView;

    public Story story;
    boolean shouldFetchMoreRemakes = false;
    //    VideoPlayerFragment videoPlayerFragment;
    int rowHeight;

    TextView likesCount;
    TextView viewsCount;

    //Fetching remakes area
    private final int NUMBERTOREFRESH = 16;
    final int fetchRemakes = NUMBERTOREFRESH;
    int skipRemakes = NUMBERTOREFRESH;

    RemakesAdapter adapter;

    SwipeRefreshLayoutBottom swipeLayout;

    //    Animation related variables
    boolean enteredRecorder = false;

    // videoview section
    VideoViewInternal mainVideo;
    String filePath;
    String fileURL;
    String thumbURL;
    String entityID;
    int entityType;
    int originatingScreen;

    // Info
    HashMap<String, Object> info;
    long initTime;

    // More settings
    boolean allowToggleFullscreen = false;
    boolean finishOnCompletion = false;
    boolean autoHideControls = true;
    boolean autoStartPlaying = false;
    boolean isEmbedded = false;
    boolean videoShouldNotPlay  = false;
    boolean firstVideoPlay = true;
    boolean firstRemakesLoad = true;


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            Activity a = getActivity();
            if(a != null) a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onRefreshBottom() {
        showFetchMoreRemakesProgress();
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                Log.d(TAG, "Will fetch more remakes.");
//                Get remakes from server and update the number to skip
                MainActivity activity = (MainActivity)getActivity();
                if(activity != null && story != null) {
                    activity.refetchMoreRemakesForStory(story, fetchRemakes, skipRemakes, User.getCurrent().getOID());
                    skipRemakes += fetchRemakes;
                }

            }
        }, 5000);
    }

    class RemakesAdapter extends BaseAdapter {
        private Activity mContext;
        private List<Remake> remakes;

        public RemakesAdapter(Activity context, List<Remake> remakes) {
            this.mContext = context;
            this.remakes = remakes;
        }

        void refreshRemakesFromLocalStorage() {

            hideFetchMoreRemakesProgress();
            User excludedUser = User.getCurrent();

            if (remakes==null) {
                remakes = story.getRemakes(excludedUser);
            } else {
                remakes.clear();
                remakes.addAll(story.getRemakes(excludedUser));
            }
            Log.d(TAG, String.format("%d remakes for this story", remakes.size()));
        }

        @Override
        public void notifyDataSetChanged() {
            refreshRemakesFromLocalStorage();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            int count = remakes.size();
            if (count > 0) {
                aq.id(R.id.noRemakesMessage).visibility(View.INVISIBLE);
            } else {
                aq.id(R.id.noRemakesMessage).visibility(View.VISIBLE);
            }
            return remakes.size();
        }

        @Override
        public Object getItem(int i) {
            return remakes.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }


        @Override
        public View getView(int i, View rowView, ViewGroup viewGroup) {
            try {
                if (rowView == null) {
                    rowView = inflater.inflate(R.layout.list_row_remake, remakesGridView, false);
                }
                final Remake remake = (Remake) getItem(i);
                final int remakeGridviwId = i;
                // Maintain 16/9 aspect ratio
                AbsListView.LayoutParams p = (AbsListView.LayoutParams)rowView.getLayoutParams();
                p.height = rowHeight / 2;
                rowView.setLayoutParams(p);

                // Configure Remake UI
                AQuery aq = new AQuery(rowView);
                aq.id(R.id.remakeImage).image(remake.thumbnailURL, true, true, 256, R.drawable.glass_dark);
//                aq.id(R.id.liked_button).clicked(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        updateLikes(remake.getOID().toString());
//                    }
//                });

                updateLikesAndViews(remake, aq);
                aq.id(R.id.watchRemakeButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, String.format("remakeOID: %s", remake.getOID()));

                        playRemakeMovie(remake.getOID(),remakeGridviwId);

                        HashMap props = new HashMap<String,String>();
                        props.put("story_id" , remake.getStory().getOID());
                        props.put("remake_id" , remake.getOID());
                        props.put("remake_owner_id", remake.userID);
                        props.put("index", String.valueOf(remakeGridviwId));
                        HMixPanel.sh().track("SDSelectedRemake",props);
                    }
                });
            } catch (InflateException ex) {
                Log.e(TAG, "Inflate exception on grid of story details", ex);
            }
            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    //    Update likes and views UI from remake
    private void updateLikesAndViews(Remake remake, AQuery aq) {

        likesCount = aq.id(R.id.likes_count).getTextView();
        int likesCountdb = remake.likesCount;
        if(likesCountdb < 0)
            likesCountdb = 0;
        likesCount.setText(Integer.toString(likesCountdb));

        viewsCount = aq.id(R.id.views_count).getTextView();
        int viewsCountdb = remake.viewsCount;
        if(viewsCountdb < 0)
            viewsCountdb = 0;
        viewsCount.setText(Integer.toString(viewsCountdb));
    }

    public static StoryDetailsFragment newInstance(int sectionNumber, Story story) {
        StoryDetailsFragment fragment;
        fragment = new StoryDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.story = story;
        Log.d(fragment.TAG, String.format("Showing story details: %s", story.name));
        return fragment;
    }

    private void initialize() {

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq = new AQuery(rootView);
        aq.id(R.id.storyDescription).text(story.description);
        aq.id(R.id.makeYourOwnButton).clicked(onClickedMakeYourOwnButton);
        mainVideo = (VideoViewInternal)aq.id(R.id.mainVideo).getView();

        mainVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                togglePlayPause();
                showControls();
                return false;
            }
        });
        //endregion

        // Aspect Ratio
        MainActivity activity = (MainActivity)getActivity();
        rowHeight = (activity.screenWidth * 9) / 16;

        setVideoLayout(true);

        if(firstRemakesLoad) {
            refreshRemakesAdapter();
            firstRemakesLoad = false;
        }

        loadVideoPlayer();

        loadVideoFromFileOrUrl(true);

        swipeLayout = (SwipeRefreshLayoutBottom)aq.id(R.id.swipe_container).getView();
        swipeLayout.setOnRefreshListener(this);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    private void refreshRemakesAdapter() {
        User excludedUser = User.getCurrent();
        // Adapters
        List<Remake> remakes = story.getRemakes(excludedUser);
//      Set gridview adapter
        adapter = new RemakesAdapter(getActivity(), remakes);
        remakesGridView = (ExpandableHeightGridView)aq.id(R.id.remakesGridView).getGridView();
        remakesGridView.setExpanded(true);
        remakesGridView.setAdapter(adapter);
    }

    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_story_details, container, false);

        return rootView;
    }

    private void loadVideoPlayer() {
        // Initialize the video of the story we need to show in the fragment.
        Bundle b = new Bundle();

        info = new HashMap<String, Object>();
        initTime = System.currentTimeMillis();

        // if there is a file locally play it and not the url (faster!! :))
        File cacheDir = getActivity().getCacheDir();
        File mOutFile = new File(cacheDir,
                story.getStoryVideoLocalFileName());
        if(mOutFile.exists()) {
            filePath = mOutFile.getPath();
        }

        fileURL = story.video;
        allowToggleFullscreen = true;
        finishOnCompletion = false;
        isEmbedded = true;
        thumbURL = story.thumbnail;
        autoStartPlaying = false;
        entityID = story.getOID().toString();
        entityType = HEvents.H_STORY;
        originatingScreen = HomageApplication.HM_STORY_DETAILS_TAB;

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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RemakeVideoFragmentActivity.CHANGED_LIKE_STATUS){
            refreshData();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity main = (MainActivity)getActivity();
                main.refetchTopRemakesForStory(story, User.getCurrent().getOID());
                shouldFetchMoreRemakes = true;
            }
        }, 500);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(!videoShouldNotPlay) {
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                handleEmbeddedVideoConfiguration(newConfig);
                ActionBar action = getActivity().getActionBar();
                if (action != null) action.show();
            }else{
                handleEmbeddedVideoConfiguration(newConfig);
                ActionBar action = getActivity().getActionBar();
                if (action != null) action.hide();
            }
        }else{
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        initialize();
        SetTitle();

        ((MainActivity) getActivity()).lastSection = MainActivity.SECTION_STORY_DETAILS;

        aq.id(R.id.greyscreen).visibility(View.GONE);

    }

    public void SetTitle() {
        // Set action bar
        ((MainActivity) getActivity())
                .setActionBarTitle(story.name);
        ((MainActivity) getActivity())
                .setActionBarNavBackgroundResource(R.drawable.selector_up_button);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();

        ((MainActivity)getActivity()).lastStory = story;

        ((MainActivity)getActivity()).startMusic(true);

        mainVideo.pause();
        aq.id(R.id.greyscreen).visibility(View.VISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    public void refreshData() {
        // Just an example of refreshing the data from local storage,
        // after it was fetched from server and parsed.

        adapter.notifyDataSetChanged();
    }
    //endregion

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    // fetchmoreremakesprogress
    private void showFetchMoreRemakesProgress() {
        aq.id(R.id.scroll_container).getView().setPadding(0,0,0, conversions.pixelsToDp(getActivity(), 75));
        aq.id(R.id.fetchMoreRemakesProgress).getView().setVisibility(View.VISIBLE);
        ((SmoothProgressBar) aq.id(R.id.fetchMoreRemakesProgress).getView()).progressiveStart();
    }

    private void hideFetchMoreRemakesProgress() {
        swipeLayout.setRefreshing(false);
        ((SmoothProgressBar)aq.id(R.id.fetchMoreRemakesProgress).getView()).progressiveStop();
        aq.id(R.id.scroll_container).getView().setPadding(0,0,0,conversions.pixelsToDp(getActivity(), 50));
    }
    //endregion


    //region *** Fullscreen ***
    private void handleEmbeddedVideoConfiguration(Configuration cfg) {
        int orientation = cfg.orientation;

        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
//                if(videoIsDisplayed) {
                enterFullScreen();
//                }
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                exitFullScreen();
                break;
        }
    }

    void enterFullScreen() {
        Log.v(TAG, "Video, change to full screen");

        // Hide everything else
        aq.id(R.id.makeYourOwnButton).visibility(View.GONE);
        aq.id(R.id.storyHead).visibility(View.GONE);
        aq.id(R.id.remakesGridView).visibility(View.GONE);
        aq.id(R.id.moreRemakes).visibility(View.GONE);
        aq.id(R.id.noRemakesMessage).visibility(View.GONE);
        aq.id(R.id.moreRemakes).visibility(View.GONE);
        aq.id(R.id.fetchMoreRemakesProgress).visibility(View.GONE);

        aq.id(R.id.scroll_container).getView().setPadding(0,0,0,0);

        // Show the video in full screen.
        setVideoLayout(false);

        // Actionbar
        ActionBar action = getActivity().getActionBar();
        if (action != null) action.hide();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    void exitFullScreen() {
        Log.v(TAG, "Video, exit full screen");

        // Hide everything else
        aq.id(R.id.makeYourOwnButton).visibility(View.VISIBLE);
        aq.id(R.id.storyHead).visibility(View.VISIBLE);
        aq.id(R.id.remakesGridView).visibility(View.VISIBLE);
        aq.id(R.id.moreRemakes).visibility(View.VISIBLE);
//        aq.id(R.id.loadingLayout).visibility(View.VISIBLE);
        aq.id(R.id.moreRemakes).visibility(View.VISIBLE);
        aq.id(R.id.fetchMoreRemakesProgress).visibility(View.VISIBLE);

        aq.id(R.id.scroll_container).getView().setPadding(0,0,0, conversions.pixelsToDp(getActivity(), 50));

        // Return to original layout
        setVideoLayout(true);

        // Actionbar
        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.showActionBar();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void setVideoLayout(boolean portraitOrLandscape) {
        View container = aq.id(R.id.mainVideoWrapper).getView();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        int portraitheight = (size.x * 9) / 16;
        int setHeight;
        if(portraitOrLandscape){
            setHeight = portraitheight;
        }
        else{
            setHeight = height;
        }

        if(container != null){
            container.getLayoutParams().width = width;
            container.getLayoutParams().height = setHeight;
        }
    }

    //endregion

    //region video player calls
    private void playRemakeMovie(String remakeID, int remakeGridviwId) {
        videoShouldNotPlay = true;
        mainVideo.pause();
        mainVideo.stopPlayback();
        hideFetchMoreRemakesProgress();

        if(remakeID != null && !remakeID.isEmpty()) {
            Remake remake = Remake.findByOID(remakeID);

            Intent intent = new Intent(this.getActivity(), RemakeVideoFragmentActivity.class);
            // Initialize the video of the story we need to show in the fragment.
            intent.putExtra(RemakeVideoFragmentActivity.K_FILE_URL, remake.videoURL);
            intent.putExtra(RemakeVideoFragmentActivity.K_ALLOW_TOGGLE_FULLSCREEN, true);
            intent.putExtra(RemakeVideoFragmentActivity.K_FINISH_ON_COMPLETION, false);
            intent.putExtra(RemakeVideoFragmentActivity.K_IS_EMBEDDED, true);
            intent.putExtra(RemakeVideoFragmentActivity.K_THUMB_URL, remake.thumbnailURL);
            intent.putExtra(RemakeVideoFragmentActivity.K_GRIDVIEW_REMAKE_ID, remakeGridviwId);
            intent.putExtra(HEvents.HK_VIDEO_ENTITY_ID, remakeID);
            intent.putExtra(HEvents.HK_VIDEO_ENTITY_TYPE, HEvents.H_REMAKE);
            intent.putExtra(HEvents.HK_VIDEO_ORIGINATING_SCREEN, HomageApplication.HM_STORY_DETAILS_TAB);

            startActivityForResult(intent,RemakeVideoFragmentActivity.CHANGED_LIKE_STATUS);
        }
    }

    //
    final View.OnClickListener onClickedMakeYourOwnButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            HomageApplication.getInstance().downloadPaused = true;
            enteredRecorder = true;
            ((MainActivity)getActivity()).stopDownloadThread();

            videoShouldNotPlay = true;
            mainVideo.pause();
            mainVideo.stopPlayback();
            createNewRemake();
        }
    };

    final View.OnClickListener onClickedPlayStoryVideo = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            FullScreenVideoPlayerActivity.openFullScreenVideoForURL(getActivity(), story.video,
                    story.thumbnail, HEvents.H_STORY , story.getOID().toString(),
                    HomageApplication.HM_STORY_DETAILS_TAB, true);
        }
    };

    private void createNewRemake() {
        if (story == null) return;

        User user = User.getCurrent();
        if (user == null) return;

        HashMap props = new HashMap<String,String>();
        props.put("story" , story.name);


        Remake unfinishedRemake = user.unfinishedRemakeForStory(story);
        MainActivity main = (MainActivity) this.getActivity();
        if (unfinishedRemake == null) {
            // No info about an unfinished remake exists in local storage.
            // Create a new remake.
            HMixPanel.sh().track("SDNewRemake",props);
            main.sendRemakeStoryRequest(story);
        } else {
            // An unfinished remake exists. Ask user if she want to continue this remake
            // or start a new one.
            main.askUserIfWantToContinueRemake(unfinishedRemake);
        }
    }

//    private void updateLikes(final String remakeID)
//    {
////        TODO create updatelikes
//    }
//
//    private void updateViews(final String remakeID)
//    {
////        TODO create updateViews
//    }

    private void showReportDialogForRemake(final String remakeID)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
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


    // *********************
    // VIDEO SECTION
    // *********************
    // Video file path / url

    public void loadVideoFromFileOrUrl(boolean startPlaying) {

        if (mainVideo != null){

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
                        if(!mainVideo.setVideoFD(fis.getFD())){
                            // if it doesn't work play from url
                            mainVideo.setVideoURI(Uri.parse(fileURL));
                        }
                    } catch (IOException e) {
                        // if it doesn't work play from url
                        mainVideo.setVideoURI(Uri.parse(fileURL));
                        e.printStackTrace();
                    }catch (SecurityException se) {
                        // if it doesn't work play from url
                        mainVideo.setVideoURI(Uri.parse(fileURL));
                        se.printStackTrace();
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (fileURL != null) {

                // A remote video with a given URL.
                mainVideo.setVideoURI(Uri.parse(fileURL));
            }

            mainVideo.setOnPreparedListener(this);
            mainVideo.setOnErrorListener(this);
            mainVideo.setOnCompletionListener(this);
        }
    }

    void showThumbState() {
        if(aq != null) {
            if (mainVideo != null) mainVideo.seekTo(100);
//        pause();
            if (thumbURL != null) {
                aq.id(R.id.videoThumbnailImage).visibility(View.VISIBLE);
            }
            aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
            aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mainVideo.seekTo(100);
        mainVideo.pause();
        autoStartPlaying = false;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        info.put(HEvents.HK_VIDEO_PLAYBACK_TIME, mainVideo.getCurrentPosition());
        info.put(HEvents.HK_VIDEO_TOTAL_DURATION, mainVideo.getDuration());
        HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_FINISH, info);

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        aq.id(R.id.videoFragmentLoading).visibility(View.GONE);
        if(autoStartPlaying){
            if(firstVideoPlay) {
                mainVideo.start();
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                firstVideoPlay = false;
                HEvents.sh().track(HEvents.H_EVENT_VIDEO_WILL_AUTO_PLAY, info);
            }
            else{
                mainVideo.seekTo(100);
            }
        }
        else{
            mainVideo.seekTo(100);
        }
    }

    //region *** Controls ***
    void showControls() {
        aq.id(R.id.videoBigPlayButton).visibility(View.VISIBLE);

        if (autoHideControls) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideControls();
                }
            }, 1000);
        }
    }

    void hideControls() {
        aq.id(R.id.videoBigPlayButton).visibility(View.GONE);
    }

    void togglePlayPause() {
        if (mainVideo.isPlaying()) {
            videoShouldNotPlay = true;
            mainVideo.pause();
            ((IconButton)aq.id(R.id.videoBigPlayButton).getView()).setText(R.string.icon_pause);
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_USER_PRESSED_PAUSE, info);
            info.put(HEvents.HK_VIDEO_PLAYBACK_TIME, mainVideo.getCurrentPosition());
            info.put(HEvents.HK_VIDEO_TOTAL_DURATION, mainVideo.getDuration());
            HEvents.sh().track(HEvents.H_EVENT_VIDEO_PLAYER_FINISH, info);
        } else {
            videoShouldNotPlay = false;
            mainVideo.start();
            ((IconButton)aq.id(R.id.videoBigPlayButton).getView()).setText(R.string.icon_play);
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        firstVideoPlay = false;
    }
//endregion
}


