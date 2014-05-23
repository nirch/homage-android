package com.homage.app.Recorder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.homage.app.R;

public class RecorderOverlayDlgActivity extends Activity {
    String TAG = getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the overlay dialogue full screen activity.");

        // Make this activity, full screen with no title bar.
        //ActivityHelper.goFullScreen(this);

        // Set the content layout
        setContentView(R.layout.activity_recorder_overlay_dlg);
        //endregion

        //region *** Bind to UI event handlers ***
        ((Button)findViewById(R.id.dismissButton)).setOnClickListener(onUIClickedDismissButton);
        //endregion



    }



    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // Pressed the test button (used for debugging)
    final View.OnClickListener onUIClickedDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            finish();
            RecorderOverlayDlgActivity.this.overridePendingTransition(R.anim.animation_fadeout, R.anim.animation_fadeout);
        }
    };


    //endregion
}
