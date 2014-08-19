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
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
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
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.media.camera.CameraManager;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.homage.networking.uploader.UploadManager;
import com.homage.views.ActivityHelper;
import com.homage.views.Pacman;

import java.io.File;
import java.io.IOException;
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
public class RecorderActivity extends Activity {
    private String TAG = "TAG_"+getClass().getName();

    LayoutInflater inflater;

    final public static int RECORDER_CLOSED = 666;

    private AQuery aq;
    private View controlsDrawer;
    private View recordButton;
    private View recorderFullDetailsContainer;
    private View recorderShortDetailsContainer;
    private ListView scenesListView;
    private ViewPager videosPager;
    private RecorderVideosPagerAdapter videosAdapter;

    // Remake info
    protected Remake remake;
    protected Story story;
    protected List<Scene> scenes;
    protected List<Footage.ReadyState> footagesReadyStates;
    protected int currentSceneID;
    protected String outputFile;

    // Actions & State
    private boolean isRecording;
    private final static int ACTION_DO_NOTHING = 1;
    private final static int ACTION_HANDLE_STATE = 2;
    private final static int ACTION_ADVANCE_STATE = 3;
    private final static int ACTION_ADVANCE_AND_HANDLE_STATE = 4;
    private final static int ACTION_BY_RESULT_CODE = 5;
    private RecorderStateMachine stateMachine;
    private Handler counterDown;
    protected int countDown;
    protected boolean canceledCountDown;
    protected final static int countDownFrom = 3;

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

    // Layouts and views
    private Animation fadeInAnimation, fadeOutAnimation;
    public int viewHeightForClosingControlsDrawer;
    private boolean viewsInitialized;
    private FrameLayout recPreviewContainer;

    private int currentVideoPage = -1;
    private ImageView videosPageIndicator1, videosPageIndicator2;


    // Media
    MediaPlayer mp;

    // Error handling
    public class RecorderException extends Exception {
        public RecorderException(String message) {
            super(message);
        }
    }


    //region *** Activity lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewsInitialized = false;
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Initialize camera manger, if not initialized yet.
        if (!CameraManager.sh().isInitialized()) {
            CameraManager.sh().init(HomageApplication.getContext());
        }


        // Get info about the remake
        Bundle b = getIntent().getExtras();
        String remakeOID = b.getString("remakeOID");
        remake = Remake.findByOID(remakeOID);
        story = remake.getStory();
        isRecording = false;

        // Use back face camera by default.
        // User will need to switch manually to selfie if interested.
        CameraManager.sh().resetToPreferBackCamera();

        //region *** Layout initializations ***
        Log.d(TAG, String.format("Started recorder for remake: %s", remake.getOID()));

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Set the content layout, load resources and store some references to views
        setContentView(R.layout.activity_recorder);

