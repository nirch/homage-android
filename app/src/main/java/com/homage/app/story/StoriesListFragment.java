/**
       Homage.
     ,         ,
     |\\\\ ////|
     | \\\V/// |
     |  |~~~|  |
     |  |===|  |
     |  |   |  |
     |  |   |  |
      \ |   | /
       \|===|/
        '---'
   Stories Fragment
 */
package com.homage.app.story;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.main.SettingsActivity;
import com.homage.app.recorder.RecorderActivity;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;
import com.homage.views.ActivityHelper;

import java.util.HashMap;
import java.util.List;

public class StoriesListFragment extends Fragment {
    String TAG = "TAG_StoriesListFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    List<Story> stories;
    ListView storiesListView;
    AQuery aq;
    ProgressDialog pd;

    static boolean createdOnce = false;

    BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return stories.size();
        }

        @Override
        public Object getItem(int i) {
            return stories.get(i);
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
            if (rowView == null) rowView = inflater.inflate(R.layout.list_row_story, storiesListView, false);
            Story story = (Story)getItem(i);
            AQuery aq = new AQuery(rowView);
            aq.id(R.id.storyName).text(story.name);
            aq.id(R.id.storyRemakesCount).text(String.format("#%d", story.remakesNum));
            aq.id(R.id.storyImage).image(story.thumbnail, true, true, 200, R.drawable.glass_dark);
            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return stories.size() == 0;
        }

    };

    public static StoriesListFragment newInstance(int sectionNumber) {
        StoriesListFragment fragment;
        fragment = new StoriesListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public StoriesListFragment() {
        super();
    }

    private void initialize() {
        // AQeury
        aq = new AQuery(rootView);

        // Set the list adapter for the stories list view.
        stories = Story.allActiveStories();
        storiesListView = aq.id(R.id.storiesListView).getListView();
        storiesListView.setAdapter(adapter);

//        // Refetch the stories in the background.
//        if (!createdOnce) {
//            HomageServer.sh().refetchStories();
//        }
        createdOnce = true;

        if (stories.size() > 0) {
            //aq.id(R.id.loadingStoriesProgress).visibility(View.GONE);
        }

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.storiesListView).itemClicked(onItemClicked);
        //endregion
    }

    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_stories, container, false);
        initialize();
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
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    //endregion

    private void showStoryDetails(Story story) {
        if (story == null) return;

        SharedPreferences pref = HomageApplication.getSettings(getActivity());

        boolean skipStoryDetails = pref.getBoolean("settings_skip_story_details", false);
        MainActivity main = (MainActivity)this.getActivity();

        if (skipStoryDetails) {
            // Used for debugging. Skips the story details screen.
            // And just opens the recorder for a remake of this story.
            main.remakeStory(story);
        } else {
            // Will show the story details screen.
            main.showStoryDetails(story);
        }

    }


    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------
    private AdapterView.OnItemClickListener onItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            User user = User.getCurrent();
            if (user == null) return;

            Story story = stories.get(i);
            if (story == null) return;

            // Remake the story
            showStoryDetails(story);
        }
    };
    //endregion
}
