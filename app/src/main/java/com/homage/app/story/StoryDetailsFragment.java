package com.homage.app.story;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.homage.CustomViews.GestureListener;
import com.homage.CustomViews.SwipeRefreshLayoutBottom;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.player.RemakeVideoFragmentActivity;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.app.player.FullScreenVideoPlayerActivity;

import java.util.HashMap;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class StoryDetailsFragment extends Fragment implements com.homage.CustomViews.SwipeRefreshLayoutBottom.OnRefreshListener{
    public String TAG = "TAG_StoryDetailsFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String VIDEO_FRAGMENT_TAG = "videoPlayerFragment";

    View rootView;
    LayoutInflater inflater;
    AQuery aq;
    GridView remakesGridView;

    private final Handler handler = new Handler();
    private Runnable runPager;

    public Story story;
    boolean shouldFetchMoreRemakes = false;
    VideoPlayerFragment videoPlayerFragment;
    int rowHeight;

    TextView likesCount;
    TextView viewsCount;

    //Fetching remakes area
    private final int NUMBERTOREFRESH = 16;
    final int fetchRemakes = NUMBERTOREFRESH;
    int skipRemakes = NUMBERTOREFRESH;

    RemakesAdapter adapter;
    FrameLayout storyDetailsVideoContainer;

    SwipeRefreshLayoutBottom swipeLayout;

    //    Gesture stuff
    private GestureDetector mGestureDetector;
    boolean userScrolled = false;
    boolean scrollingUp;
    float lastScrollPosition = 400;

//    Animation related variables
    boolean videoIsDisplayed = false;
    boolean transformingVideo = false;
    boolean firstRun = true;
    boolean finishedPlayingVideo = true;

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
                aq.id(R.id.loadingLayout).visibility(View.INVISIBLE);
            } else {
                aq.id(R.id.loadingLayout).visibility(View.VISIBLE);
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
                aq.id(R.id.liked_button).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateLikes(remake.getOID().toString());
                    }
                });

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

    ;

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

        aq = new AQuery(rootView);
        aq.id(R.id.storyDescription).text(story.description);

        // Aspect Ratio
        MainActivity activity = (MainActivity)getActivity();
        rowHeight = (activity.screenWidth * 9) / 16;

        refreshRemakesAdapter();

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.makeYourOwnButton).clicked(onClickedMakeYourOwnButton);
        //endregion
    }

    private void refreshRemakesAdapter() {
        User excludedUser = User.getCurrent();
        // Adapters
        List<Remake> remakes = story.getRemakes(excludedUser);
//      Set gridview adapter
        adapter = new RemakesAdapter(getActivity(), remakes);
        remakesGridView = (GridView)aq.id(R.id.remakesGridView).getGridView();
        remakesGridView.setAdapter(adapter);
    }

    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_story_details, container, false);
        initialize();

        // Bind the gestureDetector to GestureListener
        mGestureDetector = new GestureDetector(getActivity(), new GestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//                flingUI();
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
//                doubletapUI();
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent ev) {
//                singleTapUI();
                return super.onSingleTapUp(ev);
            }
        });

//        Bind the more remakes button to the Gesture detector
        aq.id(R.id.moreRemakes).getView().setOnTouchListener(new View.OnTouchListener() {
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

        // Add embedded video player fragment.
        videoPlayerFragment = new VideoPlayerFragment();
        runPager = new Runnable() {

            @Override
            public void run()
            {

                getFragmentManager().beginTransaction().add(R.id.storyDetailsVideoContainer,
                        videoPlayerFragment,
                        VIDEO_FRAGMENT_TAG).commit();
            }
        };
        handler.post(runPager);

        loadVideoPlayer();


        // Allow orientation change.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);


        swipeLayout = (SwipeRefreshLayoutBottom)aq.id(R.id.swipe_container).getView();
        swipeLayout.setOnRefreshListener(this);

        storyDetailsVideoContainer = (FrameLayout)aq.id(R.id.storyDetailsVideoContainer).getView();

//        Flings the UI into correct position after loading
        if(rootView != null) {
            rootView.post(new Runnable() {
                               @Override
                               public void run() {
                                   // code you want to run when view is visible for the first time
                                   if (firstRun) {
                                       openDemoVideo();
                                       firstRun = false;
                                   }
                               }
                           }
            );
        }

        ((GridView)aq.id(R.id.remakesGridView).getView()).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // get last scroll position and current scroll position to figure out if scrolling up or down
                // then pass that through global variable to onscroll of gridview and only open video when scrolling down
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastScrollPosition = event.getY();
                }
                else if(event.getAction() == MotionEvent.ACTION_MOVE){

                    float currentPosition = event.getY();
                    if(currentPosition > lastScrollPosition)
                    {
                        scrollingUp = false;
                    }
                    else{
                        scrollingUp = true;
                    }

                    lastScrollPosition = currentPosition;
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    lastScrollPosition = 0;
                }

                return false;
            }
        });

