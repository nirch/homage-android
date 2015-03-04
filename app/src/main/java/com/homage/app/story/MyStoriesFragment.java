package com.homage.app.story;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.Display;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.androidquery.AQuery;
import com.homage.app.Download.DownloadTask;
import com.homage.app.R;
import com.homage.app.Utils.constants;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.app.recorder.RecorderActivity;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.uploader.UploadManager;
import com.homage.networking.analytics.HEvents;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class MyStoriesFragment extends Fragment {
    String TAG = "TAG_MyStoriesFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    ListView myStoriesListView;
    AQuery aq;

    // drag variables to animate sliding correctly
    int maxMovement;
    float firstX;
    float secondX;
    float firstY;
    float secondY;
    boolean fingerIsDown = false;

    public View getLastViewTouched() {
        return lastViewTouched;
    }

    View lastViewTouched;

    public void setShareIsDisplayed(boolean shareIsDisplayed) {
        this.shareIsDisplayed = shareIsDisplayed;
    }

    //    Animation related variables
    boolean shareIsDisplayed = true;
    boolean changedViews = false;

    public void setShareView(View shareView) {
        this.shareView = shareView;
    }

    View shareView;

    private User user;

    int desnityDPI;

    public static MyStoriesFragment newInstance(int sectionNumber, User user) {
        MyStoriesFragment fragment;
        fragment = new MyStoriesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.user = user;
        Log.d(fragment.TAG, String.format("Me screen for user: %s", user.getTag()));
        return fragment;
    }

    public void refreshUI(){

        // check for remakes. if there are none display a message.
        // if the process of fetching is taking place notify the user
        // if threre are remakes do not display any message.
        // if there are remakes and the process of fetching is taking place
        // make sure the progress is spinning to let the use know
        if (MainActivity.remakes.size() == 0) {
            aq.id(R.id.noRemakesMessage).visibility(View.VISIBLE);
            if(MainActivity.fetchingMyRemakes){
                aq.id(R.id.noRemakesMessage).getTextView().setText(
                        getActivity().getResources().getString(R.string.checking_for_remakes));
                ((MainActivity)getActivity()).showRefreshProgress();
            }
            else{
                aq.id(R.id.noRemakesMessage).getTextView().setText(
                        getActivity().getResources().getString(R.string.no_remakes));
                ((MainActivity)getActivity()).hideRefreshProgress();
            }
        } else {
            aq.id(R.id.noRemakesMessage).visibility(View.GONE);
            if(MainActivity.fetchingMyRemakes){
                ((MainActivity)getActivity()).showRefreshProgress();
            }
            else{
                ((MainActivity)getActivity()).hideRefreshProgress();
            }
        }

        HMixPanel.sh().track("MEUserRefresh",null);
        adapter.notifyDataSetChanged();
    }

    private void openRecorderForExistingRemake(Remake remake) {
        Log.d(TAG, "continue remake");
        Intent myIntent = new Intent(getActivity(), RecorderActivity.class);
        Bundle b = new Bundle();
        b.putString("remakeOID", remake.getOID());
        myIntent.putExtras(b);
        getActivity().startActivity(myIntent);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            Activity a = getActivity();
            if(a != null) a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void openRecorderForNewRemake(Story story) {
        Log.d(TAG, "new remake");
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.remakeStory(story);
    }

    BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return MainActivity.remakes.size();
        }

        @Override
        public Object getItem(int i) {
            return MainActivity.remakes.get(i);
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
                if (rowView == null)
                    rowView = inflater.inflate(R.layout.list_row_my_story, myStoriesListView, false);
                final Remake remake = (Remake) getItem(i);
                final Story story = remake.getStory();

                final HashMap props = new HashMap<String, String>();
                props.put("story", story.name);
                props.put("remake_id", remake.getOID());

                AQuery aq = new AQuery(rowView);
                aq.id(R.id.storyName).text(story.name);

                aq.id(R.id.storyImage).image(remake.thumbnailURL, true, true, desnityDPI/3, R.drawable.glass_dark);

                if (remake.status == Remake.Status.DONE.getValue()) {
                    aq.id(R.id.storyImage).visibility(View.VISIBLE);
                    aq.id(R.id.myPlayButton).visibility(View.VISIBLE);
                    aq.id(R.id.myShareButtonContainer).visibility(View.VISIBLE);
                    aq.id(R.id.loadingRemakeProgress).visibility(View.GONE);
                    aq.id(R.id.myMessageContainer).visibility(View.GONE);
                } else if(remake.status == Remake.Status.RENDERING.getValue() ||
                        remake.status == Remake.Status.STARTED_CREATION.getValue()) {
                    aq.id(R.id.storyImage).visibility(View.VISIBLE);
                    aq.id(R.id.myPlayButton).visibility(View.GONE);
                    aq.id(R.id.myShareButtonContainer).visibility(View.GONE);
                    aq.id(R.id.myMessageContainer).visibility(View.VISIBLE);
                    aq.id(R.id.loadingRemakeProgress).visibility(View.VISIBLE);
                }else{
                    aq.id(R.id.storyImage).visibility(View.VISIBLE);
                    aq.id(R.id.myPlayButton).visibility(View.GONE);
                    aq.id(R.id.myShareButtonContainer).visibility(View.GONE);
                    aq.id(R.id.loadingRemakeProgress).visibility(View.GONE);
                    aq.id(R.id.myMessageContainer).visibility(View.GONE);
                }

                bindUIButtonsToSwipeGesture(aq);

                // Delete
                aq.id(R.id.myDeleteButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, String.format("my story, clicked delete: %s", remake.getOID()));
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.askUserIfWantToDeleteRemake(remake);
                    }
                });

                if(remake.status == Remake.Status.DONE.getValue()) {
                    // Share
                    aq.id(R.id.myShareButton).clicked(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, String.format("my story, clicked share: %s", remake.getOID()));
                            shareRemake(remake);
                        }
                    });

                    // Play
                    aq.id(R.id.myPlayButton).clicked(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, String.format("my story, clicked play: %s", remake.getOID()));
                            HMixPanel.sh().track("MEPlayRemake", props);
                            File cacheDir = getActivity().getCacheDir();
                            File outFile = new File(cacheDir, MainActivity.getLocalVideoFile(remake.videoURL));
                            if (!outFile.exists()) {
                                FullScreenVideoPlayerActivity.openFullScreenVideoForURL(getActivity(),
                                        remake.videoURL, remake.thumbnailURL, HEvents.H_REMAKE, remake.getOID().toString(),
                                        HomageApplication.HM_ME_TAB, true);
                            }
                            else{
                                FullScreenVideoPlayerActivity.openFullScreenVideoForFile(getActivity(),
                                        outFile.getPath(), HEvents.H_REMAKE, remake.getOID().toString(),
                                        HomageApplication.HM_ME_TAB, true);
                            }
                        }
                    });

                }

                // Remake
                aq.id(R.id.myResetButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, String.format("my story, clicked remake: %s", remake.getOID()));
                        MainActivity mainActivity = (MainActivity) getActivity();

                        switch (remake.status) {
                            case 1: // IN PROGRESS
                                // Open recorder with this remake.
                                mainActivity.askUserIfWantToContinueRemake(remake);
                                break;
                            case 2: // RENDERING
                                // Open recorder with this remake.
                                mainActivity.askUserIfWantToContinueRemake(remake);
                                break;

                            case 4: // TIMEOUT
                                // Open recorder with this remake.
                                mainActivity.askUserIfWantToContinueRemake(remake);
                                break;

                            case 3: // DONE
                                // Open recorder with a new remake.
                                openRecorderForNewRemake(story);
                                props.put("remake_id", remake.getOID());
                                HMixPanel.sh().track("MEDoRemake", props);
                                break;

                            default:
                                Log.e(TAG, String.format("Wrong status for remake in my stories. %s", remake.getOID()));
                        }
                    }
                });

            } catch (InflateException ex) {
                Log.e(TAG, "Inflate exception in row of my stories", ex);
            }
            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return MainActivity.remakes.size() == 0;
        }
    };

    //region *** Share ***
    private void shareRemake(final Remake sharedRemake)
    {
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.shareRemake(sharedRemake);
    }
    //endregion


    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Pixel density
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        desnityDPI = metrics.densityDpi;

        // Inflate layout
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_my_stories, container, false);
        aq = new AQuery(rootView);

        myStoriesListView = aq.id(R.id.myStoriesListView).getListView();

        // Don't allow orientation change.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //
        aq.id(R.id.remakeAStoryButton).clicked(onClickedRemakeStories);

        refreshUI();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Set title bar
        ((MainActivity)getActivity()).setActionBarTitle(((MainActivity)getActivity()).getResources()
                .getString(R.string.nav_item_2_me));

        HMixPanel.sh().track("MEEnterTab",null);

        if (myStoriesListView.getAdapter() == null) {
            myStoriesListView.setAdapter(adapter);
        }

        refreshUI();

    }


    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
    private View.OnClickListener onClickedRemakeStories = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MainActivity mainActivity = (MainActivity)getActivity();
            mainActivity.showStories();
            mainActivity.setActionBarTitle(mainActivity.getResources().getString(R.string.nav_item_1_stories));
        }
    };
    //endregion


    // region Animation and Gesture
    /**
     *  ==========================
     *      ANIMATION BABY YEAH!!
     *  ==========================
     */

    private void bindUIButtonsToSwipeGesture(final AQuery aqView){

        aqView.id(R.id.swipeLayout).getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (checkForSwipeOrClickGesture(view, motionEvent, aqView)) return true;

                return false;
            }
        });

    }

    Rect outRect = new Rect();
    int[] location = new int[2];

    private boolean inViewInBounds(View view, int x, int y){
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }

    private boolean checkForSwipeOrClickGesture(View view, MotionEvent motionEvent, AQuery aqView) {

        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
            firstX = motionEvent.getX();
            secondX = firstX;
            firstY = motionEvent.getY();
            secondY = firstY;
//                            fingerIsDown = true;
            lastViewTouched = view;
            return true;
        }
        else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
        {

//                            fingerIsDown = false;
            int x = (int)motionEvent.getRawX();
            int y = (int)motionEvent.getRawY();
            View playButton =  aqView.id(R.id.myPlayButton).getView();
            View shareButton =  aqView.id(R.id.myShareButton).getView();
            View deleteButton =  aqView.id(R.id.myDeleteButton).getView();
            View resetButton =  aqView.id(R.id.myResetButton).getView();

            if(shareIsDisplayed && Math.abs(secondY - firstY) < 10
                    && Math.abs(secondX - firstX) < 10){

                if( inViewInBounds(playButton, x, y)){

                    playButton.performClick();

                }

                if( inViewInBounds(shareButton, x, y)){

                    shareButton.performClick();

                }

            }


            if(!shareIsDisplayed && Math.abs(secondY - firstY) < 10
                    && Math.abs(secondX - firstX) < 10){

                if( inViewInBounds(deleteButton, x, y)){

                    deleteButton.performClick();

                }

                if( inViewInBounds(resetButton, x, y)){

                    resetButton.performClick();

                }

            }

            firstX = secondX;
            firstY = secondY;

        }

        if(motionEvent.getAction() == MotionEvent.ACTION_MOVE){

            secondX = motionEvent.getX();
            secondY = motionEvent.getY();

            // if there is an open slide and user scrolls up or down close it.
            if(Math.abs(secondY - firstY) > Math.abs(secondX - firstX)){
                closeLastView();
            }
            // if its not a down or up motion, and it's a big enough motion
            // going right and share is displayed
            // slide share out
            else if(shareIsDisplayed && Math.abs(secondY - firstY) < Math.abs(secondX - firstX)
                    && secondX - firstX > 10){
                closeLastView();
                slideOutShare(view);
            }
            // if its not a down or up motion, and it's a big enough motion
            // going left and share is not displayed
            // slide share in
            else if(!shareIsDisplayed && Math.abs(secondY - firstY) < Math.abs(secondX - firstX)
                    && secondX - firstX < 10){
                closeLastView();
                slideInShare(view);
            }
        }
        return false;
    }

    private void slideOutShare(final View viewTouched) {

        if(shareIsDisplayed) {

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            final int width = size.x;
            final int height = size.y;
            final int portraitheight = (size.x * 9) / 16;

            shareIsDisplayed = false;


            ((RelativeLayout) viewTouched.getParent()).findViewById(R.id.mainContainer).animate().xBy(width).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (!shareIsDisplayed && viewTouched == lastViewTouched) {
                        ((RelativeLayout) viewTouched.getParent()).findViewById(R.id.other_controls_container).setVisibility(View.VISIBLE);
                        changedViews = false;
                    }

                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
        }
    }

    public void slideInShare(final View viewTouched) {

        if(!shareIsDisplayed) {

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            final int width = size.x;
            final int height = size.y;
            final int portraitheight = (size.x * 9) / 16;

            shareIsDisplayed = true;

            ((RelativeLayout) viewTouched.getParent()).findViewById(R.id.other_controls_container).setVisibility(View.GONE);

            ((RelativeLayout) viewTouched.getParent()).findViewById(R.id.mainContainer).animate().xBy(-width);

        }
    }

    private void closeLastView() {
        // If changed view close view if opened and go to next view
        if(shareView != null && lastViewTouched != shareView){
            if(!shareIsDisplayed){
                changedViews = true;
                slideInShare(shareView);

                shareIsDisplayed = true;
            }
        }

        shareView = lastViewTouched;
    }

    // endregion
}
