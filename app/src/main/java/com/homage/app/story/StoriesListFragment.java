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

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.androidquery.AQuery;
import com.crashlytics.android.Crashlytics;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;

import java.io.File;
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

    int rowHeight;
    int desnityDPI;
    int index = 0;
    int top = 0;

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
            try {
                if (rowView == null)
                    rowView = inflater.inflate(R.layout.list_row_story, storiesListView, false);

                Story story = (Story)getItem(i);
                AQuery aq = new AQuery(rowView);
                aq.id(R.id.storyName).text(story.name);
                aq.id(R.id.storyRemakesCount).text(String.format("%d", story.remakesNum));

                // Maintain 16/9 aspect ratio
                AbsListView.LayoutParams p = (AbsListView.LayoutParams)rowView.getLayoutParams();
                p.height = rowHeight;
                rowView.setLayoutParams(p);

                // Thumbnail
                aq.id(R.id.storyImage).image(story.thumbnail, true, true, desnityDPI, R.drawable.glass_dark);

            } catch (InflateException ex) {
                Crashlytics.log(Log.ERROR, TAG, "Inflate exception in row of stories");
                Log.e(TAG, "Inflate exception in row of stories", ex);
            } catch (Exception ex) {
                Crashlytics.log(Log.ERROR, TAG, "Unexpected critical exception in row of stories.");
                Log.e(TAG, "Unexpected critical exception in row of stories", ex);
            }
            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return stories.size() == 0;
        }

    };

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            Activity a = getActivity();
            if(a != null) a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

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
        // Pixel density
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        desnityDPI = metrics.densityDpi;

        // Aspect Ratio
        MainActivity activity = (MainActivity)getActivity();
        rowHeight = (activity.screenWidth * 9) / 16;

        // AQeury
        aq = new AQuery(rootView);

        // Set the list adapter for the stories list view.
        stories = Story.allActiveStories();
        storiesListView = aq.id(R.id.storiesListView).getListView();
        createdOnce = true;

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

        // Force portrait.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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

        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.currentSection = MainActivity.SECTION_STORIES;

        ActionBar action = getActivity().getActionBar();
        if (action != null) {
            action.setTitle(R.string.nav_item_1_stories);
            ((MainActivity)getActivity()).onSectionAttached(MainActivity.SECTION_STORIES);
            action.show();
        }


        if (storiesListView.getAdapter() == null) {
            storiesListView.setAdapter(adapter);
        }
//        Get back to last location
        storiesListView.setSelectionFromTop(index, top);

        StopLoadingScreen();
    }

    public void StartLoadingScreen() {
        aq.id(R.id.storygreyscreen).visibility(View.VISIBLE);
        aq.id(R.id.loadingMeProgress).visibility(View.VISIBLE);
    }

    public void StopLoadingScreen() {
        aq.id(R.id.storygreyscreen).visibility(View.GONE);
        aq.id(R.id.loadingMeProgress).visibility(View.GONE);
    }


    @Override
    public void onPause() {
        super.onPause();
//        Save Location
        index = storiesListView.getFirstVisiblePosition();
        View v = storiesListView.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - storiesListView.getPaddingTop());
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


    public void refresh() {
        if (stories == null) {
            stories = Story.allActiveStories();
        } else {
            stories.clear();
            stories.addAll(Story.allActiveStories());
        }

        HMixPanel.sh().track("UserRefreshStories",null);

        adapter.notifyDataSetChanged();
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
            HashMap props = new HashMap<String ,String>();
            props.put("story" , story.name);
            props.put("index" , Integer.toString(i));
            HMixPanel.sh().track("SelectedAStory",props);

            if (story == null) return;

            // Remake the story
            showStoryDetails(story);
        }
    };
    //endregion
}
