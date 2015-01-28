/**
    $$\   $$\  $$$$$$\  $$\      $$\  $$$$$$\   $$$$$$\  $$$$$$$$\
    $$ |  $$ |$$  __$$\ $$$\    $$$ |$$  __$$\ $$  __$$\ $$  _____|
    $$ |  $$ |$$ /  $$ |$$$$\  $$$$ |$$ /  $$ |$$ /  \__|$$ |
    $$$$$$$$ |$$ |  $$ |$$\$$\$$ $$ |$$$$$$$$ |$$ |$$$$\ $$$$$\
    $$  __$$ |$$ |  $$ |$$ \$$$  $$ |$$  __$$ |$$ |\_$$ |$$  __|
    $$ |  $$ |$$ |  $$ |$$ |\$  /$$ |$$ |  $$ |$$ |  $$ |$$ |
    $$ |  $$ | $$$$$$  |$$ | \_/ $$ |$$ |  $$ |\$$$$$$  |$$$$$$$$\
    \__|  \__| \______/ \__|     \__|\__|  \__| \______/ \________|
             ____  ____  ___  __  ____  ____  ____  ____
            (  _ \(  __)/ __)/  \(  _ \(    \(  __)(  _ \
             )   / ) _)( (__(  O ))   / ) D ( ) _)  )   /
            (__\_)(____)\___)\__/(__\_)(____/(____)(__\_)

                            Activity
*/
package com.homage.app.recorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.Log;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.grafika.AspectFrameLayout;
import com.androidquery.AQuery;
import com.crashlytics.android.Crashlytics;
import com.homage.FileHandler.ContourHandler;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.SettingsActivity;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.networking.uploader.UploadManager;
import com.homage.views.ActivityHelper;
import com.homage.views.Pacman;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;


/*
 * This the main activity class of the recorder of the homage app.
 * Yes, this activity class is somewhat big and contains a big portion of the functionality
 * of the app.
 *
 * Lets break it down:
 *
 *      The Activity of the recorder consists of:
 *          - Camera preview layer.
 *          - Silhouette layer.
 *          - A controls drawer that has two states: open and closed. When dragged open, it shows
 *          more info about the remake and allows the user to switch between scenes.
 *          When closed, a record button is available. When drawer is open, the record button
 *          changes to a "close drawer" button.
 *          - Video players showing ready made videos by homage: Our story & Our story.
 *          - The state machine: controls the flow of the remake. Guides the user with
 *          overlaying dlg messages and tells the user what she should/can do next.
 *          - Some simple overlaying buttons: close recorder, flip camera and show direction.
 *
 *
 *
 * @author  Homage
 * @author  Aviv Wolf
 * @since   0.1
 */
public class RecorderActivity extends Activity
        implements SurfaceTexture.OnFrameAvailableListener {
    private String TAG = "TAG_"+getClass().getName();

    LayoutInflater inflater;

    public final static String K_SAVED_RECORDER_MAKING_A_SCENE_STATE = "savedRecorderMakingASceneState";
    public final static String K_SAVED_CURRENT_SCENE_ID = "savedCurrentSceneId";

    final public static int RECORDER_CLOSED = 666;

    public static String hackFinishedRemakeOID = null;
    public static int hackDismissReason = -1;

    // Camera handler (handles messages from other threads)
    private CameraHandler mCameraHandler;
    private boolean mRecordingEnabled;

    private Intent starterIntent;
    private boolean shouldReleaseCameraOnNavigation = true;

    // Some references to UI elements
    private AQuery aq;
    private View controlsDrawer;
    private View recordButton;
    private View warningButton;
    private View recorderSceneScriptButton;
    private View recorderFullDetailsContainer;
    private View recorderShortDetailsContainer;
    private ListView scenesListView;
    private ViewPager videosPager;
    private View topBlackBar, bottomBlackBar;
    private RecorderVideosPagerAdapter videosAdapter;

    // Remake info
    protected Remake remake;
    protected Story story;
    protected List<Scene> scenes;
    protected List<Footage.ReadyState> footagesReadyStates;
    protected int currentSceneID;
    protected String outputFile;

    // Actions & State
    public boolean isRecording;
    public boolean itsPreviewTime = true;
    public boolean isBackgroundDetectionRunning;


    //Contour
    public String contourLocalUrl;

//    preferences;
    SharedPreferences prefs;
    SharedPreferences.Editor prefsEditor;

    //Constants
    private final static int ACTION_DO_NOTHING = 1;
    private final static int ACTION_HANDLE_STATE = 2;
    private final static int ACTION_ADVANCE_STATE = 3;
    private final static int ACTION_ADVANCE_AND_HANDLE_STATE = 4;
    private final static int ACTION_BY_RESULT_CODE = 5;
    public final static int DONT_SHOW_THIS_AGAIN = 6;

//    Recorder Info constants
    public final static String ERROR_DESCRIPTION = "errorDescription";
    public final static String REASON_CODE = "reasonCode";
    public final static String TARGET_DURATION = "tragetDuration";
    public final static String OUTPUT_DURATION = "outputDuration";
    public final static String DURATION_DIFF = "durationDiff";
    public final static String FILE_SIZE = "fileSize";


    private RecorderStateMachine stateMachine;
    private Handler counterDown;
    protected int countDown;
    protected boolean canceledCountDown;


//    Warning section
    protected final static int countDownFrom = 3;
    protected final static int warningCountDownFrom = 5;
    private int warningCountDown = warningCountDownFrom;
    public int lastcc = 0;

    // The possible states of the recorder, handled by the state machine.
    static private enum RecorderState {
        JUST_STARTED,
        TUTORIAL,
        SCENE_MESSAGE,
        MAKING_A_SCENE,
        FINISHED_A_SCENE_MESSAGE,
        EDITING_TEXTS,
        FINISHED_ALL_SCENES_MESSAGE,
        USER_REQUEST_TO_CHECK_WHAT_NEXT,
        HELP_SCREENS
    }

    private boolean shouldShowScriptBar = false;

    // Reasons for the dismissal of the recorder
    public final static int DISMISS_REASON_USER_ABORTED_PRESSING_X = 500;
    public final static int DISMISS_REASON_FINISHED_REMAKE = 600;

    private AspectFrameLayout previewContainer;

    // Layouts and views
    private Animation fadeInAnimation, fadeOutAnimation;
    public int viewHeightForClosingControlsDrawer;
    private boolean viewsInitialized;
    private int currentVideoPage = -1;
    private ImageView videosPageIndicator1, videosPageIndicator2;

    // Media
    MediaPlayer mp;

    //region *** Activity lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraManager.sh().SetContext(this);
        viewsInitialized = false;
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        starterIntent = getIntent();
        prefs = HomageApplication.getSettings(this);
        prefsEditor = prefs.edit();
        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);

        // Is recording enabled?
        mRecordingEnabled = false;

        //
        // Get info about the remake
        //
        Bundle b = getIntent().getExtras();
        String remakeOID = b.getString("remakeOID");
        remake = Remake.findByOID(remakeOID);
        story = remake.getStory();
        isRecording = false;
