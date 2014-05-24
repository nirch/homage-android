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
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.media.camera.CameraManager;
import com.homage.views.ActivityHelper;


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
    private View controlsDrawer;
    private int viewHeightForClosingControlsDrawer;
    private boolean viewsInitialized;

    private enum State {
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

    private State currentState;

    // For showing the camera preview on screen.
    private FrameLayout recPreviewContainer;
    private OrientationEventListener onOrientationChanged;

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

        // Set the content layout
        setContentView(R.layout.activity_recorder);
        controlsDrawer = aq.id(R.id.recorderControlsDrawer).getView();
        //endregion

        //region *** Camera video preview preparation ***
        if (recPreviewContainer==null) {
            // We will show the video feed of the camera
            // on a preview texture in the background.
            recPreviewContainer = (FrameLayout)findViewById(R.id.preview_container);

            // Tell the camera manager to prepare in the background.
            CameraManager cm = CameraManager.sh();
            cm.startCameraPreviewInView(this, recPreviewContainer);
        }
        //endregion

        //region *** State initialization ***
        currentState = State.JUST_STARTED;
        handleCurrentState();
        //endregion

        //region *** Bind to UI event handlers ***
        // aq.id(R.id.recorderTestButton).clicked(onUIClickedTestButton);
        controlsDrawer.setOnTouchListener(onDraggingControlsDrawer);
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

    //region Views Initilizations
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

        closeControlsDrawer(false);

    }
    //endregion

    //region Recorder's state machine.
    private void handleCurrentState() {


    }
    //endregion

    //region *** Controls Drawer **
    private boolean isControlsDrawerOpen() {
        return controlsDrawer.getTranslationY() == 0;
    }

    private void closeControlsDrawer(boolean animated) {
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
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            controlsDrawer.startAnimation(anim);
        } else {
            controlsDrawer.setTranslationY(viewHeightForClosingControlsDrawer);
        }
    }

    private void openControlsDrawer(boolean animated) {
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
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            controlsDrawer.startAnimation(anim);
        } else {
            controlsDrawer.setTranslationY(0);
        }
    }

    private void toggleControlsDrawer(boolean animated) {
        if (isControlsDrawerOpen()) {
            closeControlsDrawer(animated);
        } else {
            openControlsDrawer(animated);
        }
    }
    //endregion

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // Pressed the test button (used for debugging)
    final View.OnClickListener onUIClickedTestButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            toggleControlsDrawer(true);
//            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayDlgActivity.class);
//            RecorderActivity.this.startActivity(myIntent);
//            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }
    };


//    private final View.OnDragListener onDraggingControlsDrawer = new View.OnDragListener() {
//        @Override
//        public boolean onDrag(View view, DragEvent dragEvent) {
//            Log.d(TAG, "XXX");
//            return false;
//        }
//    };


    private final View.OnTouchListener onDraggingControlsDrawer = new View.OnTouchListener() {
        float startPosTouch, deltaTouch, startPosView, newPosView;

        @Override
        public boolean onTouch(final View v,final MotionEvent event)
        {
            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                {
                    startPosTouch = event.getRawY();
                    startPosView = controlsDrawer.getTranslationY();
                    return true;
                }
                case MotionEvent.ACTION_MOVE:
                {
                    deltaTouch = startPosTouch - event.getRawY();
                    newPosView = startPosView - deltaTouch;
                    if (newPosView < 0) newPosView = 0;
                    controlsDrawer.setTranslationY(newPosView);
                    return true;
                }
                case MotionEvent.ACTION_UP:
                {
                    deltaTouch = startPosTouch - event.getRawY();
                    newPosView = startPosView - deltaTouch;
                    Log.d(TAG, String.format(">> %f", newPosView));

                    if (newPosView < viewHeightForClosingControlsDrawer / 2) {
                        openControlsDrawer(true);
                    } else {
                        closeControlsDrawer(true);
                    }


                    return true;
                }
            }
            return false;
        }
     };

    //endregion

}
