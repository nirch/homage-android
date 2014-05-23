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
package com.homage.app.Recorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.homage.app.R;
import com.homage.media.CameraManager;
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
    String TAG = getClass().getName();

    // For showing the camera preview on screen.
    private FrameLayout recPreviewContainer;

    private OrientationEventListener onOrientationChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the recorder full screen activity.");

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Set the content layout
        setContentView(R.layout.activity_recorder);
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

        //region *** Bind to UI event handlers ***
        ((Button)findViewById(R.id.recorder_test_button)).setOnClickListener(onUIClickedTestButton);
        //endregion
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraManager cm = CameraManager.sh();
        cm.releaseMediaRecorder();
        cm.releaseCamera();
    }

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // Pressed the test button (used for debugging)
    final View.OnClickListener onUIClickedTestButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayDlgActivity.class);
            RecorderActivity.this.startActivity(myIntent);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }
    };
    //endregion

}
