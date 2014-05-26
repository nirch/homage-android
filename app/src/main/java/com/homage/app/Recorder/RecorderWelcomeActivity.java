package com.homage.app.recorder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.homage.app.R;

public class RecorderWelcomeActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the welcome dialogue.");

        // Set the content layout
        setContentView(R.layout.activity_welcome_overlay_dlg);
        //endregion

        //region *** Bind to UI event handlers ***
        ((Button)findViewById(R.id.dismissButton)).setOnClickListener(onClickedDismissButton);
        //endregion
    }


    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
    View.OnClickListener onClickedDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
            overridePendingTransition(R.anim.animation_fadein_with_zoom, R.anim.animation_fadeout_with_zoom);
        }
    };
    //endregion
}
