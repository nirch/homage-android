package com.homage.app.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.views.ActivityHelper;

import java.util.HashMap;
import java.util.List;

public class StoryDetailsFragment extends Fragment {
    public String TAG = "TAG_StoryDetailsFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String VIDEO_FRAGMENT_TAG = "videoPlayerFragment";

    View rootView;
    LayoutInflater inflater;
    AQuery aq;
    GridView remakesGridView;

    static boolean createdOnce = false;
    public Story story;
    boolean shouldFetchMoreRemakes = false;
    VideoPlayerFragment videoPlayerFragment;
    int rowHeight;

    RemakesAdapter adapter;

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
                aq.id(R.id.loadingRemakesProgress).visibility(View.INVISIBLE);
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
                if (rowView == null)
                    rowView = inflater.inflate(R.layout.list_row_remake, remakesGridView, false);
                final Remake remake = (Remake) getItem(i);

                // Maintain 16/9 aspect ratio
                AbsListView.LayoutParams p = (AbsListView.LayoutParams)rowView.getLayoutParams();
                p.height = rowHeight / 2;
                rowView.setLayoutParams(p);

                // Configure
                AQuery aq = new AQuery(rowView);
                aq.id(R.id.remakeImage).image(remake.thumbnailURL, true, true, 256, R.drawable.glass_dark);
                aq.id(R.id.reportButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showReportDialogForRemake(remake.getOID().toString());
                    }
                });
                aq.id(R.id.watchRemakeButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, String.format("remakeOID: %s", remake.getOID()));
                        playRemakeMovie(remake.getOID());
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
    };

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

        User excludedUser = User.getCurrent();

        // Aspect Ratio
        MainActivity activity = (MainActivity)getActivity();
        rowHeight = (activity.screenWidth * 9) / 16;

        // Adapter
        adapter = new RemakesAdapter(getActivity(), story.getRemakes(excludedUser));
        remakesGridView = aq.id(R.id.remakesGridView).getGridView();
        remakesGridView.setAdapter(adapter);
        remakesGridView.setOnScrollListener(onGridViewScrollListener);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        //aq.id(R.id.remakesGridView).itemClicked(onItemClicked);
        aq.id(R.id.makeYourOwnButton).clicked(onClickedMakeYourOwnButton);
        //aq.id(R.id.storyDetailsPlayButton).clicked(onClickedPlayStoryVideo);
        //endregion
    }

    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_story_details, container, false);
        initialize();

        // Add embedded video player fragment.
        boolean shouldCreateChild = getArguments().getBoolean("shouldCreateStoryDetailsVideoFragment");

        // Ensure fragment created only once!
        FragmentManager childFM = getChildFragmentManager();
        videoPlayerFragment = (VideoPlayerFragment)childFM.findFragmentByTag(VIDEO_FRAGMENT_TAG);
        if (videoPlayerFragment == null) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            fm.beginTransaction();
            videoPlayerFragment = new VideoPlayerFragment();
            ft.add(R.id.storyDetailsVideoContainer, videoPlayerFragment, VIDEO_FRAGMENT_TAG);
            ft.commit();
        }

        // Allow orientation change.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

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
                } catch (Exception e) {}
            }
        });

        // When video playing, allow orientation changes.
        videoPlayerFragment.setOnStartedPlayback(new Runnable() {
            @Override
            public void run() {
                try {
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                } catch (Exception e) {}
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));


//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
        MainActivity main = (MainActivity)getActivity();
        main.refetchTopRemakesForStory(story);
        shouldFetchMoreRemakes = true;
//            }
//        }, 500);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleEmbeddedVideoConfiguration(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopStoryVideo();
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
//    private AdapterView.OnItemClickListener onItemClicked = new AdapterView.OnItemClickListener() {
//        @Override
//        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//            //Remake remake = remakes.get(i);
//            //if (remake == null) return;
//            //Log.d(TAG, String.format("remakeOID: %s", remake.getOID()));
//            //playRemakeMovie(remake.getOID());
//        }
//    };

    private AbsListView.OnScrollListener onGridViewScrollListener= new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if(firstVisibleItem + visibleItemCount >= 10){
                // End has been reached
                if (shouldFetchMoreRemakes) {
                    Log.d(TAG, "Will fetch more remakes.");
                    showFetchMoreRemakesProgress();
                    shouldFetchMoreRemakes = false;
                    MainActivity activity = (MainActivity)getActivity();
                    activity.refetchMoreRemakesForStory(story);
                }
            }
        }
    };

    private void showFetchMoreRemakesProgress() {
        aq.id(R.id.fetchMoreRemakesProgress).visibility(View.VISIBLE);
    }

    private void hideFetchMoreRemakesProgress() {
        aq.id(R.id.fetchMoreRemakesProgress).visibility(View.GONE);
    }
    //endregion


    //region *** Fullscreen ***
    private void handleEmbeddedVideoConfiguration(Configuration cfg) {
        int orientation = cfg.orientation;

        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                enterFullScreen();
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
        aq.id(R.id.storyDescription).visibility(View.GONE);
        aq.id(R.id.remakesContainer).visibility(View.GONE);

        // Show the video in full screen.
        View container = aq.id(R.id.storyDetailsVideoContainer).getView();
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) container.getLayoutParams();
        params.width =  metrics.widthPixels;
        params.height = metrics.heightPixels;

        container.setLayoutParams(params);

        // Actionbar
        getActivity().getActionBar().hide();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // (remove the top margin that is there for the action bar)
        container = getActivity().findViewById(R.id.bigContainer);
        android.support.v4.widget.DrawerLayout.LayoutParams params2 = (android.support.v4.widget.DrawerLayout.LayoutParams)container.getLayoutParams();
        params2.setMargins(0,0,0,0);
        container.setLayoutParams(params2);
    }

    void exitFullScreen() {
        Log.v(TAG, "Video, exit full screen");

        aq.id(R.id.makeYourOwnButton).visibility(View.VISIBLE);
        aq.id(R.id.storyDescription).visibility(View.VISIBLE);
        aq.id(R.id.remakesContainer).visibility(View.VISIBLE);

        // Return to original layout
        View container = aq.id(R.id.storyDetailsVideoContainer).getView();
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams)container.getLayoutParams();
        params.width =  metrics.widthPixels;
        params.height = (int) (216*metrics.density);
        container.setLayoutParams(params);

        // Actionbar
        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.showActionBar();
    }

    //endregion


    //region video player calls
    private void playRemakeMovie(String remakeID) {
        Intent myIntent = new Intent(this.getActivity(), FullScreenVideoPlayerActivity.class);
        Remake remake = Remake.findByOID(remakeID);
        if (remake.videoURL == null) {
            Toast.makeText(getActivity(), "Video unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri videoURL = Uri.parse(remake.videoURL);
        myIntent.putExtra(VideoPlayerFragment.K_FILE_URL, videoURL.toString());
        myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
        myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, true);
        myIntent.putExtra(VideoPlayerFragment.K_THUMB_URL, remake.thumbnailURL);

        myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_ID, remakeID);
        myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_TYPE, HEvents.H_REMAKE);
        myIntent.putExtra(HEvents.HK_VIDEO_ORIGINATING_SCREEN, HomageApplication.HM_STORY_DETAILS_TAB);

        startActivity(myIntent);
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
            FullScreenVideoPlayerActivity.openFullScreenVideoForURL(getActivity(), story.video, story.thumbnail, HEvents.H_STORY , story.getOID().toString(), HomageApplication.HM_STORY_DETAILS_TAB, true);
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

