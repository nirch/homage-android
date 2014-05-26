/*
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
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.media.camera.CameraManager;
import com.homage.model.Remake;
import com.homage.model.User;
import com.homage.views.ActivityHelper;

import java.util.logging.Handler;


/*
 * This class consists ...
 *
 * <p>The...</p>
 *
 * @author  Homage
 * @author  Aviv Wolf
 * @since   0.1
 */
public class RecorderActivity extends Activity {

    private String TAG = "TAG_"+getClass().getName();

    final public static int RECORDER_CLOSED = 666;

    private AQuery aq;

    private View recoderView;
    private View controlsDrawer;
    private View recordButton;
    private View recorderFullDetailsContainer;
    private View recorderShortDetailsContainer;

    // Remake info
    protected Remake remake;
    protected int currentSceneID;

    // Actions & State
    private final static int ACTION_DO_NOTHING = 1;
    private final static int ACTION_HANDLE_STATE = 2;
    private final static int ACTION_ADVANCE_STATE = 3;
    private final static int ACTION_ADVANCE_AND_HANDLE_STATE = 4;
    private RecorderStateMachine stateMachine;
    static private enum RecorderState {
        JUST_STARTED,
        INTRO_MESSAGE,
        SCENE_MESSAGE,
        MAKING_A_SCENE,
        FINISHED_A_SCENE_MESSAGE,
        EDITING_TEXTS,
        FINISHED_ALL_SCENES_MESSAGE,
        USER_REQUEST_TO_CHECK_WHAT_NEXT,
        HELP_SCREENS
    }


    // Layouts and views
    private Animation fadeInAnimation, fadeOutAnimation;
    private int viewHeightForClosingControlsDrawer;
    private boolean viewsInitialized;
    private FrameLayout recPreviewContainer;


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