//        dontShowAgain = false;
        isBackgroundDetectionRunning = false;
        ContourHandler ch = new ContourHandler();
        ch.DownloadContour(this, remake, "/homage/contours/");

        // Crashlytics logging
        Crashlytics.setString("remakeID", remakeOID);
        Crashlytics.setString("storyID", story.getOID());
        Crashlytics.setString("storyName", story.name);
        Crashlytics.log("RecorderActivity onCreate");

        //region *** Layout initializations ***
        Log.d(TAG, String.format("Started recorder for remake: %s", remake.getOID()));
        HashMap props = new HashMap<String,String>();
        props.put("story" , remake.getStory().name);
        props.put("remake_id" , remake.getOID());
        HMixPanel.sh().track("REEnterRecorder",props);

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Set the content layout, load resources and store some references to views
        setContentView(R.layout.activity_recorder);

        // AQuery & Some references (just for shorter syntax and faster access later)
        aq = new AQuery(this);
        controlsDrawer = aq.id(R.id.recorderControlsDrawer).getView();
        recordButton = aq.id(R.id.recorderRecordButton).getView();
        warningButton = aq.id(R.id.warningRecordButton).getView();
        recorderSceneScriptButton = aq.id(R.id.recorderSceneScriptButton).getView();
        recorderFullDetailsContainer = aq.id(R.id.recorderFullDetailsContainer).getView();
        recorderShortDetailsContainer = aq.id(R.id.recorderShortDetailsContainer).getView();
        scenesListView = aq.id(R.id.scenesListView).getListView();
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.animation_fadein);
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.animation_fadeout);
        videosPager = (ViewPager)aq.id(R.id.videosPager).getView();
        videosPageIndicator1 = aq.id(R.id.videosPageIndicator1).getImageView();
        videosPageIndicator2 = aq.id(R.id.videosPageIndicator2).getImageView();
        topBlackBar = aq.id(R.id.blackBarTop).getView();
        bottomBlackBar = aq.id(R.id.blackBarBottom).getView();
        previewContainer = (AspectFrameLayout)findViewById(R.id.cameraPreview_afl);

        closeControlsDrawer(false);
        updateScriptBar();
        scenesListView.setVisibility(View.GONE);

        updateVideosPageIndicator(0);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/

        // Pushed THE button! (Clicked on the record button)
        aq.id(R.id.recorderRecordButton).clicked(onClickedRecordButton);
        // Pushed THE button! (Clicked on the record button)
        aq.id(R.id.warningRecordButton).clicked(onClickedWarningButton);

        // Dragging the drawer up and down.
        aq.id(R.id.recorderControlsDrawer).getView().setOnTouchListener(new OnDraggingControlsDrawerListener(this));

        aq.id(R.id.recorderShortDetailsSpacerContainerButton).clicked(onClickedOpenDrawerButton);
        // Clicked on the close controls drawer button.
        aq.id(R.id.recorderCloseDrawerButton).clicked(onClickedCloseDrawerButton);

        // Dismissing the recorder.
        aq.id(R.id.recorderOverlayDismissButton).clicked(onClickedRecorderDismissButton);

        // Show scene description info.
        aq.id(R.id.recorderOverlaySceneDescriptionButton).clicked(onClickedSceneDescriptionButton);
        aq.id(R.id.recorderSceneDirectionButton).clicked(onClickedSceneDescriptionButton);

        // Toggle showing script
        aq.id(R.id.recorderSceneScriptButton).clicked(onClickedToggleScriptButton);

        // Clicked the dismiss button on the "Welcome screen" overlay.
        aq.id(R.id.welcomeScreenDismissButton).clicked(onClickedWelcomeScreenDismissButton);

        // Clicked the change cameras button
        aq.id(R.id.recorderOverlayFlipCameraButton).clicked(onClickedFlipCameraButton);

        // Clicked the create movie button (the button above the scenes list)
        aq.id(R.id.createMovieButton).clicked(onClickedCreateMovieButton);

        // Deprecated - now handled with buttons inside each row of the list (see adapter)
        // Clicked on a scene number in the list of scenes.
        // User tries to select a scene in the list.
        // aq.id(R.id.scenesListView).itemClicked(onClickedSceneItem);
        HideWarningButton();
        //endregion
    }

    @Override
    public void onBackPressed() {
        askUserIfWantToCloseRecorder();
    }

    @Override
    protected void onPause(){
        super.onPause();

        showCurtainsAnimated(false);

        final CameraManager cm = CameraManager.sh();

        // Cancel recording if currently recording.
        if (cm.isRecording()) {
            cm.cancelRecording();
        }

        showCurtainsAnimated(false);
        cm.pauseCameraPreviewIfInitialized();
        cancelCountingDownToRecording();
        Log.d(TAG, "onPause complete");
    }

    @Override
    protected void onResume() {
        super.onResume();
        closeControlsDrawer(true);
        showCurtainsAnimated(false);

        shouldReleaseCameraOnNavigation = true;

        int orientation = getResources().getConfiguration().orientation;
        Log.d(TAG, String.format("Current orientation: %d", orientation));

        final CameraManager cm = CameraManager.sh();
        cm.resumeCameraPreviewIfInitialized();
        Log.d(TAG, "onPause complete");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (shouldReleaseCameraOnNavigation) {
            CameraManager.sh().releaseCamera();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shouldReleaseCameraOnNavigation) {
            CameraManager cm = CameraManager.sh();
            cm.releaseCamera();
            cm.releaseRecordingManagers();
        }
    }
    //endregion

    private void reloadPreview() {
        final CameraManager cm = CameraManager.sh();

        // Set the preview aspect ratio.
        final AspectFrameLayout layout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);

        // Aspect ratio should always be set to 16/9
        double videoAspectRatio = (double)cm.mCameraPreviewWidth / (double)cm.mCameraPreviewHeight;
        double onScreenAspectRatio = (double)16/(double)9;

        layout.setOriginalAspectRatio(videoAspectRatio);
        layout.setAspectRatio(onScreenAspectRatio);
        layout.setCroppingBarsViews(topBlackBar, bottomBlackBar);
        layout.requestLayout();

        showCurtainsAnimated(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideCurtainsAnimated(false);
            }
        }, 200);

    }

    //region *** Views Initializations ***
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!viewsInitialized) initViews();
        if (scenesListView.getAdapter()==null) {
            initScenesAdapter();
        } else {
            updateScenesList();
        }

        //region *** State initialization ***
        if (stateMachine == null) {
            stateMachine = new RecorderStateMachine();

            Bundle b = starterIntent.getExtras();
            boolean isRecorderMakingAScene = b.getBoolean(K_SAVED_RECORDER_MAKING_A_SCENE_STATE, false);
            if (isRecorderMakingAScene) {
                int sceneId = b.getInt(K_SAVED_CURRENT_SCENE_ID);
                stateMachine.setState(RecorderState.MAKING_A_SCENE);
                currentSceneID = sceneId;
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        stateMachine.handleCurrentState();
                    } catch (RecorderException ex) {
                        Log.e(TAG, "Recorder state machine error.", ex);
                    }
                }
            }, 100);
        }
        //endregion

        if (!hasFocus) {
            hideCurtainsAnimated(false);
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                reloadPreview();
            }
        }, 0);

    }

    private void initViews() {
        Scene scene = story.findScene(currentSceneID);

        // initialize only once.
        if (viewsInitialized) return;

        viewsInitialized = true;

        // When closing the drawer, we will move it down according to its height.
        viewHeightForClosingControlsDrawer = controlsDrawer.getHeight();

        // But we will still want to see the record buttons and the short details
        // panel at the bottom of the screen, so we will take them into account.
        viewHeightForClosingControlsDrawer -= aq.id(R.id.recorderShortDetailsContainer).getView().getHeight();
        viewHeightForClosingControlsDrawer -= aq.id(R.id.recorderRecordButton).getView().getHeight()/2;
        recorderFullDetailsContainer.setAlpha(0);
        closeControlsDrawer(false);

        // Videos paging adapter
        videosAdapter = new RecorderVideosPagerAdapter(this, this.remake);
        ViewPager videosPager = (ViewPager)aq.id(R.id.videosPager).getView();
        videosPager.setAdapter(videosAdapter);
        videosPager.setOnPageChangeListener(onVideosPagerChangeListener);
    }
    //endregion

    //region *** Camera ***
    private void initCameraPreview() {
        // Configure the GLSurfaceView.
        // This will start the Renderer thread, with an
        // appropriate EGL context.
        CameraManager cm = CameraManager.sh();
        cm.openCamera();
        cm.startCameraPreviewInView(this, previewContainer);

        // After initialized, fade in the camera by fading out the "curtains" slowly
        hideCurtainsAnimated(true);
        reloadPreview();
    }
    //endregion

    private void showCurtainsAnimated(boolean animated) {
        if (animated) {
            Animation showCurtainsAnim = AnimationUtils.loadAnimation(RecorderActivity.this, R.anim.animation_fadein);
            showCurtainsAnim.setDuration(300);
            aq.id(R.id.recorderCurtains).animate(showCurtainsAnim);
            return;
        }
        aq.id(R.id.recorderCurtains).getView().setAlpha(1);
    }

    private void hideCurtainsAnimated(boolean animated) {
        if (animated) {
            Animation hideCurtainsAnim = AnimationUtils.loadAnimation(RecorderActivity.this, R.anim.animation_fadeout);
            hideCurtainsAnim.setDuration(1000);
            aq.id(R.id.recorderCurtains).animate(hideCurtainsAnim);
            return;
        }
        aq.id(R.id.recorderCurtains).getView().setAlpha(0);
    }

    //region *** The scenes list ***
    private void reloadData() {
        scenes = story.getScenesReversedOrder();
        footagesReadyStates = remake.footagesReadyStatesReversedOrder();
    }

    private void initScenesAdapter() {
        ListView listView = aq.id(R.id.scenesListView).getListView();
        reloadData();
        listView.setAdapter(scenesAdapter);
    }

    private void updateScenesList() {
        reloadData();
        scenesAdapter.notifyDataSetChanged();
    }

    private BaseAdapter scenesAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return scenes.size();
        }

        @Override
        public Object getItem(int i) {
            return scenes.get(i);
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
            if (rowView == null) rowView = inflater.inflate(R.layout.list_row_scene, scenesListView, false);

            final int index = i;
            Scene scene = (Scene)getItem(i);
            Footage.ReadyState readyState = footagesReadyStates.get(i);

            AQuery raq = new AQuery(rowView);
            raq.id(R.id.sceneNumberInList).text(scene.getTitle());
            raq.id(R.id.sceneIndicatorInList).visibility(
                    scene.getSceneID() == currentSceneID ? View.VISIBLE : View.INVISIBLE);
            raq.id(R.id.sceneTimeInList).text(scene.getTimeString());

            // Configure the view look, according to the footage ready state.
            if (readyState == Footage.ReadyState.READY_FOR_FIRST_RETAKE) {
                //
                // Ready for first retake
                raq.id(R.id.sceneIconLockedInList).visibility(View.GONE);
                raq.id(R.id.sceneTimeInList).visibility(View.VISIBLE);
                raq.id(R.id.sceneRetakeButton).visibility(View.GONE);
                raq.id(R.id.sceneNumberInList).textColorId(R.color.homage_text);
            } else if (readyState == Footage.ReadyState.READY_FOR_SECOND_RETAKE) {
                //
                // Ready for second retake.
                raq.id(R.id.sceneIconLockedInList).visibility(View.GONE);
                raq.id(R.id.sceneTimeInList).visibility(View.GONE);
                raq.id(R.id.sceneRetakeButton).visibility(View.VISIBLE);
                raq.id(R.id.sceneNumberInList).textColorId(R.color.homage_impact);
            } else {
                //
                // Locked.
                raq.id(R.id.sceneIconLockedInList).visibility(View.VISIBLE);
                raq.id(R.id.sceneTimeInList).visibility(View.GONE);
                raq.id(R.id.sceneRetakeButton).visibility(View.GONE);
                raq.id(R.id.sceneNumberInList).textColorId(R.color.homage_disabled);
            }

            raq.id(R.id.sceneNumberInList).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickedSceneAtItem(index);
                }
            });

            raq.id(R.id.sceneRetakeButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickedRetakeAtItem(index);
                }
            });


            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    };
    //endregion


    //region *** Controls Drawer **
    /**
     The controls drawer
     Can be dragged open or closed.

     **********************************
     *                                *
     *               ==               *
     *==============|  |==============*
     *    Scene 1    ==   ▢▢▢▢▢▢▢▢▢▢  *
     *    Scene 2    ||   ▢▢▢▢▢▢▢▢▢▢  *
     *    Scene 3    ||****************
     *    Scene 4    ||  dir | script *
     **********************************

     Allows to start recording when closed.
     Shows more info about the story/scene when opened.
     */

    /**
     *
     * @return
     */
    public float getHeightForClosingDrawer() {
        return viewHeightForClosingControlsDrawer;
    }

    public float getControlsDrawerPosition() {
        return controlsDrawer.getTranslationY();
    }

    public void setControlsDrawerPosition(float newPosition) {
        //float part = newPosition / viewHeightForClosingControlsDrawer;
        //part = part < 0 ? 0 : part;
        //part = part > 1 ? 1 : part;
        controlsDrawer.setTranslationY(newPosition);
    }

    private boolean isControlsDrawerOpen() {
        return controlsDrawer.getTranslationY() == 0;
    }

    public void hideControlsDrawer(boolean animated) {
        // Problem on
        if (animated) {
            // TODO: implement
        } else {
            controlsDrawer.setVisibility(View.GONE);
            //controlsDrawer.setAlpha(0);

            //controlsDrawer.setScaleX(0.5f);
        }
    }

    public void showControlsDrawer(boolean animated) {
        controlsDrawer.setTranslationY(0);
        if (animated) {
            // TODO: implement
        } else {
            //controlsDrawer.setAlpha(1);
            controlsDrawer.setVisibility(View.VISIBLE);
            controlsDrawer.setAlpha(1);
            closeControlsDrawer(false);

            //controlsDrawer.setScaleX(1.0f);
        }
    }

    public void closeControlsDrawer(boolean animated) {
        if (videosAdapter != null) videosAdapter.doneIfPlaying();

        if (animated) {
            float deltaToClosed = controlsDrawer.getTranslationY() - viewHeightForClosingControlsDrawer;
            TranslateAnimation anim = new TranslateAnimation(0,0,0,-deltaToClosed);
            anim.setDuration(250);
            anim.setFillAfter(true);
            anim.setFillEnabled(true);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Silly hack for solving the ugly flicker effect on animation end.
                    animation = new TranslateAnimation(0.0f, 0.0f, 0.0f, 0.0f);
                    animation.setDuration(1);
                    controlsDrawer.startAnimation(animation);
                    controlsDrawer.setTranslationY(viewHeightForClosingControlsDrawer);
                    controlsDrawerClosed();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            controlsDrawer.startAnimation(anim);
        } else {
            controlsDrawer.setTranslationY(viewHeightForClosingControlsDrawer);
            controlsDrawerClosed();
        }
        itsPreviewTime = true;
    }

    public void openControlsDrawer(boolean animated) {
        if (animated) {
            float deltaToZero = controlsDrawer.getTranslationY();
            TranslateAnimation anim = new TranslateAnimation(0,0,0,-deltaToZero);
            anim.setDuration(250);
            anim.setFillAfter(true);
            anim.setFillEnabled(true);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Silly hack for solving the ugly flicker effect on animation end.
                    animation = new TranslateAnimation(0.0f, 0.0f, 0.0f, 0.0f);
                    animation.setDuration(1);
                    controlsDrawer.startAnimation(animation);
                    controlsDrawer.setTranslationY(0);
                    controlsDrawerOpened();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            controlsDrawer.startAnimation(anim);
        } else {
            controlsDrawer.setTranslationY(0);
            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            HMixPanel.sh().track("REexpandMenu",props);
            controlsDrawerOpened();
        }
        itsPreviewTime = false;
    }


    private void controlsDrawerClosed() {
        if (recordButton.isClickable()) return;
        recordButton.startAnimation(fadeInAnimation);
        recordButton.setVisibility(View.VISIBLE);
        recordButton.setClickable(true);
        recorderFullDetailsContainer.startAnimation(fadeOutAnimation);
        recorderShortDetailsContainer.startAnimation(fadeInAnimation);
        recorderFullDetailsContainer.setVisibility(View.GONE);
        scenesListView.setVisibility(View.GONE);
        videosPager.setVisibility(View.GONE);
        videosAdapter.hideSurfaces();
        showOverlayButtons(false);


//        final CameraManager cm = CameraManager.sh();
        videosAdapter.done();
        updateScriptBar();
        aq.id(R.id.createMovieButton).visibility(View.GONE);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideCurtainsAnimated(false);
                CameraManager cm = CameraManager.sh();
                cm.resumeCameraPreviewIfInitialized();
            }
        }, 200);
