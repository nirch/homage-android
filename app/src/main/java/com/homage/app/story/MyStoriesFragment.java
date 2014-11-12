package com.homage.app.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Display;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.app.recorder.RecorderActivity;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.uploader.UploadManager;
import com.homage.networking.analytics.HEvents;

import java.io.File;
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

    int rowHeight;

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

                // Maintain 16/9 aspect ratio
                AbsListView.LayoutParams p = (AbsListView.LayoutParams)rowView.getLayoutParams();
                p.height = rowHeight;
                rowView.setLayoutParams(p);

                if (i > 3) {
                    aq.id(R.id.storyImage).image(remake.thumbnailURL, true, true, 256, R.drawable.glass_dark, null, R.anim.animation_fadein_with_zoom);
                } else {
                    aq.id(R.id.storyImage).image(remake.thumbnailURL, true, true, 256, R.drawable.glass_dark);
                }

                if (remake.status == Remake.Status.DONE.getValue()) {
                    aq.id(R.id.myPlayButton).visibility(View.VISIBLE);
                    aq.id(R.id.myShareButton).visibility(View.VISIBLE);
                } else {
                    aq.id(R.id.myPlayButton).visibility(View.INVISIBLE);
                    aq.id(R.id.myShareButton).visibility(View.INVISIBLE);
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
    private void shareRemake(final Remake sharedRemake) {

        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.shareRemake(sharedRemake);

    }

    //endregion


    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Aspect Ratio
        MainActivity activity = (MainActivity)getActivity();
        rowHeight = (activity.screenWidth * 9) / 16;

        // Inflate layout
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_my_stories, container, false);
        aq = new AQuery(rootView);

        // Set the list adapter for the stories list view.
        remakes = user.allAvailableRemakesLatestOnTop();
        myStoriesListView = aq.id(R.id.myStoriesListView).getListView();

        // Don't allow orientation change.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Show actionbar
        getActivity().getActionBar().show();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
        UploadManager.sh().checkUploader();
    }

    @Override
    public void onResume()
    {
        super.onResume();
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

}
