package com.homage.app.story;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.MainActivity;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.app.recorder.RecorderActivity;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;

import java.util.HashMap;
import java.util.List;

public class StoryDetailsFragment extends Fragment {
    public String TAG = "TAG_StoryDetailsFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    AQuery aq;

    private Story story;

    public static StoryDetailsFragment newInstance(int sectionNumber, Story story) {
        StoryDetailsFragment fragment;
        fragment = new StoryDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.story = story;
        Log.d(fragment.TAG, String.format("Showing story details: %s", story.name));

        // Refetch remakes for this story.
        HomageServer.sh().refetchRemakesForStory(story.getOID(), null);

        return fragment;
    }

    private void initialize() {
        aq = new AQuery(rootView);

        aq.id(R.id.storyTestImageView).image(story.thumbnail, true, true, 200, R.drawable.glass_dark);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_story_details, container, false);
        initialize();

        // Example of embedded video player
        View videoContainer = aq.id(R.id.videoFragmentContainer).getView();

        VideoPlayerFragment videoPlayer = new VideoPlayerFragment();
        getFragmentManager().beginTransaction().replace(
                R.id.videoFragmentContainer,
                new Fragment()
        );

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onResume() {
        super.onResume();
        initObservers();
    }

    @Override
    public void onPause() {
        super.onPause();
        removeObservers();
    }

    protected void refreshData() {
        // Just an example of refreshing the data from local storage,
        // after it was fetched from server and parsed.
        // TODO: finish implementation of this screen.
        List<Remake> remakes = story.getRemakes();
        Log.d(TAG, String.format("%d remakes for this story", remakes.size()));
    }

    //region *** Observers ***
    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(onRemakesForStoryUpdated, new IntentFilter(HomageServer.INTENT_REMAKES_FOR_STORY));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(onRemakesForStoryUpdated);
    }

    // Observers handlers
    private BroadcastReceiver onRemakesForStoryUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HashMap<String, Object> requestInfo = Server.requestInfoFromIntent(intent);
            String storyOID = (String)requestInfo.get("storyOID");
            if (StoryDetailsFragment.this.story.getOID().equals(storyOID)) {
                StoryDetailsFragment.this.refreshData();
            }
        }
    };
    //endregion


}
