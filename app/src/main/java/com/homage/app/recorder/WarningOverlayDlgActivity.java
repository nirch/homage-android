package com.homage.app.recorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.androidquery.AQuery;
import com.homage.app.R;

public class WarningOverlayDlgActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();

    protected AQuery aq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aq = new AQuery(this);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the warning overlay dialogue full screen activity.");

        // Set the content layout
        setContentView(R.layout.activity_warning_overlay_dlg);
        //endregion

        // Pushed THE button! (Clicked on the Ok button)
        aq.id(R.id.OkButton).clicked(onClickedOkButton);
        // Pushed THE button! (Clicked on the record button)
        aq.id(R.id.dontShowAgainButton).clicked(onClickedDontShowAgainButton);
    }

    private View.OnClickListener onClickedOkButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent resultIntent = new Intent();
            //resultIntent.putExtra(RecorderActivity.DONT_SHOW_AGAIN_STRING, RecorderActivity.DONT_SHOW_AGAIN_STRING);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    };

    private View.OnClickListener onClickedDontShowAgainButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Intent resultIntent = new Intent();
            //resultIntent.putExtra(RecorderActivity.DONT_SHOW_AGAIN_STRING, RecorderActivity.DONT_SHOW_AGAIN_STRING);
            setResult(Activity.RESULT_CANCELED, resultIntent);
            finish();
        }
    };
}
