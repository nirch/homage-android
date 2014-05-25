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
    private AQuery aq;

    private View recoderView;
    private View controlsDrawer;
    private View recordButton;
    private View recorderFullDetailsContainer;
    private View recorderShortDetailsContainer;


    private Animation fadeInAnimation, fadeOutAnimation;

    private int viewHeightForClosingControlsDrawer;
    private boolean viewsInitialized;

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
    private RecorderStateMachine stateMachine;
    private FrameLayout recPreviewContainer;

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
        stateMachine = new RecorderStateMachine();
        stateMachine.handleCurrentState();
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
    protected void onPause() {
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

    private void initCameraPreview() {
        // We will show the video feed of the camera
        // on a preview texture in the background.
        recPreviewContainer = (FrameLayout)findViewById(R.id.preview_container);

        recoderView.postDelayed(new Runnable() {
            @Override
            public void run() {
                CameraManager cm = CameraManager.sh();
                cm.startCameraPreviewInView(RecorderActivity.this, recPreviewContainer);

                // After initialized, fade in the camera by fading out the "curtains" slowly
                Animation removeCurtainsAnim = AnimationUtils.loadAnimation(RecorderActivity.this, R.anim.animation_fadeout);
                removeCurtainsAnim.setDuration(1500);
                aq.id(R.id.recorderCurtains).animate(removeCurtainsAnim);

            }
        }, 800);

        // Tell the camera manager to prepare in the background.
//        new AsyncTask<Void, Void, Void>(){
//            @Override
//            protected Void doInBackground(Void... params) {
//                return null;
//            }
//        }.execute();
    }


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
                case JUST_STARTED:
                    
                    break;
                default:
                    throw new RecorderException(String.format("Unimplemented recorder state %s", currentState);
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
    //endregion
}
