package com.homage.app.recorder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.androidquery.util.Constants;
import com.homage.app.R;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;

import java.util.HashMap;

public class RecorderOverlayFinishedAllSceneMessageDlgActivity extends RecorderOverlayDlgActivity {
    String TAG = "TAG_" + getClass().getName();

    private Remake remake;
    private Story story;

    protected ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide impact buttons
        Bundle b = getIntent().getExtras();
        String remakeOID = b.getString("remakeOID");

        remake = Remake.findByOID(remakeOID);
        story = remake.getStory();

        aq.id(R.id.overlayIcon).image(R.drawable.recorder_icon_trophy);
        aq.id(R.id.bigImpactTitle).text(getResources().getString(R.string.title_great_job));
        aq.id(R.id.descriptionText).text("You nailed all scenes. Lets finish the movie");
        aq.id(R.id.actionButton).text("CREATE MOVIE");
        aq.id(R.id.actionButton).getButton().setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_create_movie_small,0,0,0);

        //region *** Bind to UI event handlers ***

        // Make the movie
        aq.id(R.id.actionButton).clicked(onClickedActionButton);

        // Retake button.
        aq.id(R.id.overlayRetakeSceneButton).clicked(onClickedRetakeButton);

        //endregion
    }

    @Override
    public void onResume() {
        super.onResume();
        initObservers();
    }

    @Override
    public void onPause() {
        super.onPause();
        removeObservers();
    }

    //region *** Observers ***
    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onRender, new IntentFilter(HomageServer.INTENT_RENDER));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onRender);
    }

    // Observers handlers
    private BroadcastReceiver onRender = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (pd!=null) {
                pd.dismiss();
                pd = null;
            }

            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);

            Activity activity = RecorderOverlayFinishedAllSceneMessageDlgActivity.this;
            if (success) {
                activity.setResult(
                        ResultCode.MOVIE_MARKED_BY_USER_FOR_CREATION.getValue()
                );

                finish();

                activity.overridePendingTransition(
                        R.anim.animation_fadeout_with_zoom,
                        R.anim.animation_fadeout_with_zoom);
            } else {
                Toast.makeText(activity, "Render failed.", Toast.LENGTH_LONG).show();
            }
        }
    };
    //endregion



    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // Pressed the retake scene button
    final View.OnClickListener onClickedRetakeButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            setResult(ResultCode.RETAKE_SCENE.getValue());
            finish();
            RecorderOverlayFinishedAllSceneMessageDlgActivity.this.overridePendingTransition(
                    R.anim.animation_fadeout_with_zoom,
                    R.anim.animation_fadeout_with_zoom);
        }
    };

    //
    // Pressed the test button (used for debugging)
    final View.OnClickListener onClickedActionButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            Resources res = getResources();
            ProgressDialog pd;
            pd = new ProgressDialog(RecorderOverlayFinishedAllSceneMessageDlgActivity.this);
            pd.setTitle(res.getString(R.string.pd_title_please_wait));
            pd.setMessage(res.getString(R.string.pd_msg_preparing_remake));
            pd.setCancelable(false);
            pd.show();
            HomageServer.sh().renderRemake(remake.getOID());
            RecorderOverlayFinishedAllSceneMessageDlgActivity.this.pd = pd;
        }
    };
    //endregion
}

