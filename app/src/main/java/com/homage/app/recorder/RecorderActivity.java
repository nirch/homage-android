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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.media.camera.CameraManager;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.model.User;
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

    // Reasons for the dismissal of the recorder
    public static int DISMISS_REASON_USER_ABORTED_PRESSING_X = 500;
    public static int DISMISS_REASON_FINISHED_REMAKE = 600;

    // Layouts and views
    private Animation fadeInAnimation, fadeOutAnimation;
    private int viewHeightForClosingControlsDrawer;
    private boolean viewsInitialized;
    private FrameLayout recPreviewContainer;

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

        // Get info about the remake
        Bundle b = getIntent().getExtras();
        String remakeOID = b.getString("remakeOID");
        remake = Remake.findByOID(remakeOID);
        story = remake.getStory();
        isRecording = false;

        // Prepare sound
        mp = MediaPlayer.create(getApplicationContext() , R.raw.cinema_countdown);

        //region *** Layout initializations ***
        Log.d(TAG, String.format("Started the recorder for remake:", remake.getOID()));

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
        //hideControlsDrawer(false);
        closeControlsDrawer(false);
        scenesListView.setVisibility(View.GONE);
        //endregion

        //region *** State initialization ***
        try {
            stateMachine = new RecorderStateMachine();
            stateMachine.handleCurrentState();
        } catch (RecorderException ex) {
            Log.e(TAG, "Recorder state machine error.", ex);
        }
        //endregion

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

        // Clicked the dismiss button on the "Welcome screen" overlay.
        aq.id(R.id.welcomeScreenDismissButton).clicked(onClickedWelcomeScreenDismissButton);

        // Clicked on a scene nunber in the list of scenes.
        // User tries to select a scene in the list.
        aq.id(R.id.scenesListView).itemClicked(onClickedSceneItem);

        //endregion
    }

    @Override
    protected void onDestroy() {
        super.onPause();
        CameraManager cm = CameraManager.sh();
        cm.releaseMediaRecorder();
        cm.releaseCamera();
        recPreviewContainer = null;
    }
    //endregion


    //region *** Views Initializations ***
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        if (!viewsInitialized) initViews();
        if (recPreviewContainer==null) initCameraPreview();
        if (scenesListView.getAdapter()==null) {
            initScenesAdapter();
        } else {
            updateScenesList();
        }
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

        videosAdapter = new RecorderVideosPagerAdapter(this, this.remake);
        ViewPager videosPager = (ViewPager)aq.id(R.id.videosPager).getView();
        videosPager.setAdapter(videosAdapter);
        videosPager.setOnPageChangeListener(onVideosPagerChangeListener);
    }
    //endregion

    //region *** Camera ***
    private void initCameraPreview() {
        // We will show the video feed of the camera
        // on a preview texture in the background.
        recPreviewContainer = (FrameLayout)findViewById(R.id.preview_container);

        // TODO: Initialize CameraManager using AsyncTask
        // TODO: remove the postDelayed hack. implement this correctly.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CameraManager cm = CameraManager.sh();
                cm.startCameraPreviewInView(RecorderActivity.this, recPreviewContainer);

                // After initialized, fade in the camera by fading out the "curtains" slowly
                hideCurtainsAnimated(true);


            }
        }, 500);
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
            hideCurtainsAnim.setDuration(300);
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

        CameraManager cm = CameraManager.sh();
        cm.preview.show();
        hideCurtainsAnimated(true);
        videosAdapter.done();
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

        CameraManager cm = CameraManager.sh();
        cm.preview.hide();
        showCurtainsAnimated(false);
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
            // TODO: choose latest scene here if a continued remake.
            //
            // Select the first scene requiring a first retake.
            // If none found (all footages already taken by the user),
            // will select the last scene for this remake instead.
            //
            // TODO: finish implementation
            currentSceneID = remake.nextReadyForFirstRetakeSceneID();
            if (currentSceneID < 1) currentSceneID = remake.lastSceneID();
            updateUIForSceneID(currentSceneID);

            // Just started. Show welcome message if user entered for the first time.
            // If not here for the first time, skip.
            User user = User.getCurrent();
            if (user.isFirstUse) {
                aq.id(R.id.welcomeScreenOverlay).visibility(View.VISIBLE);
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
            RecorderActivity.this.startActivityForResult(myIntent, ACTION_BY_RESULT_CODE);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }

        private void finishedAllScenesMessage() {
            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayFinishedAllSceneMessageDlgActivity.class);
            myIntent.putExtra("remakeOID", remake.getOID());
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

        // Start recording
        isRecording = true;

        outputFile = CameraManager.sh().startRecording();
        if (outputFile == null) {
            Toast.makeText(
                    RecorderActivity.this,
                    "Failed to start recording.",
                    Toast.LENGTH_SHORT).show();
            returnFromRecordingUI();
            return;
        }
        Log.d(TAG, String.format("Started recording to local file: %s", outputFile));

        // Update UI
        hideControlsDrawer(false);
        hideOverlayButtons(false);

        Scene scene = story.findScene(currentSceneID);
        ProgressBar progressBar = aq.id(R.id.recordingProgressBar).getProgressBar();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setAlpha(0.3f);
        ProgressBarAnimation anim = new ProgressBarAnimation(progressBar, 0, 100);
        anim.setDuration(scene.duration);
        progressBar.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                CameraManager.sh().stopRecording();
                returnFromRecordingUI();
                checkFinishedRecording(outputFile);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

        });
    }

    private void checkFinishedRecording(String outputFile) {
        try {
            isRecording = false;

            // Check the output file
            File file = new File(outputFile);
            if (file.exists()) {
                Log.d(TAG, String.format("Output file exists: %s", outputFile));


            } else {
                throw new IOException(String.format("Failed saving output file: %s", outputFile));
            }

            // All is well, update the footage object
            Footage footage = remake.findFootage(currentSceneID);
            footage.rawLocalFile = outputFile;
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


    //region *** UI updates ***
    private void updateUIForSceneID(int sceneID) {
        Story story = remake.getStory();
        Scene scene = story.findScene(sceneID);
        aq.id(R.id.silhouette).image(scene.silhouetteURL,false, true);
        aq.id(R.id.sceneNumber).text(scene.getTitle());
        aq.id(R.id.sceneTime).text(scene.getTimeString());
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
    //endregion


    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
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

    private ViewPager.SimpleOnPageChangeListener onVideosPagerChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            videosAdapter.doneIfPlaying();
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
                        stateMachine.setState(RecorderState.MAKING_A_SCENE);
                        stateMachine.handleCurrentState();
                    } else if (resultCode == RecorderOverlayDlgActivity.ResultCode.MOVIE_MARKED_BY_USER_FOR_CREATION.getValue()) {
                        // User marked this movie for creation. Bye bye.
                        recorderDoneWithReason(DISMISS_REASON_FINISHED_REMAKE);
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
