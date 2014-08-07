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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.androidquery.AQuery;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.homage.app.R;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.app.recorder.RecorderActivity;
import com.homage.app.story.StoriesListFragment;
import com.homage.app.story.StoryDetailsFragment;
import com.homage.app.user.LoginActivity;
import com.homage.app.story.MyStoriesFragment;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;
import java.util.HashMap;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    String TAG = "TAG_MainActivity";

    AQuery aq;
    AQuery actionAQ;

    static final int SECTION_LOGIN      = 0;
    static final int SECTION_STORIES    = 1;
    static final int SECTION_ME         = 2;
    static final int SECTION_SETTINGS   = 3;
    static final int SECTION_HOWTO      = 4;
    static final int SECTION_STORY_DETAILS      = 101;


    static final int REQUEST_CODE_RECORDER = 10001;


    static final String FRAGMENT_TAG_ME = "fragment me";
    static final String FRAGMENT_TAG_STORIES = "fragment stories";
    static final String FRAGMENT_TAG_MY_STORIES = "fragment my stories";

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private StoryDetailsFragment storyDetailsFragment;
    private CharSequence mTitle;

    private Remake lastRemakeSentToRender;

    private int currentSection;
    private Story currentStory;

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    ProgressDialog pd;
    MovieProgressFragment movieProgressFragment;

    GoogleCloudMessaging gcm;
    String regid;

    //region *** Lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);

        aq = new AQuery(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        lastRemakeSentToRender = null;

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout)findViewById(R.id.drawer_layout));

        // Custom actionbar layout
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_view);
        actionAQ = new AQuery(getActionBar().getCustomView());

        // Movie creation progress bar fragment
        movieProgressFragment = (MovieProgressFragment)getSupportFragmentManager()
                .findFragmentById(R.id.movieProgressBar);


        // Refresh stories
        showRefreshProgress();
        HomageServer.sh().refetchStories();

        // Force portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.navButton).clicked(onClickedNavButton);

        actionAQ.id(R.id.refreshButton).clicked(onClickedRefreshButton);
        //endregion
    }

    @Override
    protected void onResume() {
        super.onResume();
        initObservers();
        updateLoginState();
        updateRenderProgressState();
        hideRefreshProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeObservers();
    }

    @Override
    protected void onDestroy() {
        HMixPanel.sh().flush();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != REQUEST_CODE_RECORDER) return;

        Log.d(TAG, String.format("Returned from recorder with result code: %d", resultCode));

        switch (resultCode) {
            case RecorderActivity.DISMISS_REASON_FINISHED_REMAKE:
                // User sent a remake to rendering. Show the create movie progress bar
                // To keep track of the rendering.
                String remakeOID = resultData.getStringExtra("remakeOID");
                if (remakeOID==null) return;
                Remake renderedRemake = Remake.findByOID(remakeOID);
                if (renderedRemake==null) {
                    Log.e(TAG, "Critical error. Recorder sent remake to rendering, but not found in local storage.");
                    break;
                }
                Log.d(TAG, String.format("Sent remake. Will show progress for remake %s", remakeOID));
                movieProgressFragment.showProgressForRemake(renderedRemake);
                showStories();
                break;

            case RecorderActivity.DISMISS_REASON_USER_ABORTED_PRESSING_X:
                // No need to do anything
                break;

            default:
                // Do nothing
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(final int position) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handleDrawerSectionSelection(position);
            }
        }, 200);
    }
    //endregion

    public void handleDrawerSectionSelection(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case SECTION_LOGIN:
                User user = User.getCurrent();
                if (user.isGuest()) {
                    // User is guest. Will show the join login screen.
                    showLogin();
                } else {
                    // User is logged in. Will logout user and go back to the first login screen.
                    logout();
                }
                break;

            case SECTION_STORIES:
                currentSection = position;
                showStories();
                break;

            case SECTION_ME:
                currentSection = position;
                showMyStories();
                break;

            case SECTION_SETTINGS:
                showSettings();
                break;

            case SECTION_HOWTO:
                showHowTo();
                break;

            default:
                // Not implemented yet. Just put a place holder fragment for now.
                currentSection = position;
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.animation_fadein_with_zoom, R.anim.animation_fadeout_with_zoom)
                        .replace(R.id.container, PlaceholderFragment.newInstance(position))
                        .commitAllowingStateLoss();
        }
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    //region *** Observers ***
    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onStoriesUpdated, new IntentFilter(HomageServer.INTENT_STORIES));
        lbm.registerReceiver(onRemakeCreation, new IntentFilter(HomageServer.INTENT_REMAKE_CREATION));
        lbm.registerReceiver(onUserLogin, new IntentFilter(HomageServer.INTENT_USER_CREATION));
        lbm.registerReceiver(onRemakesForStoryUpdated, new IntentFilter(HomageServer.INTENT_REMAKES_FOR_STORY));
        lbm.registerReceiver(onRemakesForUserUpdated, new IntentFilter(HomageServer.INTENT_USER_REMAKES));
        lbm.registerReceiver(onRemakeDeletion, new IntentFilter((HomageServer.INTENT_REMAKE_DELETION)));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onStoriesUpdated);
        lbm.unregisterReceiver(onRemakeCreation);
        lbm.unregisterReceiver(onUserLogin);
        lbm.unregisterReceiver(onRemakesForStoryUpdated);
        lbm.unregisterReceiver(onRemakesForUserUpdated);
        lbm.unregisterReceiver(onRemakeDeletion);
    }

    private void refreshMyStoriesIfCurrentSection() {
        hideRefreshProgress();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment f = fragmentManager.findFragmentByTag(FRAGMENT_TAG_ME);
        if (f!=null) {
            ((MyStoriesFragment)f).refresh();
        }
    }

    // Observers handlers
    private BroadcastReceiver onStoriesUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);
            hideRefreshProgress();

            if (currentSection == SECTION_STORIES && success) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment f = fragmentManager.findFragmentByTag(FRAGMENT_TAG_STORIES);
                if (f!=null) {
                    ((StoriesListFragment)f).refresh();
                }
                //showStories();
            }
        }
    };

    private BroadcastReceiver onRemakeCreation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pd.dismiss();
            boolean success = intent.getBooleanExtra(Server.SR_SUCCESS, false);
            HashMap<String, Object> responseInfo = (HashMap<String, Object>)intent.getSerializableExtra(Server.SR_RESPONSE_INFO);

            if (success) {
                assert(responseInfo != null);
                String remakeOID = (String)responseInfo.get("remakeOID");
                if (remakeOID == null) return;

                // Open recorder for the remake.
                Intent myIntent = new Intent(MainActivity.this, RecorderActivity.class);
                Bundle b = new Bundle();
                b.putString("remakeOID", remakeOID);
                myIntent.putExtras(b);
                MainActivity.this.startActivityForResult(myIntent, REQUEST_CODE_RECORDER);
            }
        }
    };

    // Observers handlers
    private BroadcastReceiver onRemakesForStoryUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HashMap<String, Object> requestInfo = Server.requestInfoFromIntent(intent);
            String storyOID = (String)requestInfo.get("storyOID");
            if (storyDetailsFragment.story.getOID().equals(storyOID)) {
                hideRefreshProgress();
                storyDetailsFragment.refreshData();
            }
        }
    };

    private BroadcastReceiver onRemakesForUserUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshMyStoriesIfCurrentSection();
        }
    };

    private BroadcastReceiver onRemakeDeletion = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (pd != null) pd.dismiss();
            boolean success = intent.getBooleanExtra(Server.SR_SUCCESS, false);
            HashMap<String, Object> responseInfo = (HashMap<String, Object>) intent.getSerializableExtra(Server.SR_RESPONSE_INFO);

            if (success && currentSection == SECTION_ME) {
                refreshMyStoriesIfCurrentSection();
            }
        }
    };

    private BroadcastReceiver onUserLogin = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateLoginState();
        }
    };
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
        actionBar.setDisplayShowTitleEnabled(false);
    }

    public void showActionBar() {
        // Actionbar
        getActionBar().show();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        View container = findViewById(R.id.bigContainer);
        android.support.v4.widget.DrawerLayout.LayoutParams params2 = (android.support.v4.widget.DrawerLayout.LayoutParams)container.getLayoutParams();
        int m = (int)(45.0f*metrics.density);
        params2.setMargins(0, m, 0, 0);
        container.setLayoutParams(params2);
    }

    public void updateLoginState() {
        User user = User.getCurrent();
        if (user==null) {
            Log.d(TAG, "No logged in user.");
        } else {
            Log.d(TAG, String.format("Current user:%s", user.getTag()));
        }
        mNavigationDrawerFragment.refresh();
    }

    public void updateRenderProgressState() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment f = null;

        switch (currentSection) {
            case SECTION_STORIES:
                f = fragmentManager.findFragmentByTag(FRAGMENT_TAG_ME);
                if (f==null) break;
                ((StoriesListFragment)f).updateRenderProgressState();
                break;
            case SECTION_ME:
                f = fragmentManager.findFragmentByTag(FRAGMENT_TAG_ME);
                if (f==null) break;
                ((MyStoriesFragment)f).updateRenderProgressState();
                break;
            case SECTION_STORY_DETAILS:
                f = fragmentManager.findFragmentByTag(FRAGMENT_TAG_ME);
                if (f==null) break;
                ((StoryDetailsFragment)f).updateRenderProgressState();
                break;
        }

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

    //region *** Show other fragments & activities ***
    public void showLogin() {
        Intent myIntent = new Intent(this, LoginActivity.class);
        startActivity(myIntent);
    }

    public void logout() {
        User.logoutAllUsers();
        Intent myIntent = new Intent(this, LoginActivity.class);
        myIntent.putExtra(LoginActivity.SK_ALLOW_GUEST_LOGIN, true);
        myIntent.putExtra(LoginActivity.SK_START_MAIN_ACTIVITY_AFTER_LOGIN, true);
        startActivity(myIntent);
        finish();
    }

    public void showStoryDetails(Story story) {
        showActionBar();

        currentSection = SECTION_STORY_DETAILS;
        currentStory = story;
        FragmentManager fragmentManager = getSupportFragmentManager();
        storyDetailsFragment = StoryDetailsFragment.newInstance(SECTION_STORY_DETAILS, story);
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.animation_fadein, R.anim.animation_fadeout)
                .replace(R.id.container, storyDetailsFragment)
                .commitAllowingStateLoss();
    }

    public void showStories() {
        showActionBar();

        FragmentManager fragmentManager = getSupportFragmentManager();
        currentSection = SECTION_STORIES;
        currentStory = null;
        fragmentManager.beginTransaction()
                .replace(R.id.container, StoriesListFragment.newInstance(currentSection), FRAGMENT_TAG_STORIES)
                .commitAllowingStateLoss();
    }

    public void showMyStories() {
        showActionBar();

        User user = User.getCurrent();
        if (user==null) return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        currentSection = SECTION_ME;
        currentStory = null;
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.animation_fadein, R.anim.animation_fadeout)
                .replace(R.id.container, MyStoriesFragment.newInstance(currentSection, user), FRAGMENT_TAG_ME)
                .commitAllowingStateLoss();
    }

    public void showSettings() {
        Intent myIntent = new Intent(this, SettingsActivity.class);
        startActivity(myIntent);
    }

    public void showHowTo() {
        Intent myIntent = new Intent(this, FullScreenVideoPlayerActivity.class);
        Uri videoURL = Uri.parse("android.resource://com.homage.app/raw/intro_video");
        myIntent.putExtra(VideoPlayerFragment.K_FILE_URL, videoURL.toString());
        myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
        myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, true);
        startActivity(myIntent);
    }
    //endregion

    public void refetchRemakesForStory(Story story) {
        HomageServer.sh().refetchRemakesForStory(story.getOID(), null, null, null); // Implement partial fetch
        showRefreshProgress();
    }

    public void refetchRemakesForCurrentUser() {
        User user = User.getCurrent();
        if (user==null) return;
        HomageServer.sh().refetchRemakesForUser(user.getOID(), null);
        showRefreshProgress();
    }

    //region *** refresh ***
    public void showRefreshProgress() {
        actionAQ.id(R.id.refreshProgress).visibility(View.VISIBLE);
        actionAQ.id(R.id.refreshButton).visibility(View.GONE);
    }

    public void hideRefreshProgress() {
        actionAQ.id(R.id.refreshProgress).visibility(View.GONE);
        actionAQ.id(R.id.refreshButton).visibility(View.VISIBLE);
    }
    //endregion

    //region *** more actions ***
    public void remakeStory(Story story) {
        if (story == null) return;

        User user = User.getCurrent();
        if (user == null) return;

        Remake unfinishedRemake = user.unfinishedRemakeForStory(story);
        if (unfinishedRemake == null) {
            // No info about an unfinished remake exists in local storage.
            // Create a new remake.
            sendRemakeStoryRequest(story);
        } else {
            // An unfinished remake exists. Ask user if she want to continue this remake
            // or start a new one.
            askUserIfWantToContinueRemake(unfinishedRemake);
        }
    }

    public void sendRemakeStoryRequest(Story story) {
        if (story == null) return;

        User user = User.getCurrent();
        if (user == null) return;

        Resources res = getResources();
        pd = new ProgressDialog(this);
        pd.setTitle(res.getString(R.string.pd_title_please_wait));
        pd.setMessage(res.getString(R.string.pd_msg_creating_movie));
        pd.setCancelable(true);
        pd.show();

        // Send the request to the server.
        HomageServer.sh().createRemake(
                story.getOID(),
                user.getOID(),
                Remake.DEFAULT_RENDER_OUTPUT_HEIGHT,
                null);
    }

    public void askUserIfWantToDeleteRemake(final Remake remake) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pd_title_delete_remake_question);

        // set dialog message
        builder
            .setMessage(R.string.pd_msg_delete_remake_question)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, String.format("Chosen to delete remake %s", remake.getOID()));
                    deleteRemake(remake);
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, String.format("Cancel delete remake %s", remake.getOID()));
                }
            });
        builder.create().show();
    }

    public void deleteRemake(Remake remake) {
        if (remake == null) return;

        Resources res = getResources();
        pd = new ProgressDialog(this);
        pd.setTitle(res.getString(R.string.pd_title_please_wait));
        pd.setMessage(res.getString(R.string.pd_msg_deleting_remake));
        pd.setCancelable(true);
        pd.show();

        // Send the request to the server.
        HomageServer.sh().deleteRemake(
                remake.getOID(),
                null);
    }

    public void askUserIfWantToContinueRemake(Remake remake) {
        if (remake == null) return;

        final Remake theRemake = remake;
        final Story theStory = remake.getStory();
        final User user = User.getCurrent();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.continue_remake_message);
        builder.setItems(
                new CharSequence[] {"continue", "New", "Cancel"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Open recorder for the remake.
                                String remakeOID = theRemake.getOID();
                                Intent myIntent = new Intent(MainActivity.this, RecorderActivity.class);
                                Bundle b = new Bundle();
                                b.putString("remakeOID", remakeOID);
                                myIntent.putExtras(b);
                                MainActivity.this.startActivityForResult(myIntent, REQUEST_CODE_RECORDER);
                                break;
                            case 1:
                                user.deleteAllUnfinishedRemakesForStory(theStory);
                                sendRemakeStoryRequest(theStory);
                                break;
                            case 2:
                                break;
                        }
                    }
                }
        );
        builder.create().show();
    }
    //endregion

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

    private View.OnClickListener onClickedRefreshButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {


            switch (currentSection) {
                case SECTION_STORIES:
                    showRefreshProgress();
                    HomageServer.sh().refetchStories();
                    break;

                case SECTION_STORY_DETAILS:
                    if (currentStory==null) break;
                    refetchRemakesForStory(MainActivity.this.currentStory);
                    break;

                case SECTION_ME:
                    refetchRemakesForCurrentUser();
                    break;
            }
        }
    };

    public void onBackPressed(){
        Log.d(TAG, "Pressed back button");

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (currentSection == SECTION_STORY_DETAILS) {
            showStories();
        } else {
            super.onBackPressed();
        }

    }
    //endregion
}