        // Aquery
        aq = new AQuery(this);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the recorder full screen activity.");

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Set the content layout, load resources and store some references to views
        setContentView(R.layout.activity_recorder);
        recoderView = aq.id(R.id.recoderView).getView();
        controlsDrawer = aq.id(R.id.recorderControlsDrawer).getView();
        recordButton = aq.id(R.id.recorderRecordButton).getView();
        recorderFullDetailsContainer = aq.id(R.id.recorderFullDetailsContainer).getView();
        recorderShortDetailsContainer = aq.id(R.id.recorderShortDetailsContainer).getView();
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.animation_fadein);
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.animation_fadeout);
        //endregion

        //region *** Camera video preview preparation ***
        if (recPreviewContainer==null) {
            initCameraPreview();
        }
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

        // Dragging the drawer up and down.
        aq.id(R.id.recorderControlsDrawer).getView().setOnTouchListener(new OnDraggingControlsDrawerListener(this));

        // Clicked on the close controls drawer button.
        aq.id(R.id.recorderCloseDrawerButton).clicked(onClickedCloseDrawerButton);

        // Dismissing the recorder.
        aq.id(R.id.recorderDismissButton).clicked(onClickedRecorderDismissButton);
        //endregion
    }

    @Override
    protected void onDestroy() {
        super.onPause();
        CameraManager cm = CameraManager.sh();
        cm.releaseMediaRecorder();
        cm.releaseCamera();
    }
    //endregion


    //region *** Views Initializations ***
    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        if (!viewsInitialized) initViews();
    }

    private void initViews() {
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
    }
    //endregion


    //region *** Camera ***
    private void initCameraPreview() {
        // We will show the video feed of the camera
        // on a preview texture in the background.
        recPreviewContainer = (FrameLayout)findViewById(R.id.preview_container);

        // TODO: Initialize CameraManager using AsyncTask
        // TODO: remove the postDelayed hack. implement this correctly.
        recoderView.postDelayed(new Runnable() {
            @Override
            public void run() {
                CameraManager cm = CameraManager.sh();
                cm.startCameraPreviewInView(RecorderActivity.this, recPreviewContainer);

                // After initialized, fade in the camera by fading out the "curtains" slowly
                Animation removeCurtainsAnim = AnimationUtils.loadAnimation(RecorderActivity.this, R.anim.animation_fadeout);
                removeCurtainsAnim.setDuration(2500);
                aq.id(R.id.recorderCurtains).animate(removeCurtainsAnim);
            }
        }, 1000);
    }
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

    public void closeControlsDrawer(boolean animated) {
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

    private void toggleControlsDrawer(boolean animated) {
        if (isControlsDrawerOpen()) {
            closeControlsDrawer(animated);
        } else {
            openControlsDrawer(animated);
        }
    }

    private void controlsDrawerClosed() {
        if (recordButton.isClickable()) return;
        recordButton.startAnimation(fadeInAnimation);
        recordButton.setClickable(true);
        recorderFullDetailsContainer.startAnimation(fadeOutAnimation);
        recorderShortDetailsContainer.startAnimation(fadeInAnimation);

    }

    private void controlsDrawerOpened() {
        if (!recordButton.isClickable()) return;
        recordButton.startAnimation(fadeOutAnimation);
        recordButton.setVisibility(View.INVISIBLE);
        recordButton.setClickable(false);
        recorderFullDetailsContainer.setAlpha(1);
        recorderFullDetailsContainer.startAnimation(fadeInAnimation);
        recorderShortDetailsContainer.startAnimation(fadeOutAnimation);
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

        private void handleCurrentState() throws RecorderException {
            switch (currentState) {
                case JUST_STARTED: justStarted(); break;
                default:
                    throw new RecorderException(String.format("Unimplemented recorder state %s", currentState));
            }
        }

        private void advanceState() throws RecorderException {
            switch (currentState) {
                case JUST_STARTED:
                    currentState = RecorderState.INTRO_MESSAGE;
                    break;
                default:
                    throw new RecorderException(String.format("Unimplemented recorder state %s", currentState));
            }
        }

        private void justStart() throws RecorderException {
            // TODO: choose latest scene here if a continued remake.
            //
            // Select the first scene requiring a first retake.
            // If none found (all footages already taken by the user),
            // will select the last scene for this remake instead.
            //
            currentSceneID = remake.nextReadyForFirstRetakeSceneID();
            _currentSceneID = [self.remake nextReadyForFirstRetakeSceneID];
            if (!self.currentSceneID) _currentSceneID = [self.remake lastSceneID];
            [self updateUIForSceneID:self.currentSceneID];

            // Just started. Show welcome message if user entered for the first time.
            // If not here for the first time, skip.
            User user = User.getCurrent();
            if (user.isFirstUse || 1==1) {
                Intent myIntent = new Intent(RecorderActivity.this, RecorderWelcomeActivity.class);
                RecorderActivity.this.startActivityForResult(myIntent, ACTION_ADVANCE_AND_HANDLE_STATE);
                overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
            } else {
                advanceState();
                handleCurrentState();
            }
        }

    }
    //endregion


    //region *** Recorder's actions ***
    /**
     *   ======================
     *   The recorder's actions
     *   ======================
     */

    private void recorderDone() {
        /**
         This is the end, beautiful friend
         This is the end, my only friend, the end
         Of our elaborate plans, the end
         Of everything that stands, the end
         No safety or surprise, the end
         I'll never look into your eyes, again
         */
        finish();
        overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
    }
    //endregion


    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
    View.OnClickListener onClickedCloseDrawerButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            closeControlsDrawer(true);
        }
    };

    View.OnClickListener onClickedRecorderDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            recorderDone();
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
                default:
                    throw new RecorderException("Unimplemented return request code for recorder");
            }
        }
        catch (RecorderException ex) {
            Log.e(TAG, "Critical error when returning to recorder activity.", ex);
        }
    }

    //endregion
}
