package com.homage.app.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;
import com.homage.app.R;
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

import java.util.HashMap;
import java.util.List;

public class MyStoriesFragment extends Fragment {
    String TAG = "TAG_MyStoriesFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    List<Remake> remakes;
    ListView myStoriesListView;
    AQuery aq;

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

    public void refresh() {
        if (remakes == null) {
            remakes = user.allAvailableRemakesLatestOnTop();
        } else {
            remakes.clear();
            remakes.addAll(user.allAvailableRemakesLatestOnTop());
        }

        if (remakes.size() == 0) {
            aq.id(R.id.noRemakesMessage).visibility(View.VISIBLE);
        } else {
            aq.id(R.id.noRemakesMessage).visibility(View.GONE);
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
                    aq.id(R.id.myPlayButton).visibility(View.VISIBLE);
                    aq.id(R.id.myShareButtonContainer).visibility(View.VISIBLE);
                } else {
                    aq.id(R.id.myPlayButton).visibility(View.GONE);
                    aq.id(R.id.myShareButtonContainer).visibility(View.GONE);
                }

                // Delete
                aq.id(R.id.myDeleteButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, String.format("my story, clicked delete: %s", remake.getOID()));
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.askUserIfWantToDeleteRemake(remake);
                    }
                });

                // Share
                aq.id(R.id.myShareButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, String.format("my story, clicked share: %s", remake.getOID()));
                        User user = User.getCurrent();
                        if (user.isGuest()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            AlertDialog alert = builder.setMessage(R.string.share_signed_in_only)
                                    .setTitle(R.string.join_us_title)
                                    .setNegativeButton(R.string.ok_got_it, null)
                                    .setPositiveButton(R.string.join_us_title, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            MainActivity activity = (MainActivity) getActivity();
                                            activity.showLogin();
                                        }
                                    })
                                    .create();
                            alert.show();
                            return;
                        }
                        shareRemake(remake);
                    }
                });

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

                // Play
                aq.id(R.id.myPlayButton).clicked(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, String.format("my story, clicked play: %s", remake.getOID()));
                        HMixPanel.sh().track("MEPlayRemake", props);
                        FullScreenVideoPlayerActivity.openFullScreenVideoForURL(getActivity(), remake.videoURL, remake.thumbnailURL, HEvents.H_REMAKE, remake.getOID().toString(), HomageApplication.HM_ME_TAB, true);
                    }
                });

            } catch (InflateException ex) {
                Log.e(TAG, "Inflate exception in row of my stories", ex);
            }
            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return remakes.size() == 0;
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

        // Set title bar
        ((MainActivity) getActivity())
                .setActionBarTitle(getActivity().getResources().getString(R.string.nav_item_2_me));

        // Pixel density
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        desnityDPI = metrics.densityDpi;

        // Inflate layout
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_my_stories, container, false);
        aq = new AQuery(rootView);

        // Set the list adapter for the stories list view.
        remakes = user.allAvailableRemakesLatestOnTop();

        myStoriesListView = aq.id(R.id.myStoriesListView).getListView();

        // Don't allow orientation change.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //
        aq.id(R.id.remakeAStoryButton).clicked(onClickedRemakeStories);



        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        UploadManager.sh().checkUploader();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ((MainActivity)getActivity()).setActionBarTitle(((MainActivity)getActivity()).getResources()
                .getString(R.string.nav_item_2_me));

//        Make Stories Loading screen disappear... just in case
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment f = fragmentManager.findFragmentByTag(MainActivity.FRAGMENT_TAG_STORIES);
        if (f!=null) {
            ((StoriesListFragment)f).StopLoadingScreen();
        }

        HMixPanel.sh().track("MEEnterTab",null);

        if (myStoriesListView.getAdapter() == null) {
            myStoriesListView.setAdapter(adapter);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            MainActivity main = (MainActivity)getActivity();
            if (main != null) main.refetchRemakesForCurrentUser();
            }
        }, 500);



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
}