//        ((GridView)aq.id(R.id.remakesGridView).getView())

        ((GridView)aq.id(R.id.remakesGridView).getView()).setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                    userScrolled = true;
                }
                else{
                    userScrolled = false;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(userScrolled) {
                    if (!firstRun && firstVisibleItem == 0 && !videoIsDisplayed && !scrollingUp) {
                        openDemoVideo();
                    } else if (!firstRun && firstVisibleItem >= 0 && videoIsDisplayed && scrollingUp) {
                        closeDemoVideo();
                    }
                }

            }
        });

        return rootView;
    }

//    region Gesture Handlers and functions

    public void singleTapUI(){
//        openCloseRemakesGridView();
    }

    public void flingUI() {
//        openCloseRemakesGridView();
    }

    public void doubletapUI() {
//        flipScreen();
        int test = 10;
    }

    public void openCloseRemakesGridView() {

        if(transformingVideo == false) {

//        Open Remakes GridView
            if (videoIsDisplayed) {
                closeDemoVideo();
            }
            //        Close Remakes GridView
            else {
                openDemoVideo();

            }

        }
    }

    private void openDemoVideo() {

        if(!transformingVideo)
    {
        transformingVideo = true;

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int width = size.x;
        final int height = size.y;
        final int portraitheight = (size.x * 9) / 16;
        //            On first run only animate the gridview to close and display demo video
        if (!firstRun) {
            animateStoryDetailsVideoContainer(width, portraitheight, -185f, 120f, 1f, 1f);
            aq.id(R.id.moreRemakes).getView().animate().yBy(portraitheight);
            aq.id(R.id.swipe_container).getView().animate().yBy(portraitheight);
        } else {
            animateStoryDetailsVideoContainer(width, portraitheight, 0, 0, 0, 0);
            aq.id(R.id.moreRemakes).getView().animate().yBy(portraitheight);
            aq.id(R.id.swipe_container).getView().animate().yBy(portraitheight);
        }

        videoIsDisplayed = true;
        videoPlayerFragment.videoIsShowing = true;

//            Allow screen orientation to rotate demo video into full screen
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        aq.id(R.id.moreRemakes).getTextView().setText(R.string.show_more_remakes);
    }
        //        firstRun runs first but never runs again...
        firstRun = false;
    }

    private void closeDemoVideo() {

        if(!transformingVideo) {
            transformingVideo = true;

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            final int width = size.x;
            final int height = size.y;
            final int portraitheight = (size.x * 9) / 16;
            //            Animate UI
            aq.id(R.id.moreRemakes).getView().animate().yBy(-portraitheight);
            aq.id(R.id.swipe_container).getView().animate().yBy(-portraitheight);
            animateStoryDetailsVideoContainer(0, 0, 185f, -120f, -1f, -1f);

            videoIsDisplayed = false;
            videoPlayerFragment.videoIsShowing = false;

//            Video is closed so don't allow flip screen! And close video if playing
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            videoPlayerFragment.fullStop();

            aq.id(R.id.moreRemakes).getTextView().setText(R.string.show_demo_video);
            //        firstRun runs first but never runs again...
            firstRun = false;
        }
    }

    private void animateStoryDetailsVideoContainer(final int width, final int height, float xby,
                                                   float yby, float scalexby, float scaleyby) {
//        Scale and move the video container
        storyDetailsVideoContainer.animate().xBy(xby).yBy(yby).scaleXBy(scalexby)
                .scaleYBy(scaleyby).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                If the video is open set it's height and width
                if(height != 0) {
                    videoPlayerFragment.getView().getLayoutParams().height = height;
                    videoPlayerFragment.getView().getLayoutParams().width = width;
                }
                transformingVideo = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

//    endregion

    private void loadVideoPlayer() {
        // Initialize the video of the story we need to show in the fragment.
        Bundle b = new Bundle();
        b.putString(VideoPlayerFragment.K_FILE_URL, story.video);
        b.putBoolean(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, true);
        b.putBoolean(VideoPlayerFragment.K_FINISH_ON_COMPLETION, false);
        b.putBoolean(VideoPlayerFragment.K_IS_EMBEDDED, true);
        b.putString(VideoPlayerFragment.K_THUMB_URL, story.thumbnail);

        b.putString(HEvents.HK_VIDEO_ENTITY_ID, story.getOID().toString());
        b.putInt(HEvents.HK_VIDEO_ENTITY_TYPE, HEvents.H_STORY);
        b.putInt(HEvents.HK_VIDEO_ORIGINATING_SCREEN, HomageApplication.HM_STORY_DETAILS_TAB);

        videoPlayerFragment.setArguments(b);

        // When video not playing, don't allow orientation changes.
        videoPlayerFragment.setOnFinishedPlayback(new Runnable() {
            @Override
            public void run() {
                try {
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    finishedPlayingVideo = true;
                } catch (Exception e) {}
            }
        });

        // When video playing, allow orientation changes.
        videoPlayerFragment.setOnStartedPlayback(new Runnable() {
            @Override
            public void run() {
                try {
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    finishedPlayingVideo = false;
                } catch (Exception e) {}
            }
        });
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
        if(videoIsDisplayed) {
            handleEmbeddedVideoConfiguration(newConfig);
            ActionBar action = getActivity().getActionBar();
            if (action != null) action.hide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        videoPlayerFragment.remakePlaying = false;
        videoPlayerFragment.storyDetailsPaused = false;
        SetTitle();

        ((MainActivity) getActivity()).lastSection = MainActivity.SECTION_STORY_DETAILS;

        aq.id(R.id.greyscreen).visibility(View.GONE);
    }

    public void SetTitle() {
        // Set title bar
        ((MainActivity) getActivity())
                .setActionBarTitle(story.name);
    }

    @Override
    public void onPause() {
        super.onPause();
        videoPlayerFragment.storyDetailsPaused = true;
        stopStoryVideo();
        aq.id(R.id.greyscreen).visibility(View.VISIBLE);
        handler.removeCallbacks(runPager);
    }



    public void refreshData() {
        // Just an example of refreshing the data from local storage,
        // after it was fetched from server and parsed.

        adapter.notifyDataSetChanged();
        aq.id(R.id.loadingRemakesProgress).visibility(View.INVISIBLE);
    }
    //endregion

    private void stopStoryVideo() {
        // Stop the video
        if (videoPlayerFragment != null) {
            try {
                videoPlayerFragment.fullStop();
            } catch (Exception ex) {}
        }
    }

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

// fetchmoreremakesprogress
    private void showFetchMoreRemakesProgress() {
        aq.id(R.id.loadingLayout).getView().setVisibility(View.VISIBLE);
        aq.id(R.id.swipe_container).getView().setPadding(0,0,0,25);
        aq.id(R.id.fetchMoreRemakesProgress).getView().setVisibility(View.VISIBLE);
        ((SmoothProgressBar) aq.id(R.id.fetchMoreRemakesProgress).getView()).progressiveStart();
    }

    private void hideFetchMoreRemakesProgress() {
        swipeLayout.setRefreshing(false);
        aq.id(R.id.loadingLayout).getView().setVisibility(View.GONE);
        ((SmoothProgressBar)aq.id(R.id.fetchMoreRemakesProgress).getView()).progressiveStop();
        aq.id(R.id.swipe_container).getView().setPadding(0,0,0,0);
    }
    //endregion


    //region *** Fullscreen ***
    private void handleEmbeddedVideoConfiguration(Configuration cfg) {
        int orientation = cfg.orientation;

        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                if(videoIsDisplayed) {
                    enterFullScreen();
                }
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
        aq.id(R.id.swipe_container).visibility(View.GONE);
        aq.id(R.id.loadingLayout).visibility(View.GONE);
        aq.id(R.id.moreRemakes).visibility(View.GONE);
        aq.id(R.id.fetchMoreRemakesProgress).visibility(View.GONE);

        // Show the video in full screen.
        View container = aq.id(R.id.storyDetailsVideoContainer).getView();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        container.getLayoutParams().width = width;
        container.getLayoutParams().height = height;
        if (videoPlayerFragment != null && videoPlayerFragment.getView() != null && videoPlayerFragment.getView().getLayoutParams() != null) {
            videoPlayerFragment.getView().getLayoutParams().width = width;
            videoPlayerFragment.getView().getLayoutParams().height = height;
        }

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
        aq.id(R.id.swipe_container).visibility(View.VISIBLE);
        aq.id(R.id.loadingLayout).visibility(View.VISIBLE);
        aq.id(R.id.moreRemakes).visibility(View.VISIBLE);
        aq.id(R.id.fetchMoreRemakesProgress).visibility(View.VISIBLE);

        // Return to original layout
        View container = aq.id(R.id.storyDetailsVideoContainer).getView();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        int portraitheight = (size.x * 9) / 16;
        container.getLayoutParams().width = width;
        container.getLayoutParams().height = portraitheight;
        if(videoPlayerFragment != null && videoPlayerFragment.getView().getLayoutParams() != null) {
            videoPlayerFragment.getView().getLayoutParams().width = width;
            videoPlayerFragment.getView().getLayoutParams().height = portraitheight;
        }

        // Actionbar
        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.showActionBar();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    //endregion

    //region video player calls
    private void playRemakeMovie(String remakeID, int remakeGridviwId) {
        videoPlayerFragment.remakePlaying = true;
        stopStoryVideo();
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

        stopStoryVideo();

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

    private void updateLikes(final String remakeID)
    {
//        TODO create updatelikes
    }

    private void updateViews(final String remakeID)
    {
//        TODO create updateViews
    }

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
}
//endregion

