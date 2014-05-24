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
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

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
    String TAG = "TAG_"+getClass().getName();
    AQuery aq;
    View controlsDrawer;
    int viewHeightForClosingCotnrolsDrawer;
    boolean viewsInitialized;

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
    };
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
        aq.id(R.id.recorderTestButton).clicked(onUIClickedTestButton);
        controlsDrawer.setOnDragListener(onDraggingControlsDrawer);
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
        viewHeightForClosingCotnrolsDrawer = controlsDrawer.getHeight();

        // But we will still want to see the record buttons and the short details
        // panel at the bottom of the screen, so we will take them into account.
        viewHeightForClosingCotnrolsDrawer -= aq.id(R.id.recorderShortDetailsContainer).getView().getHeight();
        viewHeightForClosingCotnrolsDrawer -= aq.id(R.id.recorderRecordButton).getView().getHeight()/2;

        closeContolsDrawer(false);

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

    private void closeContolsDrawer(boolean animated) {
        controlsDrawer.setTranslationY(viewHeightForClosingCotnrolsDrawer);
    }

    private void openControlsDrawer(boolean animated) {
        controlsDrawer.setTranslationY(0);
    }

    private void toggleControlsDrawer(boolean animated) {
        if (isControlsDrawerOpen()) {
            closeContolsDrawer(animated);
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
            toggleControlsDrawer(false);



//            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayDlgActivity.class);
//            RecorderActivity.this.startActivity(myIntent);
//            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }
    };


    final View.OnDragListener onDraggingControlsDrawer = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            Log.d(TAG, "XXX");
            return false;
        }
    };

    //endregion

}
