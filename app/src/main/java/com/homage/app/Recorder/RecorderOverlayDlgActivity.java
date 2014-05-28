package com.homage.app.recorder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.androidquery.AQuery;
import com.homage.app.R;

public class RecorderOverlayDlgActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();

    protected AQuery aq;

    static public enum ResultCode {
        NOP(0),
        NEXT_SCENE(1),
        RETAKE_SCENE(2),
        ERROR(666);

        private final int value;
        private ResultCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aq = new AQuery(this);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the overlay dialogue full screen activity.");

        // Set the content layout
        setContentView(R.layout.activity_recorder_overlay_dlg);
        //endregion
    }
}