        // AQuery & Some references (just for shorter syntax later)
        aq = new AQuery(this);
        controlsDrawer = aq.id(R.id.recorderControlsDrawer).getView();
        recordButton = aq.id(R.id.recorderRecordButton).getView();
        recorderFullDetailsContainer = aq.id(R.id.recorderFullDetailsContainer).getView();
        recorderShortDetailsContainer = aq.id(R.id.recorderShortDetailsContainer).getView();
        scenesListView = aq.id(R.id.scenesListView).getListView();
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.animation_fadein);
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.animation_fadeout);
        videosPager = (ViewPager)aq.id(R.id.videosPager).getView();
        videosPageIndicator1 = aq.id(R.id.videosPageIndicator1).getImageView();
        videosPageIndicator2 = aq.id(R.id.videosPageIndicator2).getImageView();

        closeControlsDrawer(false);
        updateScriptBar();
        scenesListView.setVisibility(View.GONE);

        // Preload and cache silhouettes in the background
        List<Scene> scenes = story.getScenesOrdered();
        for (Scene scene : scenes) {
            Log.d(TAG, String.format("Preloading %s", scene.silhouetteURL));
            aq.image(scene.silhouetteURL, false, true, 0, 0);
        }
        //endregion

        updateVideosPageIndicator(0);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/

        // Pushed THE button! (Clicked on the record button)
        aq.id(R.id.recorderRecordButton).clicked(onClickedRecordButton);

        // Dragging the drawer up and down.
        aq.id(R.id.recorderControlsDrawer).getView().setOnTouchListener(new OnDraggingControlsDrawerListener(this));

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
        // Clicked on a scene nunber in the list of scenes.
        // User tries to select a scene in the list.
        // aq.id(R.id.scenesListView).itemClicked(onClickedSceneItem);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        closeControlsDrawer(true);

        // Reconnect to the camera if needed.
        if (!CameraManager.sh().isCameraAvailable()) {
            CameraManager.sh().restartCamera();
        }
    }

    @Override
    protected void onStop() {
        super.onResume();
        CameraManager.sh().releaseCamera();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager cm = CameraManager.sh();
        cm.releaseMediaRecorder();
        cm.releaseCamera();
        recPreviewContainer = null;
    }
    //endregion


    //region *** Views Initializations ***
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!viewsInitialized) initViews();
        if (recPreviewContainer==null) initCameraPreview();
        if (scenesListView.getAdapter()==null) {
            initScenesAdapter();
        } else {
            updateScenesList();
        }

        //region *** State initialization ***
        try {
            if (stateMachine == null) {
                stateMachine = new RecorderStateMachine();
                stateMachine.handleCurrentState();
            }
        } catch (RecorderException ex) {
            Log.e(TAG, "Recorder state machine error.", ex);
        }
        //endregion
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
        //hideControlsDrawer(false);

        // Videos paging adapter
        videosAdapter = new RecorderVideosPagerAdapter(this, this.remake);
        ViewPager videosPager = (ViewPager)aq.id(R.id.videosPager).getView();
        videosPager.setAdapter(videosAdapter);
        videosPager.setOnPageChangeListener(onVideosPagerChangeListener);

        // Black bars at the top and bottom of silhouette + camera preview
        // (used to crop camera preview to 16/9 in case of other screen aspect ratios)
        ImageView silhouette = aq.id(R.id.silhouette).getImageView();
        int w = silhouette.getWidth();
        int h = silhouette.getHeight();
        int m = (h - (w * 9 / 16)) / 2;

        View topBlackBar = aq.id(R.id.blackBarTop).getView();
        View bottomBlackBar = aq.id(R.id.blackBarBottom).getView();

        if (m > 0) {

            android.widget.RelativeLayout.LayoutParams params;

            params = (android.widget.RelativeLayout.LayoutParams) topBlackBar.getLayoutParams();
            params.height = m;
            topBlackBar.setLayoutParams(params);

            params = (android.widget.RelativeLayout.LayoutParams) bottomBlackBar.getLayoutParams();
            params.height = m;
            bottomBlackBar.setLayoutParams(params);
        } else {
            topBlackBar.setVisibility(View.GONE);
            bottomBlackBar.setVisibility(View.GONE);
        }
    }
    //endregion

    //region *** Camera ***
    private void initCameraPreview() {
        // We will show the video feed of the camera
        // on a preview texture in the background.
        recPreviewContainer = (FrameLayout)findViewById(R.id.preview_container);
        CameraManager cm = CameraManager.sh();

        cm.startCameraPreviewInView(RecorderActivity.this, recPreviewContainer);

        // After initialized, fade in the camera by fading out the "curtains" slowly
        hideCurtainsAnimated(true);
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
            controlsDrawerOpened();
        }
    }


    private void controlsDrawerClosed() {
        if (recordButton.isClickable()) return;

        recordButton.startAnimation(fadeInAnimation);
        recordButton.setClickable(true);
        recorderFullDetailsContainer.startAnimation(fadeOutAnimation);
        recorderShortDetailsContainer.startAnimation(fadeInAnimation);
        recorderFullDetailsContainer.setVisibility(View.GONE);
        scenesListView.setVisibility(View.GONE);
        videosPager.setVisibility(View.GONE);
        videosAdapter.hideSurfaces();
        showOverlayButtons(false);

        final CameraManager cm = CameraManager.sh();
        videosAdapter.done();
        updateScriptBar();
        aq.id(R.id.createMovieButton).visibility(View.GONE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cm.preview.show();
                hideCurtainsAnimated(true);
            }
        }, 400);
    }

    private void controlsDrawerOpened() {
        if (!recordButton.isClickable()) return;

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

        CameraManager cm = CameraManager.sh();
        cm.preview.hide();
        showCurtainsAnimated(false);
        updateScriptBar();

        // Check if need to show the create movie button or not
        // Will be shown only if already taken all footages (at least once)
        if (remake.allScenesTaken()) {
            aq.id(R.id.createMovieButton).visibility(View.VISIBLE);
        } else {
            aq.id(R.id.createMovieButton).visibility(View.GONE);
        }
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

        public RecorderStateMachine() {
            this.currentState = RecorderState.JUST_STARTED;
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
                    throw new RecorderException(String.format("Unimplemented recorder state %s", currentState));
            }
        }

        private void advanceState() throws RecorderException {
            switch (currentState) {
                case JUST_STARTED:
                    // User got the "just starting" message.
                    // Now we can get to business.
                    // NEXT, we will show a message about the next scene to shoot.
                    currentState = RecorderState.SCENE_MESSAGE;
                    break;
                case SCENE_MESSAGE:
                    // User dismissed the scene message.
                    // Enter the "making a scene" state.
                    // (gives the user control of the recorder)
                    currentState = RecorderState.MAKING_A_SCENE;
                    showControlsDrawer(false);
                    break;
                case MAKING_A_SCENE:
                    // User recorded footage for a scene in a remake.
                    // Advance state to next scene message or finished movie message
                    if (remake.allScenesTaken()) {
                        Log.d(TAG, "All scenes taken");
                        currentState = RecorderState.FINISHED_ALL_SCENES_MESSAGE;
                    } else {
                        int nextSceneID = remake.nextReadyForFirstRetakeSceneID();
                        Log.d(TAG, String.format("Will advance to next scene: %d", nextSceneID));
                        currentState = RecorderState.FINISHED_A_SCENE_MESSAGE;
                    }
                    break;
                case FINISHED_A_SCENE_MESSAGE:
                    // User chosen to continue to next scene, after a finished scene message.
                    currentSceneID = remake.nextReadyForFirstRetakeSceneID();
                    currentState = RecorderState.MAKING_A_SCENE;
                    break;
                default:
                    throw new RecorderException(String.format("Unimplemented when advancing state %s", currentState));
            }
        }

        private void justStarted() throws RecorderException {
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
            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlaySceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID", remake.getOID());
            myIntent.putExtra("sceneID",currentSceneID);
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_ADVANCE_AND_HANDLE_STATE);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
            if (currentSceneID>0) {
                updateUIForSceneID(currentSceneID);
            }
        }

        private void makingAScene() {
            showControlsDrawer(false);
            updateScenesList();
            updateUIForSceneID(currentSceneID);
        }

        private void finishedASceneMessage() {
            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayFinishedSceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID", remake.getOID());
            myIntent.putExtra("sceneID",currentSceneID);
            int nextSceneID = remake.nextReadyForFirstRetakeSceneID();
            updateUIForSceneID(nextSceneID);
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_BY_RESULT_CODE);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }

        private void finishedAllScenesMessage() {
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
        setResult(dismissReason, result);
        finish();
        overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
    }

    private boolean isCountingDownToRecording() {
        if (counterDown == null) return false;
        return true;
    }

    private void cancelCountingDownToRecording() {
        if (!isCountingDownToRecording()) return;
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
        updateScriptBar();

        //
        // Async listener that is notified while recording (and end of recording).
        //
        final MediaRecorder.OnInfoListener onRecordingInfoListener = new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what != MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) return;
                Log.d(TAG, String.format("finished (%d %d) recording duration %d", what, extra, scene.duration));
                CameraManager.sh().stopRecording();
                isRecording = false;
                returnFromRecordingUI();
                if (outputFile != null) {
                    checkFinishedRecording(outputFile);
                } else {
                    Log.e(TAG, "Why missing outputFile path is missing when finishing recording?");
                }
            }
        };

        //
        // Start recording
        //
        new AsyncTask<Void, Integer, Void>(){
            @Override
            protected Void doInBackground(Void... arg0) {
                outputFile = CameraManager.sh().startRecording(scene.duration, onRecordingInfoListener);
                if (outputFile == null) {
                    Toast.makeText(
                            RecorderActivity.this,
                            "Failed to start recording.",
                            Toast.LENGTH_SHORT).show();
                    isRecording = false;
                    returnFromRecordingUI();
                    return null;
                }
                Log.d(TAG, String.format("Started recording to local file: %s", outputFile));
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
            }
        }.execute((Void)null);

        // Update UI
        hideControlsDrawer(false);
        hideOverlayButtons(false);
        hideSilhouette(false);

        // Progress bar animation
        ProgressBar progressBar = aq.id(R.id.recordingProgressBar).getProgressBar();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setAlpha(0.7f);
        ProgressBarAnimation anim = new ProgressBarAnimation(progressBar, 0, 100);
        anim.setDuration(scene.duration);
        progressBar.startAnimation(anim);
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
        showOverlayButtons(false);
        showControlsDrawer(false);
        aq.id(R.id.recordingProgressBar).visibility(View.GONE);
        isRecording = false;
        updateScriptBar();
    }

    private void askUserIfWantToCloseRecorder()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.recorder_dismiss_title);
        builder.setMessage(R.string.recorder_dismiss_message);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                recorderDoneWithReason(DISMISS_REASON_USER_ABORTED_PRESSING_X);
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }
    //endregion

    private void updateScriptBar() {
        aq.id(R.id.topScript).visibility(View.GONE);
        aq.id(R.id.bottomScript).visibility(View.GONE);
        if (!shouldShowScriptBar) return;

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
        aq.id(R.id.silhouette).image(scene.silhouetteURL, false, true, 0, 0, null, 0, AQuery.RATIO_PRESERVE);
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
    }

    private void showOverlayButtons(boolean animated) {
        if (animated) {
            // TODO: implement
        } else {
            aq.id(R.id.recorderOverlayDismissButton).visibility(View.VISIBLE);
            aq.id(R.id.recorderOverlayFlipCameraButton).visibility(View.VISIBLE);
            aq.id(R.id.recorderOverlaySceneDescriptionButton).visibility(View.VISIBLE);
        }
    }

    private void hideSilhouette(boolean animated) {
        if (animated) {
            // TODO: implement
        } else {
            aq.id(R.id.silhouette).visibility(View.INVISIBLE);
        }
    }
    //endregion


    private void clickedSceneAtItem(int i) {
        Footage.ReadyState readyState = footagesReadyStates.get(i);
        Scene scene = scenes.get(i);
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

    /*
    private AdapterView.OnItemClickListener onClickedSceneItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Footage.ReadyState readyState = footagesReadyStates.get(i);
            Scene scene = scenes.get(i);
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
    };
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

    private View.OnClickListener onClickedRecorderDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            askUserIfWantToCloseRecorder();
        }
    };

    private View.OnClickListener onClickedSceneDescriptionButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlaySceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID",remake.getOID());
            myIntent.putExtra("sceneID",currentSceneID);
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_DO_NOTHING);
            overridePendingTransition(R.anim.animation_fadein_with_zoom, R.anim.animation_fadeout_with_zoom);
        }
    };

    private View.OnClickListener onClickedToggleScriptButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            shouldShowScriptBar = !shouldShowScriptBar;
            updateScriptBar();
        }
    };



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
            CameraManager.sh().flipCamera();
        }
    };

    private View.OnClickListener onClickedCreateMovieButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!remake.allScenesTaken()) return;
            //stateMachine.setState(RecorderState.FINISHED_ALL_SCENES_MESSAGE);
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
                default:
                    throw new RecorderException(String.format("Unimplemented request code for recorder %d", requestCode));
            }
        }
        catch (RecorderException ex) {
            Log.e(TAG, "Critical error when returning to recorder activity.", ex);
        }
    }

    //endregion
}
