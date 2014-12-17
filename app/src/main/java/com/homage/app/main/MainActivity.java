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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;

import com.androidquery.AQuery;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.homage.app.R;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.app.player.RemakeVideoFragment;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.app.recorder.RecorderActivity;
import com.homage.app.story.StoriesListFragment;
import com.homage.app.story.StoryDetailsFragment;
import com.homage.app.user.LoginActivity;
import com.homage.app.story.MyStoriesFragment;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends ActionBarActivity
        implements
            NavigationDrawerFragment.NavigationDrawerCallbacks {

    String TAG = "TAG_MainActivity";

    public static final String SK_START_MAIN_WITH = "startMainWith";

    AQuery aq;
    AQuery actionAQ;

    static final int SECTION_LOGIN      = 0;
    static final int SECTION_STORIES    = 1;
    static final int SECTION_ME         = 2;
    static final int SECTION_SETTINGS   = 3;
    static final int SECTION_HOWTO      = 4;
    static final int SECTION_STORY_DETAILS      = 101;
    static final int SECTION_REMAKE_VIDEO      = 911;

    static final int SHARE_METHOD_COPY_URL  = 0;
    static final int SHARE_METHOD_FACEBOOK  = 1;
    static final int SHARE_METHOD_WHATS_APP = 2;
    static final int SHARE_METHOD_EMAIL     = 3;
    static final int SHARE_METHOD_MESSAGE   = 4;
    static final int SHARE_METHOD_WEIBO     = 5;
    static final int SHARE_METHOD_TWITTER   = 6;

    public static final int REQUEST_CODE_RECORDER = 10001;

    static final String FRAGMENT_TAG_ME = "fragment me";
    static final String FRAGMENT_TAG_STORIES = "fragment stories";
    static final String FRAGMENT_TAG_MY_STORIES = "fragment my stories";
    public static final String FRAGMENT_TAG_REMAKE_VIDEO = "fragment remake video";
    public static final String FRAGMENT_TAG_STORY_DETAILS = "fragment story details";

    private NavigationDrawerFragment mNavigationDrawerFragment;
    int defaultSelection;
    int mPositionClicked;
    int mOnResumeChangeToSection = -1;
    boolean mNavigationItemClicked = false;
    private StoryDetailsFragment storyDetailsFragment;
    private RemakeVideoFragment remakeVideoFragment;
    private CharSequence mTitle;

    private Remake lastRemakeSentToRender;

    private int currentSection;
    public Story currentStory;

    ProgressDialog pd;
    MovieProgressFragment movieProgressFragment;

    // GCM
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    GoogleCloudMessaging gcm;
    String regid;
    AtomicInteger msgId = new AtomicInteger();
    Context context;

    public int screenWidth;
    public int screenHeight;

    // Sharing
    HashMap<String, String> shareMimeTypes = new HashMap<String, String>();
    HashMap<String, Integer> shareMethods = new HashMap<String, Integer>();

    //region *** Lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Crashlytics.log("onCreate MainActivity. Loaded content view.");

        context = getApplicationContext();

        // Screen info (portrait assumed)
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = Math.min(size.x, size.y);
        screenHeight = Math.max(size.x, size.y);

        View decorView = getWindow().getDecorView();
        //int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        //decorView.setSystemUiVisibility(uiOptions);

        aq = new AQuery(this);


        // Navigation drawer
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        lastRemakeSentToRender = null;

        // Navigate to another default section if requested
        navigateToSectionIfRequested();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout)findViewById(R.id.drawer_layout),
                defaultSelection
                );
        handleDrawerSectionSelection(defaultSelection);

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

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = HomageApplication.getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        initSharing();

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
        sillyOnResumeHackDetectingFinishedRemake();
        navigateToSectionIfRequested();
        initObservers();
        updateLoginState();
        updateRenderProgressState();
        hideRefreshProgress();

        // If requested to change section on resume. navigate to that section.
        if (mOnResumeChangeToSection > -1) handleDrawerSectionSelection(mOnResumeChangeToSection);
    }

    // TODO: remove this ugly hack after implementing camera flip correctly in recorder
    // Reason for this ugly hack - Recorder starts new instances of itself when flipping camera.
    // that was done to solve another silly problem with glview. Will need to fix camera preview
    // remove the recreation of recorder activities from within the recorder and then remove this hack
    // because onActivityResult will always be called properly when the recorder is dismissed.
    private void sillyOnResumeHackDetectingFinishedRemake() {
        if (RecorderActivity.hackFinishedRemakeOID != null && RecorderActivity.hackDismissReason == RecorderActivity.DISMISS_REASON_FINISHED_REMAKE) {
            String remakeOID = RecorderActivity.hackFinishedRemakeOID;
            Remake renderedRemake = Remake.findByOID(remakeOID);
            if (renderedRemake == null) {
                Log.e(TAG, "Critical error. Recorder sent remake to rendering, but not found in local storage.");
                return;
            }
            Log.d(TAG, String.format("Sent remake. Will show progress for remake %s", remakeOID));
            movieProgressFragment.showProgressForRemake(renderedRemake);
            mOnResumeChangeToSection = SECTION_STORIES;

            RecorderActivity.hackDismissReason = -1;
            RecorderActivity.hackFinishedRemakeOID = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeObservers();
        HMixPanel mp = HMixPanel.sh();
        if (mp != null) mp.flush();
    }

    @Override
    public void onNewIntent (Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode != REQUEST_CODE_RECORDER) return;


        String logString = String.format("Returned from recorder with result code: %d", resultCode);
        Log.d(TAG, logString);
        Crashlytics.log(logString);

        String remakeOID;
        int currentSceneID;

        switch (resultCode) {
            case RecorderActivity.DISMISS_REASON_FINISHED_REMAKE:
                // User sent a remake to rendering. Show the create movie progress bar
                // To keep track of the rendering.
                remakeOID = resultData.getStringExtra("remakeOID");
                if (remakeOID == null) {
                    Log.e(TAG, "Critical error. Why no remake ID if recorder dismissed because user finished remake?");
                    return;
                }
                // Show progress
                Remake renderedRemake = Remake.findByOID(remakeOID);
                if (renderedRemake == null) {
                    Log.e(TAG, "Critical error. Recorder sent remake to rendering, but not found in local storage.");
                    break;
                }
                Log.d(TAG, String.format("Sent remake. Will show progress for remake %s", remakeOID));
                movieProgressFragment.showProgressForRemake(renderedRemake);
                mOnResumeChangeToSection = SECTION_STORIES;

                // TODO: remove this hack
                RecorderActivity.hackFinishedRemakeOID = null;
                RecorderActivity.hackDismissReason = -1;

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
        mPositionClicked = position;
        mNavigationItemClicked = true;
    }

    public void onDrawerClosed() {
        if (mNavigationItemClicked) {
            mNavigationItemClicked = false;
            handleDrawerSectionSelection(mPositionClicked);
        }
    }

    private void navigateToSectionIfRequested() {
        HomageApplication app = HomageApplication.getInstance();
        final String mainStartsWith = app.getStartingNavigationOn();
        defaultSelection = SECTION_STORIES;
        if (mainStartsWith != null && mainStartsWith.equals("MyStories")) {
            mOnResumeChangeToSection = SECTION_ME;
            defaultSelection = SECTION_ME;
        }
        app.clearStartingNavigationOn();
    }

    //endregion

    public void handleDrawerSectionSelection(int position) {
        // update the main content by replacing fragments
        if (mOnResumeChangeToSection > -1) mOnResumeChangeToSection = -1;

        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case SECTION_LOGIN:
                Crashlytics.log("handleDrawerSectionSelection --> Login");

                User user = User.getCurrent();
                if (user.isGuest()) {
                    // User is guest. Will show the join login screen.
                    showLogin();
                } else {
                    // User is logged in. Will logout user and go back to the first login screen.
                    String sessionID = HomageApplication.getInstance().currentSessionID;
                    String userID = user.getOID().toString();
                    if (sessionID != null) HomageServer.sh().reportSessionEnd(sessionID,userID);
                    HomageApplication.getInstance().currentSessionID = null;
                    logout();
                }
                break;

            case SECTION_STORIES:
                Crashlytics.log("handleDrawerSectionSelection --> Stories");

                currentSection = position;
                showStories();
                break;

            case SECTION_ME:
                Crashlytics.log("handleDrawerSectionSelection --> My Stories");

                currentSection = position;
                showMyStories();
                break;

            case SECTION_SETTINGS:
                Crashlytics.log("handleDrawerSectionSelection --> Settings");

                showSettings();
                break;

            case SECTION_HOWTO:
                Crashlytics.log("handleDrawerSectionSelection --> Howto");

                showHowTo();
                break;

            default:
                Crashlytics.log("handleDrawerSectionSelection --> Unimplemented!");

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

    // get Like refresh
    private BroadcastReceiver onRemakeLiked = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Crashlytics.log("onRemakeLiked");
            hideRefreshProgress();
            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);
            if(success) {
                HashMap<String, Object> responseInfo = (HashMap<String, Object>) intent.getSerializableExtra(Server.SR_RESPONSE_INFO);

                if (responseInfo != null) {
                    String remakeOID = (String) responseInfo.get("remakeOID");
                    String statuscode = responseInfo.get("status_code").toString();

                    if (success) {
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        Fragment f = fragmentManager.findFragmentByTag(FRAGMENT_TAG_REMAKE_VIDEO);
                        if (f!=null) {
                            ((RemakeVideoFragment)f).refresh(remakeOID);
                        }
                        //showStories();
                    }
                }
            }
        }
    };

    private BroadcastReceiver onStoriesUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Crashlytics.log("onStoriesUpdated");

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
            Crashlytics.log("onRemakeCreation");

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
                MainActivity.this.startActivityForResult(myIntent, MainActivity.REQUEST_CODE_RECORDER);
            }
        }
    };

    // Observers handlers
    private BroadcastReceiver onRemakesForStoryUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Crashlytics.log("onRemakesForStoryUpdated");

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
            Crashlytics.log("onRemakesForUserUpdated");

            refreshMyStoriesIfCurrentSection();
        }
    };

    private BroadcastReceiver onRemakeDeletion = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Crashlytics.log("onRemakeDeletion");

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
            Crashlytics.log("onUserLogin");

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

        /*
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        View container = findViewById(R.id.bigContainer);
        android.support.v4.widget.DrawerLayout.LayoutParams params2 = (android.support.v4.widget.DrawerLayout.LayoutParams)container.getLayoutParams();
        //int m = (int)(45.0f*metrics.density);
        int m = 0;
        params2.setMargins(0, m, 0, 0);
        container.setLayoutParams(params2);
        */
    }

    public void updateLoginState() {
        Crashlytics.log("updateLoginState");

        User user = User.getCurrent();
        if (user==null) {
            Log.d(TAG, "No logged in user.");
            Crashlytics.log("updateLoginState: No user");
        } else {
            String userString = String.format("Current user:%s", user.getTag());
            Log.d(TAG, userString);
            Crashlytics.log(String.format("updateLoginState: %s %s", userString, user.getOID()));
            Crashlytics.setString("userID", user.getOID());
        }
        mNavigationDrawerFragment.refresh();
    }

    public void updateRenderProgressState() {
        //deprecated
        //Log.d(TAG, "Need to update movie creation progress state");
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

//    public void showRemakeVideo(Bundle args, Remake remake) {
//        showActionBar();
//        currentSection = SECTION_REMAKE_VIDEO;
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        remakeVideoFragment = RemakeVideoFragment.newInstance(args, remake);
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        transaction.setCustomAnimations(R.anim.open_from_center, R.anim.close_from_center,R.anim.open_from_center, R.anim.close_from_center);
//        transaction.replace(R.id.remakecontainer, remakeVideoFragment, FRAGMENT_TAG_REMAKE_VIDEO);
//        transaction.addToBackStack(null);
//        transaction.commit();
//    }

    public void showStoryDetails(Story story) {
        showActionBar();

        currentSection = SECTION_STORY_DETAILS;
        currentStory = story;
        FragmentManager fragmentManager = getSupportFragmentManager();
        storyDetailsFragment = StoryDetailsFragment.newInstance(SECTION_STORY_DETAILS, story);
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.animation_slide_in, R.anim.animation_slide_out,R.anim.animation_slide_in, R.anim.animation_slide_out)
                .replace(R.id.container, storyDetailsFragment,FRAGMENT_TAG_STORY_DETAILS)
                .addToBackStack(null)
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
        HMixPanel.sh().track("appMoveToStoriesTab",null);
    }

    public void showMyStories() {
        showActionBar();

        User user = User.getCurrent();
        if (user==null) return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        currentSection = SECTION_ME;
        currentStory = null;
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.animation_fadein, R.anim.animation_fadeout,R.anim.animation_fadein, R.anim.animation_fadeout)
                .replace(R.id.container, MyStoriesFragment.newInstance(currentSection, user), FRAGMENT_TAG_ME)
                .addToBackStack(null)
                .commitAllowingStateLoss();
        HMixPanel.sh().track("appMoveToMeTab",null);
    }

    public void showSettings() {
        Intent myIntent = new Intent(this, SettingsActivity.class);
        HMixPanel.sh().track("appMoveToSettingsTab",null);
        startActivity(myIntent);

    }

    public void showHowTo() {
        Intent myIntent = new Intent(this, FullScreenVideoPlayerActivity.class);
        Uri videoURL = Uri.parse("android.resource://com.homage.app/raw/howto_video");
        myIntent.putExtra(VideoPlayerFragment.K_FILE_URL, videoURL.toString());
        myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
        myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, true);

        myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_ID, "");
        myIntent.putExtra(HEvents.HK_VIDEO_ENTITY_TYPE, HEvents.H_INTRO_MOVIE);
        myIntent.putExtra(HEvents.HK_VIDEO_ORIGINATING_SCREEN, HomageApplication.HM_HOW_TO_TAB);

        HMixPanel.sh().track("appWillPlayIntroMovie",null);

        startActivity(myIntent);
    }
    //endregion

    public void refetchTopRemakesForStory(Story story,String userID) {
        HomageServer.sh().refetchRemakesForStory(story.getOID(), null, 16, null,userID); // Implement partial fetch
        showRefreshProgress();
    }

    public void refetchAllRemakesForStory(Story story) {
        HomageServer.sh().refetchRemakesForStory(story.getOID(), null, null, null,null); // Implement partial fetch
        showRefreshProgress();
    }

    public void refetchMoreRemakesForStory(Story story, Integer limit, Integer skip, String userID )
    {                                                          //user limit skip
        HomageServer.sh().refetchRemakesForStory(story.getOID(), null, limit, skip,userID ); // Implement partial fetch
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
                    HashMap props = new HashMap<String,String>();
                    props.put("story" , remake.getStory().name);
                    props.put("remake_id" , remake.getOID());
                    HMixPanel.sh().track("MEDeleteRemake",props);
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

        HashMap props = new HashMap<String,String>();
        props.put("story" , theStory.name);

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
                                HMixPanel.sh().track("doOldRemake",null);
                                Intent myIntent = new Intent(MainActivity.this, RecorderActivity.class);
                                Bundle b = new Bundle();
                                b.putString("remakeOID", remakeOID);
                                myIntent.putExtras(b);
                                MainActivity.this.startActivityForResult(myIntent, REQUEST_CODE_RECORDER);
                                break;
                            case 1:
                                user.deleteAllUnfinishedRemakesForStory(theStory);
                                HMixPanel.sh().track("doNewRemakeOld",null);
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
                    refetchAllRemakesForStory(MainActivity.this.currentStory);
                    break;

                case SECTION_ME:
                    refetchRemakesForCurrentUser();
                    break;
            }
        }
    };

    public void onBackPressed() {
        Log.d(TAG, "Pressed back button");

        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            //additional code
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }


    //region *** Share ***
    private void initSharing() {
        shareMimeTypes.put("com.google.android.gm",      "message/rfc822");
        shareMimeTypes.put("com.google.android.email",   "message/rfc822");
        shareMimeTypes.put("com.facebook.katana",        "text/plain");
        shareMimeTypes.put("com.whatsapp",               "text/plain");
        shareMimeTypes.put("com.twitter.android",        "text/plain");
        shareMimeTypes.put("com.google.android.talk",    "text/plain");

        shareMethods.put("com.google.android.gm",      SHARE_METHOD_EMAIL);
        shareMethods.put("com.google.android.email",   SHARE_METHOD_EMAIL);
        shareMethods.put("com.facebook.katana",        SHARE_METHOD_FACEBOOK);
        shareMethods.put("com.whatsapp",               SHARE_METHOD_WHATS_APP);
        shareMethods.put("com.twitter.android",        SHARE_METHOD_TWITTER);
        shareMethods.put("com.google.android.talk",    SHARE_METHOD_MESSAGE);
    }

    private List<ResolveInfo> getSupportedActivitiesForSharing() {
        // Most important: GMail, Email, Whatsapp, Twitter, Facebook, others...
        PackageManager pm = getPackageManager();
        Intent i;

        List<ResolveInfo> activities = new ArrayList<ResolveInfo>();

        // EMAIL
        i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        final List<ResolveInfo> activitiesSupportingMail = getPackageManager().queryIntentActivities(i, 0);
        for (ResolveInfo info : activitiesSupportingMail) {
            String appName = info.loadLabel(pm).toString();
            String packageName = info.activityInfo.packageName;
            if (!shareMimeTypes.containsKey(packageName)) continue;
            String mimeType = shareMimeTypes.get(packageName);
            if (!mimeType.equals("message/rfc822")) continue;
            activities.add(info);
            Log.v(TAG, String.format("Mail share using >>>> %s %s", appName, info.activityInfo.packageName));
        }

        // PLAIN TEXT
        i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        final List<ResolveInfo> activitiesSupportingText = getPackageManager().queryIntentActivities(i, 0);
        for (ResolveInfo info : activitiesSupportingText) {
            String appName = info.loadLabel(pm).toString();
            String packageName = info.activityInfo.packageName;
            if (!shareMimeTypes.containsKey(packageName)) continue;
            String mimeType = shareMimeTypes.get(packageName);
            if (!mimeType.equals("text/plain")) continue;
            activities.add(info);
            Log.v(TAG, String.format("Share using >>>> %s %s", appName, info.activityInfo.packageName));
        }

        return activities;
    }

    public void shareRemake(final Remake sharedRemake) {
        PackageManager pm = getPackageManager();

        final Story story = sharedRemake.getStory();
        final String downloadLink = "https://itunes.apple.com/us/app/homage/id851746600?l=iw&ls=1&mt=8";


        final List<ResolveInfo> activities = getSupportedActivitiesForSharing();
        List<String> appNames = new ArrayList<String>();
        List<Drawable> appIcons = new ArrayList<Drawable>();
        for (ResolveInfo info : activities) {
            appNames.add(info.loadLabel(pm).toString());
            try {
                appIcons.add(pm.getApplicationIcon(info.activityInfo.packageName));
            } catch (Exception ex) {
                appIcons.add(null);
            }
        }

        ListAdapter adapter = new ArrayAdapterWithIcons(this, appNames, appIcons);
        new AlertDialog.Builder(this).setTitle("Share your story:")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item ) {
                        ResolveInfo info = activities.get(item);
                        String packageName = info.activityInfo.packageName;
                        String mimeType = shareMimeTypes.get(packageName);
                        Integer shareMethod = shareMethods.get(packageName);

                        final Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType(mimeType);

                        switch(shareMethod) {
                            case SHARE_METHOD_EMAIL:
                                i.putExtra(Intent.EXTRA_SUBJECT, story.shareMessage);
                                i.putExtra(Intent.EXTRA_TEXT,
                                        String.format(
                                                "%s \n\n keep calm and get Homage at: \n\n %s" ,
                                                sharedRemake.shareURL , downloadLink));
                                break;
                            default:
                                i.putExtra(Intent.EXTRA_TEXT,
                                        String.format(
                                                "%s: \n %s \n keep calm and get Homage at: \n %s" ,
                                                story.shareMessage , sharedRemake.shareURL , downloadLink));
                        }


                        // Analytics homage
                        HomageServer.sh().reportRemakeShareForUser(
                                sharedRemake.getOID(),sharedRemake.userID, shareMethod);

                        // Analytics mixpanel
                        HashMap props = new HashMap<String,String>();
                        props.put("story", story.name);
                        props.put("remake_id", sharedRemake.getOID());
                        props.put("share_method", packageName);
                        HMixPanel.sh().track("MEShareRemake",props);

                        // start the selected activity
                        i.setPackage(info.activityInfo.packageName);
                        startActivityForResult(i, 555);
                    }
                }).show();


        /*
        final Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");

        final List<ResolveInfo> activities = getPackageManager().queryIntentActivities(i, 0);

        List<String> appNames = new ArrayList<String>();
        for (ResolveInfo info : activities) {
            appNames.add(info.loadLabel(getPackageManager()).toString());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Complete Action using...");
        builder.setItems(appNames.toArray(new CharSequence[appNames.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ResolveInfo info = activities.get(item);
                int share_method = -1;

                if(info.activityInfo.packageName.equals("com.google.android.gm")) {
                    i.putExtra(Intent.EXTRA_SUBJECT, story.shareMessage);
                    i.putExtra(Intent.EXTRA_TEXT,
                            String.format(
                                    "%s \n keep calm and get Homage at: \n %s" ,
                                    sharedRemake.shareURL , downloadLink));
                    share_method = SHARE_METHOD_EMAIL;

                } else {
                    i.putExtra(Intent.EXTRA_TEXT,
                            String.format(
                                    "%s: \n %s \n keep calm and get Homage at: \n %s" ,
                                    story.shareMessage , sharedRemake.shareURL , downloadLink));
                }

                if(info.activityInfo.packageName.equals("com.whatsapp")) {
                    share_method = SHARE_METHOD_WHATS_APP;
                }

                if(info.activityInfo.packageName.equals("com.google.android.apps.docs")) {
                    share_method = SHARE_METHOD_COPY_URL;
                }

                if(info.activityInfo.packageName.equals("com.facebook.katana")) {
                    share_method = SHARE_METHOD_FACEBOOK;
                }

                if(info.activityInfo.packageName.equals("com.twitter.android")) {
                    share_method = SHARE_METHOD_TWITTER;
                }

                if(info.activityInfo.packageName.equals("com.android.mms")) {
                    share_method = SHARE_METHOD_MESSAGE;
                }

                HomageServer.sh().reportRemakeShareForUser(
                        sharedRemake.getOID(),sharedRemake.userID,share_method);

                HashMap props = new HashMap<String,String>();
                props.put("story", story.name);
                props.put("remake_id", sharedRemake.getOID());
                props.put("share_method", Integer.toString(share_method));
                HMixPanel.sh().track("MEShareRemake",props);

                // start the selected activity
                i.setPackage(info.activityInfo.packageName);
                startActivity(i);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

        return;
        */

                /*
        Drawable icon = getPackageManager().getApplicationIcon("com.example.testnotification");
imageView.setImageDrawable(icon);
         */

    }
    //endregion

    //region *** GCM ***
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(HomageApplication.GCM_SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // Persist the regID - no need to register again.
                    HomageApplication.storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.d(TAG, msg);
            }
        }.execute(null, null, null);
    }

    //endregion


}