//        HideWarningButton();
        itsPreviewTime = true;
    }

    private void controlsDrawerOpened() {
        if (!recordButton.isClickable()) return;
        HideWarningButton();
        cancelCountingDownToRecording();
        recordButton.startAnimation(fadeOutAnimation);
        recordButton.setVisibility(View.GONE);
        recordButton.setClickable(false);
        recorderFullDetailsContainer.setAlpha(1);
        recorderFullDetailsContainer.startAnimation(fadeInAnimation);
        recorderShortDetailsContainer.startAnimation(fadeOutAnimation);
        recorderFullDetailsContainer.setVisibility(View.VISIBLE);
        scenesListView.setVisibility(View.VISIBLE);
        videosPager.setVisibility(View.VISIBLE);
        videosAdapter.showSurfaces();
        hideOverlayButtons(false);

        showCurtainsAnimated(false);
        updateScriptBar();

        CameraManager cm = CameraManager.sh();
        cm.pauseCameraPreviewIfInitialized();

        // Check if need to show the create movie button or not
        // Will be shown only if already taken all footages (at least once)
        if (remake.allScenesTaken()) {
            aq.id(R.id.createMovieButton).visibility(View.VISIBLE);
        } else {
            aq.id(R.id.createMovieButton).visibility(View.GONE);
        }
//        HideWarningButton();
        itsPreviewTime = false;
    }


    //endregion

    //region *** Recorder's state machine. ***
    /**
     *   ============================
     *   The recorder's state machine
     *   ============================
     */
    private class RecorderStateMachine {
        RecorderState currentState;
        boolean initializedCameraPreview;

        public RecorderStateMachine() {
            this.currentState = RecorderState.JUST_STARTED;
            this.initializedCameraPreview = false;
        }

        protected void setState(RecorderState state) {
            this.currentState = state;
        }

        private void handleCurrentState() throws RecorderException {
            switch (currentState) {
                case JUST_STARTED: justStarted(); break;
                case SCENE_MESSAGE: sceneMessage(); break;
                case MAKING_A_SCENE: makingAScene(); break;
                case FINISHED_A_SCENE_MESSAGE: finishedASceneMessage(); break;
                case FINISHED_ALL_SCENES_MESSAGE: finishedAllScenesMessage(); break;
                default:
                    Crashlytics.log(String.format("Unimplemented recorder state %s", currentState));
                    throw new RecorderException(String.format("Unimplemented recorder state %s", currentState));
            }
        }

        private void advanceState() throws RecorderException {
            switch (currentState) {
                case JUST_STARTED:
                    // User got the "just starting" message.
                    // Now we can get to business.
                    // NEXT, we will show a message about the next scene to shoot.
                    Crashlytics.log("Recorder advance state: JUST_STARTED --> SCENE_MESSAGE");
                    currentState = RecorderState.SCENE_MESSAGE;
                    break;
                case SCENE_MESSAGE:
                    // User dismissed the scene message.
                    // Enter the "making a scene" state.
                    // (gives the user control of the recorder)
                    Crashlytics.log("Recorder advance state: SCENE_MESSAGE --> MAKING_A_SCENE");
                    currentState = RecorderState.MAKING_A_SCENE;
                    showControlsDrawer(false);
                    break;
                case MAKING_A_SCENE:
                    // User recorded footage for a scene in a remake.
                    // Advance state to next scene message or finished movie message
                    if (remake.allScenesTaken()) {
                        Log.d(TAG, "All scenes taken");
                        Crashlytics.log("Recorder advance state: MAKING_A_SCENE --> FINISHED_ALL_SCENES_MESSAGE");
                        currentState = RecorderState.FINISHED_ALL_SCENES_MESSAGE;
                    } else {
                        int nextSceneID = remake.nextReadyForFirstRetakeSceneID();
                        Log.d(TAG, String.format("Will advance to next scene: %d", nextSceneID));
                        Crashlytics.log("Recorder advance state: MAKING_A_SCENE --> FINISHED_A_SCENE_MESSAGE");
                        currentState = RecorderState.FINISHED_A_SCENE_MESSAGE;
                    }
                    break;
                case FINISHED_A_SCENE_MESSAGE:
                    // User chosen to continue to next scene, after a finished scene message.
                    Crashlytics.log("Recorder advance state: FINISHED_A_SCENE_MESSAGE --> MAKING_A_SCENE");
                    currentSceneID = remake.nextReadyForFirstRetakeSceneID();
                    Crashlytics.setInt("currentSceneID", currentSceneID);
                    currentState = RecorderState.MAKING_A_SCENE;
                    break;
                default:
                    throw new RecorderException(String.format("Unimplemented when advancing state %s", currentState));
            }
        }

        private void justStarted() throws RecorderException {
            Crashlytics.log("Recorder justStarted");

            User user = User.getCurrent();

            //
            // Select the first scene requiring a first retake.
            // If none found (all footages already taken by the user),
            // will select the last scene for this remake instead.
            //
            currentSceneID = remake.nextReadyForFirstRetakeSceneID();
            if (currentSceneID == Footage.NOT_FOUND) {
                currentSceneID = remake.lastSceneID();
                updateUIForSceneID(currentSceneID);
                setState(RecorderState.FINISHED_ALL_SCENES_MESSAGE);
                handleCurrentState();
                return;
            }

            // Just started. Show welcome message if user entered for the first time.
            // If not here for the first time, skip.
            if (user.isFirstUse) {
                aq.id(R.id.welcomeScreenOverlay).visibility(View.VISIBLE);
                user.isFirstUse = false;
                user.save();
            } else {
                advanceState();
                handleCurrentState();
            }
        }

        private void sceneMessage()  {
            Crashlytics.log("Recorder sceneMessage");

            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlaySceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID", remake.getOID());
            myIntent.putExtra("sceneID",currentSceneID);
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_ADVANCE_AND_HANDLE_STATE);
            overridePendingTransition(0, 0);
            if (currentSceneID>0) {
                updateUIForSceneID(currentSceneID);
            }
        }

        private void makingAScene() {
            Crashlytics.log("Recorder makingAScene");
            if (!initializedCameraPreview) {
                initCameraPreview();
                initializedCameraPreview = true;
            }
            showControlsDrawer(false);
            updateScenesList();
            updateUIForSceneID(currentSceneID);
        }

        private void finishedASceneMessage() {
            Crashlytics.log("Recorder finishedASceneMessage");

            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayFinishedSceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID", remake.getOID());
            myIntent.putExtra("sceneID",currentSceneID);
            int nextSceneID = remake.nextReadyForFirstRetakeSceneID();
            updateUIForSceneID(nextSceneID);
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_BY_RESULT_CODE);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }

        private void finishedAllScenesMessage() {
            Crashlytics.log("Recorder finishedAllScenesMessage");

            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayFinishedAllSceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID", remake.getOID());
            myIntent.putExtra("sceneID",currentSceneID);
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_BY_RESULT_CODE);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }
    }
    //endregion


    //region *** Recorder's actions ***
    /**
     *   ======================
     *   The recorder's actions
     *   ======================
     */


    private void recorderDoneWithReason(int dismissReason) {
        /**
         This is the end, beautiful friend
         This is the end, my only friend, the end
         Of our elaborate plans, the end
         Of everything that stands, the end
         No safety or surprise, the end
         I'll never look into your eyes, again
         */
        Intent result = new Intent();
        result.putExtra("remakeOID", remake.getOID());
        result.putExtra("currentSceneId", currentSceneID);
        setResult(dismissReason, result);

        // TODO: remove this hack after implementing camera flipping correctly.
        RecorderActivity.hackDismissReason = dismissReason;
        RecorderActivity.hackFinishedRemakeOID = remake.getOID();

        // Clean up camera preview staate
        CameraManager cm = CameraManager.sh();
        cm.cleanCameraPreview();

        // Done
        finish();
        overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout_with_zoom);
    }

    private boolean isCountingDownToRecording() {
        if (counterDown == null) return false;
        return true;
    }

    private void cancelCountingDownToRecording() {
        if (!isCountingDownToRecording()) return;

        HashMap props = new HashMap<String,String>();
        props.put("story" , remake.getStory().name);
        props.put("remake_id" , remake.getOID());
        props.put("scene_id", Integer.toString(currentSceneID));
        HMixPanel.sh().track("RECancelRecord",props);

        isRecording = false;

        canceledCountDown = true;
        counterDown = null;
        aq.id(R.id.recorderCountDownText).visibility(View.INVISIBLE);

        if (mp != null) {
            mp.pause();
            mp.reset();
        }

        showOverlayButtons(false);
        Pacman pacman = (Pacman)aq.id(R.id.pacman).getView();
        pacman.setVisibility(View.INVISIBLE);
        pacman.clearAnimation();

        Log.d(TAG, "Canceled counting down to recording.");
        Toast t = Toast.makeText(this, "Canceled recording.", Toast.LENGTH_SHORT);
        t.setGravity(Gravity.TOP, 0,0);
        t.show();
    }

    private void startCountingDownToRecording() {
        if (isCountingDownToRecording()) return;

        if(lastcc > 0){
            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id", Integer.toString(currentSceneID));
            HMixPanel.sh().track("REShootSceneWithGoodBackground",props);
        }
        else
        {
            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id", Integer.toString(currentSceneID));
            HMixPanel.sh().track("REShootSceneWithBadBackground",props);
        }

        String eventName = String.format("REHitRecordScene%d" , currentSceneID);
        HashMap props = new HashMap<String,String>();
        props.put("story" , remake.getStory().name);
        props.put("remake_id" , remake.getOID());
        props.put("scene_id", Integer.toString(currentSceneID));
        HMixPanel.sh().track(eventName,props);

        // Countdown
        hideOverlayButtons(false);
        canceledCountDown = false;
        countDown = countDownFrom;
        countDownUpdate();
        counterDown = new Handler();
        counterDown.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (canceledCountDown) return;
                countDown--;
                if (countDown < 1) {
                    Log.d(TAG, "Finished countdown. Will start recording!");
                    counterDown = null;
                    Pacman pacman = (Pacman)aq.id(R.id.pacman).getView();
                    pacman.setVisibility(View.INVISIBLE);
                    aq.id(R.id.recorderCountDownText).visibility(View.INVISIBLE);
                    startRecording();
                } else {
                    countDownUpdate();
                    counterDown.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void countDownUpdate() {
        Log.d(TAG, String.format("Count down to recording: %d", countDown));
        aq.id(R.id.recorderCountDownText).visibility(View.VISIBLE);
        aq.id(R.id.recorderCountDownText).text(String.format("%d", countDown));
        Pacman pacman = (Pacman)aq.id(R.id.pacman).getView();

        switch (countDown) {
            case 3:
                pacman.setVisibility(View.VISIBLE);
                pacman.mCurrAngle = 360;
                pacman.invalidate();
                break;

            case 2:
                mp = MediaPlayer.create(getApplicationContext() , R.raw.cinema_countdown);
                mp.start();
                pacman.startOneSecondAnimation();
                break;

            default:
                pacman.startOneSecondAnimation();
        }
    }

    public class ProgressBarAnimation extends Animation{
        private ProgressBar progressBar;
        private float from;
        private float  to;

        public ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
            super();
            this.progressBar = progressBar;
            this.from = from;
            this.to = to;
            this.setInterpolator(new LinearInterpolator());
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int) value);
        }

    }

    private void startRecording() {
        if (isRecording) return;

        final Scene scene = story.findScene(currentSceneID);
        isRecording = true;

        // Analytics
        HashMap props = new HashMap<String,String>();
        props.put("story" , remake.getStory().name);
        props.put("remake_id" , remake.getOID());
        props.put("scene_id", Integer.toString(currentSceneID));
        HMixPanel.sh().track("REStartRecording",props);

        if(!contourLocalUrl.isEmpty()) {
            HashMap mattingprops = new HashMap<String, String>();
            props.put("contourLocalUrl", contourLocalUrl);
            props.put("cc", Integer.toString(lastcc));
            HMixPanel.sh().track("MattingResult", mattingprops);
        }

        // Update the script bar.
        updateScriptBar();

        // Update UI
        hideControlsDrawer(false);
        hideOverlayButtons(false);
        hideSilhouette(false);

        // Progress bar animation
        ProgressBar progressBar = aq.id(R.id.recordingProgressBar).getProgressBar();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setAlpha(0.7f);
        ProgressBarAnimation anim = new ProgressBarAnimation(progressBar, 0, 100);
        anim.setDuration(scene.duration+300);
        progressBar.startAnimation(anim);


        // Let the camera manager handle the recording in the background.
        CameraManager cm = CameraManager.sh();
        cm.chooseRecordingMethod();
        cm.startRecording(scene.duration, new CameraManager.RecordingListener() {
            @Override
            public void recordingInfo(int what, File outputFile, HashMap<String, Object> info) {
                int toastMessage = -1;
                boolean shouldReturnFromRecordingState = false;
                HashMap props = new HashMap<String,String>();
                props.put("story" , remake.getStory().name);
                props.put("remake_id" , remake.getOID());
                props.put("scene_id", Integer.toString(currentSceneID));
                switch(what) {
                    case CameraManager.RECORDING_STARTED:
                        toastMessage = R.string.recording_started;
                        hideSilhouette(true);

                        break;

                    case CameraManager.RECORDING_FAILED:
                        showSilhouette(true);
                        toastMessage = R.string.recording_failed;
                        shouldReturnFromRecordingState = true;
                        // Analytics
//                        props.put(ERROR_DESCRIPTION, String.valueOf(info.get(ERROR_DESCRIPTION)));
                        HMixPanel.sh().track("RECaptureOutputError",props);
                        break;

                    case CameraManager.RECORDING_FAILED_TO_START:
                        showSilhouette(true);
                        toastMessage = R.string.recording_failed_to_start;
                        shouldReturnFromRecordingState = true;
                        // Analytics
//                        props.put(ERROR_DESCRIPTION, String.valueOf(info.get(ERROR_DESCRIPTION)));
                        HMixPanel.sh().track("RECaptureOutputError",props);
                        break;

                    case CameraManager.RECORDING_CANCELED:
                        showSilhouette(true);
                        toastMessage = R.string.recording_canceled;
                        shouldReturnFromRecordingState = true;
                        // Analytics
                        props.put(REASON_CODE, String.valueOf(info.get(REASON_CODE)));
                        HMixPanel.sh().track("RECaptureOutputCanceledWithReason",props);
                        break;

                    case CameraManager.RECORDING_FINISHED:
                        break;

                    case CameraManager.RECORDING_SUCCESS:
                        isRecording = false;
                        returnFromRecordingUI();
                        handleSuccessfulTake(outputFile.toString());
                        return;

                }

                // Show a message to the user if required.
                if (toastMessage != -1) {
                    Toast t = Toast.makeText(RecorderActivity.this, toastMessage, Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.TOP, 0,0);
                    t.show();
                }

                if (shouldReturnFromRecordingState) {
                    // Should return from recording state
                    // and re enable user interaction
                    isRecording = false;
                    returnFromRecordingUI();
                }

            }
        });
    }

    private void handleSuccessfulTake(String outputFile) {
        try {
            // All is well, update the footage object
            Crashlytics.log(String.format("Successful take: %s", outputFile));
            Footage footage = remake.findFootage(currentSceneID);
            if (footage == null) {
                Log.e(TAG, "Error. why footage not found after finishing recording?");
                HashMap props = new HashMap<String,String>();
                props.put("story" , remake.getStory().name);
                props.put("remake_id" , remake.getOID());
                props.put("scene_id" , Integer.toString(currentSceneID));
                HMixPanel.sh().track("RECaptureOutputLocalStorageError",props);
                return;
            }

            if (footage.rawLocalFile==null) {
                // Set the raw local file for the first time.
                footage.rawLocalFile = outputFile;
                footage.rawLocalFileTime = System.currentTimeMillis();
            } else {
                // Ensure that the flow of successful upload doesn't happen
                // When the previous rawLocalFile is still uploaded to s3
                // Reporting about the new source file, will tell the manager
                // To ignore successful upload of the older source file.
                UploadManager.sh().reportSourceFileChange(footage.rawLocalFile, outputFile);
                footage.rawLocalFile = outputFile;
                footage.rawLocalFileTime = System.currentTimeMillis();
            }

            // Mark if was selfie or not.
            CameraManager cm = CameraManager.sh();
            footage.selfie = cm.isSelfie() ? 1 : 0;

            // Inform server about availability of new take.
            String remakeID = remake.getOID();

            int sceneID = footage.sceneID;
            String takeID = footage.getTakeID();
            HomageServer.sh().putFootage(
                    remakeID,
                    sceneID,
                    takeID,
                    footage.isSelfie(),
                    null
            );

            footage.save();
            updateScenesList();
            UploadManager.sh().checkUploader();
            stateMachine.advanceState();
            stateMachine.handleCurrentState();

            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id" , Integer.toString(currentSceneID));
            HMixPanel.sh().track("RECaptureOutputValidated",props);
        } catch (Exception ex) {
            Log.e(TAG, "Checking recording finished failed.", ex);
        }
    }

    private void checkFinishedRecording(String outputFile) {
        try {
            isRecording = false;

            // Check the output file
            File file = new File(outputFile);
            if (file.exists()) {
                Log.d(TAG, String.format("Output file exists: %s", outputFile));
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(outputFile);
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long timeInmillisec = Long.parseLong( time );
                Log.d(TAG, String.format("Output file length: %d", timeInmillisec));

            } else {
                throw new IOException(String.format("Failed saving output file: %s", outputFile));
            }

            // All is well, update the footage object
            Footage footage = remake.findFootage(currentSceneID);
            if (footage == null) {
                Log.e(TAG, "Error. why footage not found after finishing recording?");
            }

            if (footage.rawLocalFile==null) {
                // Set the raw local file for the first time.
                footage.rawLocalFile = outputFile;
                footage.rawLocalFileTime = System.currentTimeMillis();
            } else {
                // Ensure that the flow of successful upload doesn't happen
                // When the previous rawLocalFile is still uploaded to s3
                // Reporting about the new source file, will tell the manager
                // To ignore successful upload of the older source file.
                UploadManager.sh().reportSourceFileChange(footage.rawLocalFile, outputFile);
                footage.rawLocalFile = outputFile;
                footage.rawLocalFileTime = System.currentTimeMillis();
            }

            // Inform server about availability of new take.
            String remakeID = remake.getOID();
            int sceneID = footage.sceneID;
            String takeID = footage.getTakeID();
            HomageServer.sh().putFootage(
                    remakeID,
                    sceneID,
                    takeID,
                    footage.isSelfie(),
                    null
            );

            footage.save();
            updateScenesList();
            UploadManager.sh().checkUploader();
            stateMachine.advanceState();
            stateMachine.handleCurrentState();

        } catch (Exception ex) {
            Log.e(TAG, "Checking recording finished failed.", ex);
        }
    }

    private void returnFromRecordingUI() {
        // Show the overlay buttons
        showOverlayButtons(false);

        // Show the controls drawer
        showControlsDrawer(false);

        // Stop and hide the progressbar
        ProgressBar progressBar = aq.id(R.id.recordingProgressBar).getProgressBar();
        progressBar.clearAnimation();
        progressBar.setVisibility(View.GONE);

        // Mark as not recording
        isRecording = false;

        // Update the script bar
        updateScriptBar();
    }

    private void askUserIfWantToCloseRecorder()
    {
        Crashlytics.log("Asking user if want to close recorder");

        CameraManager cm = CameraManager.sh();
        cm.cancelRecording();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.recorder_dismiss_title);
        builder.setMessage(R.string.recorder_dismiss_message);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                HashMap props = new HashMap<String,String>();
                props.put("story" , remake.getStory().name);
                props.put("remake_id" , remake.getOID());
                HMixPanel.sh().track("UserClosedRecorder",props);

                Crashlytics.log("User requested to abort recorder");
                recorderDoneWithReason(DISMISS_REASON_USER_ABORTED_PRESSING_X);

            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                itsPreviewTime = true;

            }
        });
        builder.create().show();
        cancelCountingDownToRecording();
    }
    //endregion

    private void updateScriptBar() {
        aq.id(R.id.topScript).visibility(View.GONE);
        aq.id(R.id.bottomScript).visibility(View.GONE);
        HashMap props = new HashMap<String,String>();
        props.put("story" , remake.getStory().name);
        props.put("remake_id" , remake.getOID());
        if (!shouldShowScriptBar) {
            HMixPanel.sh().track("REHideScript", props);
            return;
        } else {
            HMixPanel.sh().track("REShowScript",props);
        }

        if (isRecording) {
            aq.id(R.id.bottomScript).visibility(View.VISIBLE);
            return;
        }

        if (isControlsDrawerOpen()) {
            aq.id(R.id.topScript).visibility(View.VISIBLE);
        }
    }


    //region *** UI updates ***
    private void updateUIForSceneID(int sceneID) {
        Story story = remake.getStory();
        Scene scene = story.findScene(sceneID);
        aq.id(R.id.silhouette).image(scene.silhouetteURL, false, true, 0, 0, null, R.anim.animation_fadein, AQuery.RATIO_PRESERVE);
        aq.id(R.id.sceneNumber).text(scene.getTitle());
        aq.id(R.id.sceneTime).text(scene.getTimeString());
        aq.id(R.id.topScriptText).text(scene.script);
        aq.id(R.id.bottomScriptText).text(scene.script);
        if (videosAdapter != null) {
            videosAdapter.sceneID = sceneID;
            videosAdapter.notifyDataSetChanged();
        }
    }

    private void hideOverlayButtons(boolean animated) {
        if (animated) {
            // TODO: implement
        } else {
            aq.id(R.id.recorderOverlayDismissButton).visibility(View.GONE);
            aq.id(R.id.recorderOverlayFlipCameraButton).visibility(View.GONE);
            aq.id(R.id.recorderOverlaySceneDescriptionButton).visibility(View.GONE);
        }
        itsPreviewTime = false;
    }

    private void showOverlayButtons(boolean animated) {
        if (animated) {
            // TODO: implement
        } else {
            aq.id(R.id.recorderOverlayDismissButton).visibility(View.VISIBLE);
            aq.id(R.id.recorderOverlayFlipCameraButton).visibility(View.VISIBLE);
            aq.id(R.id.recorderOverlaySceneDescriptionButton).visibility(View.VISIBLE);
        }
        itsPreviewTime = true;
    }

    private void hideSilhouette(boolean animated) {
        if (animated) {
            aq.id(R.id.silhouette).animate(fadeOutAnimation);
        } else {
            aq.id(R.id.silhouette).visibility(View.INVISIBLE);
        }
        itsPreviewTime = false;
    }

    private void showSilhouette(boolean animated) {
        if (animated) {
            aq.id(R.id.silhouette).animate(fadeInAnimation);
        } else {
            aq.id(R.id.silhouette).visibility(View.VISIBLE);
        }
        itsPreviewTime = true;
    }
    //endregion


    private void clickedSceneAtItem(int i) {
        Footage.ReadyState readyState = footagesReadyStates.get(i);
        Scene scene = scenes.get(i);

        HashMap props = new HashMap<String,String>();
        props.put("story" , remake.getStory().name);
        props.put("remake_id" , remake.getOID());
        props.put("scene_id" , Integer.toString(currentSceneID));
        HMixPanel.sh().track("REReturnToScene",props);

        if (readyState == Footage.ReadyState.STILL_LOCKED) {
            Toast.makeText(
                    RecorderActivity.this,
                    "Scene locked.\nFinish with previous scenes first.",
                    Toast.LENGTH_SHORT).show();
        } else {
            currentSceneID = scene.getSceneID();
            updateUIForSceneID(currentSceneID);
            updateScenesList();
            Toast.makeText(
                    RecorderActivity.this,
                    String.format("Shooting Scene %d", currentSceneID),
                    Toast.LENGTH_SHORT).show();

        }
    }

    private void clickedRetakeAtItem(int i) {
        Footage.ReadyState readyState = footagesReadyStates.get(i);
        Scene scene = scenes.get(i);
        if (readyState != Footage.ReadyState.READY_FOR_SECOND_RETAKE) return;

        Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayRetakeSceneQuestionDlgActivity.class);
        myIntent.putExtra("remakeOID",remake.getOID());
        myIntent.putExtra("sceneID",scene.getSceneID());
        RecorderActivity.this.startActivityForResult(myIntent, ACTION_BY_RESULT_CODE);
        overridePendingTransition(R.anim.animation_fadein_with_zoom, R.anim.animation_fadeout_with_zoom);
    }

    private void updateVideosPageIndicator(int position) {
        if (currentVideoPage == position) return;
        if (position == 0) {
            videosPageIndicator1.setVisibility(View.VISIBLE);
            videosPageIndicator2.setVisibility(View.INVISIBLE);
        } else {
            videosPageIndicator1.setVisibility(View.INVISIBLE);
            videosPageIndicator2.setVisibility(View.VISIBLE);
        }
        currentVideoPage = position;
    }

    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */

    private ViewPager.SimpleOnPageChangeListener onVideosPagerChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            videosAdapter.doneIfPlaying();
        }

        @Override
        public void onPageSelected(int position) {
            updateVideosPageIndicator(position);
        }
    };

    private View.OnClickListener onClickedOpenDrawerButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            openControlsDrawer(true);

        }
    };

    private View.OnClickListener onClickedCloseDrawerButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeControlsDrawer(true);

        }
    };

    private View.OnClickListener onClickedRecordButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (isRecording) return;

            if (isCountingDownToRecording()) {
                cancelCountingDownToRecording();
            } else {
                startCountingDownToRecording();
            }
        }
    };

    //Warning stuff
    private View.OnClickListener onClickedWarningButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (isRecording) return;
            // Open warning dialogue
            Intent i = new Intent(RecorderActivity.this,WarningOverlayDlgActivity.class);
            startActivityForResult(i, DONT_SHOW_THIS_AGAIN);
        }
    };

    public void ShowWarningButton(){
       warningCountDown--;
        if(!prefs.getBoolean(SettingsActivity.DONT_SHOW_WARNING_AGAIN,false) && warningCountDown == 0 && itsPreviewTime) {
            warningButton.performClick();
//            warningCountDown = warningCountDownFrom;
        }
        isBackgroundDetectionRunning = false;
        if(recordButton.isClickable() && warningButton.getVisibility() != View.VISIBLE) {
            warningButton.setVisibility(View.VISIBLE);
            warningButton.setClickable(true);
        }
    }

    public void HideWarningButton(){
        //Reset warning
//        warningCountDown = warningCountDownFrom;
        isBackgroundDetectionRunning = false;
        if(warningButton.getVisibility() == View.VISIBLE) {
            warningButton.setVisibility(View.GONE);
            warningButton.setClickable(false);
        }
    }

    //end of warning stuff

    private View.OnClickListener onClickedRecorderDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            itsPreviewTime = false;
            askUserIfWantToCloseRecorder();
        }
    };

    private View.OnClickListener onClickedSceneDescriptionButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlaySceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID",remake.getOID());
            myIntent.putExtra("sceneID",currentSceneID);

            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id", Integer.toString(currentSceneID));
            HMixPanel.sh().track("REMenuSceneDirection", props);
            CameraManager.sh().releaseCamera();
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_DO_NOTHING);
            overridePendingTransition(R.anim.animation_fadein_with_zoom, R.anim.animation_fadeout_with_zoom);
        }
    };

    private View.OnClickListener onClickedToggleScriptButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            shouldShowScriptBar = !shouldShowScriptBar;
            toggleShowHideScriptButtonText(shouldShowScriptBar);
            updateScriptBar();
        }
    };

    private void toggleShowHideScriptButtonText(boolean showScriptBar) {
        if(showScriptBar) {
            ((Button) recorderSceneScriptButton).setText(R.string.button_hide_script);
        }
        else
        {
            ((Button) recorderSceneScriptButton).setText(R.string.button_show_script);
        }
    }


    private View.OnClickListener onClickedWelcomeScreenDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            aq.id(R.id.welcomeScreenOverlay).animate(R.anim.animation_fadeout_with_zoom, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    aq.id(R.id.welcomeScreenOverlay).visibility(View.GONE);
                    aq.id(R.id.welcomeScreenDismissButton).visibility(View.GONE);
                    aq.id(R.id.welcomeScreenDismissButton).clickable(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            if (stateMachine.currentState == RecorderState.JUST_STARTED) {
                try {
                    stateMachine.advanceState();
                    stateMachine.handleCurrentState();
                } catch (Exception ex) {}
            }
        }
    };

    private View.OnClickListener onClickedFlipCameraButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        // Analytics
        HashMap props = new HashMap<String,String>();
        props.put("story" , remake.getStory().name);
        props.put("remake_id" , remake.getOID());
        props.put("scene_id" , Integer.toString(currentSceneID));
        HMixPanel.sh().track("REFlipCamera",props);

        // Flip camera by saving state, opening another camera and restarting activity.
        showCurtainsAnimated(false);
        flip();
        }
    };

    private void flip() {
        CameraManager cm = CameraManager.sh();
        cm.pauseCameraPreviewIfInitialized();
        cm.flipCamera();
        cm.toastSelectedCamera(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CameraManager cm = CameraManager.sh();
                cm.resumeCameraPreviewIfInitialized();
            }
        }, 200);
    }

    private View.OnClickListener onClickedCreateMovieButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!remake.allScenesTaken()) return;
            try {
                stateMachine.advanceState();
                stateMachine.handleCurrentState();
            } catch (RecorderException e) {
            }
        }
    };



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            switch (requestCode) {
                case ACTION_DO_NOTHING:
                    break;
                case ACTION_ADVANCE_STATE:
                    stateMachine.advanceState();
                    break;
                case ACTION_HANDLE_STATE:
                    stateMachine.handleCurrentState();
                    break;
                case ACTION_ADVANCE_AND_HANDLE_STATE:
                    stateMachine.advanceState();
                    stateMachine.handleCurrentState();
                    break;
                case ACTION_BY_RESULT_CODE:
                    if (resultCode == RecorderOverlayDlgActivity.ResultCode.NEXT_SCENE.getValue()) {
                        // User wanted to shoot the next scene.
                        stateMachine.advanceState();
                        stateMachine.handleCurrentState();
                    } else if (resultCode == RecorderOverlayDlgActivity.ResultCode.RETAKE_SCENE.getValue()) {
                        // User wanted to retake current scene.
                        if (data!=null) {
                            int sceneID = data.getIntExtra("sceneID", -1);
                            if (sceneID > 0) {
                                currentSceneID = sceneID;
                                updateUIForSceneID(sceneID);
                            }
                        }
                        stateMachine.setState(RecorderState.MAKING_A_SCENE);
                        stateMachine.handleCurrentState();
                    } else if (resultCode == RecorderOverlayDlgActivity.ResultCode.MOVIE_MARKED_BY_USER_FOR_CREATION.getValue()) {
                        // User marked this movie for creation. Bye bye.
                        recorderDoneWithReason(DISMISS_REASON_FINISHED_REMAKE);
                    } else if (resultCode == RecorderOverlayDlgActivity.ResultCode.NOP.getValue()) {
                        return;
                    } else {
                        throw new RecorderException(String.format("Unimplemented result code for recorder %d", resultCode));
                    }
                    break;
                case (DONT_SHOW_THIS_AGAIN) : {
                    if (resultCode == Activity.RESULT_CANCELED) {
                        prefsEditor.putBoolean(SettingsActivity.DONT_SHOW_WARNING_AGAIN, true);
                        prefsEditor.commit();
                    }
                    break;
                }
                default:
                    throw new RecorderException(String.format("Unimplemented request code for recorder %d", requestCode));
            }
        }
        catch (RecorderException ex) {
            Log.e(TAG, "Critical error when returning to recorder activity.", ex);
        }
    }
    //endregion

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.
        //Log.d(TAG, "ST onFrameAvailable");
    }
    //endregion
}


