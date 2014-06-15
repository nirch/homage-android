/**

 $$\   $$\  $$$$$$\  $$\      $$\  $$$$$$\   $$$$$$\  $$$$$$$$\
 $$ |  $$ |$$  __$$\ $$$\    $$$ |$$  __$$\ $$  __$$\ $$  _____|
 $$ |  $$ |$$ /  $$ |$$$$\  $$$$ |$$ /  $$ |$$ /  \__|$$ |
 $$$$$$$$ |$$ |  $$ |$$\$$\$$ $$ |$$$$$$$$ |$$ |$$$$\ $$$$$\
 $$  __$$ |$$ |  $$ |$$ \$$$  $$ |$$  __$$ |$$ |\_$$ |$$  __|
 $$ |  $$ |$$ |  $$ |$$ |\$  /$$ |$$ |  $$ |$$ |  $$ |$$ |
 $$ |  $$ | $$$$$$  |$$ | \_/ $$ |$$ |  $$ |\$$$$$$  |$$$$$$$$\
 \__|  \__| \______/ \__|     \__|\__|  \__| \______/ \________|

                           Main Activity
 */

package com.homage.app.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.story.StoryDetailsFragment;
import com.homage.model.Story;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    String TAG = "TAG_"+getClass().getName();

    AQuery aq;
    static final int SECTION_LOGIN      = 0;
    static final int SECTION_STORIES    = 1;
    static final int SECTION_ME         = 2;
    static final int SECTION_SETTINGS   = 3;
    static final int SECTION_HOWTO      = 4;
    static final int SECTION_STORIES_DETAILS      = 101;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    private int currentSection;

    //region *** Lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aq = new AQuery(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout)findViewById(R.id.drawer_layout));

        // Custom actionbar layout
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.actionbar_view);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.navButton).clicked(onClickedNavButton);
        //endregion
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        currentSection = position;
        switch (position) {
            case SECTION_STORIES:
                showStories();
                break;

            default:
                // Not implemented yet. Just put a place holder fragment for now.
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance(position))
                        .commit();
        }
    }
    //endregion

    //region *** Options ***
    public void onSectionAttached(int number) {
        switch (number) {
            case SECTION_STORIES:
                mTitle = getString(R.string.nav_item_1_stories);
                break;
            case SECTION_ME:
                mTitle = getString(R.string.nav_item_2_me);
                break;
            case SECTION_SETTINGS:
                mTitle = getString(R.string.nav_item_3_settings);
                break;
            case SECTION_HOWTO:
                mTitle = getString(R.string.nav_item_4_howto);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region *** Placeholder fragment for unimplemented screens ***

    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment;
            fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }
    //endregion

    public void showStoryDetails(Story story) {
        Log.d(TAG, String.format("Show story details: %s", story.name));
        currentSection = SECTION_STORIES_DETAILS;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, StoryDetailsFragment.newInstance(SECTION_STORIES_DETAILS))
                .commit();
    }

    public void showStories() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        currentSection = SECTION_STORIES;
        fragmentManager.beginTransaction()
                .replace(R.id.container, StoriesListFragment.newInstance(currentSection))
                .commit();
    }

    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
    private View.OnClickListener onClickedNavButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mNavigationDrawerFragment.isDrawerOpen()) {
                mNavigationDrawerFragment.close();
            } else {
                mNavigationDrawerFragment.open();
            }
        }
    };

    public void onBackPressed(){
        Log.d(TAG, "Pressed back button");

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (currentSection == SECTION_STORIES_DETAILS) {
            showStories();
        } else {
            super.onBackPressed();
        }

    }
    //endregion
}
