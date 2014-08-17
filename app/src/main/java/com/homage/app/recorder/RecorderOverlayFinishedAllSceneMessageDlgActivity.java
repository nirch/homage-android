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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.androidquery.util.Constants;
import com.homage.app.R;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;

import java.util.HashMap;

public class RecorderOverlayFinishedAllSceneMessageDlgActivity extends RecorderOverlayDlgActivity {
    String TAG = "TAG_" + getClass().getName();

    private Remake remake;
    private Story story;
    private Scene scene;

    protected ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide impact buttons
        Bundle b = getIntent().getExtras();
        String remakeOID = b.getString("remakeOID");
        int sceneID = b.getInt("sceneID");

        remake = Remake.findByOID(remakeOID);
        story = remake.getStory();
        scene = story.findScene(sceneID);

        aq.id(R.id.overlayIcon).image(R.drawable.recorder_icon_trophy);
        aq.id(R.id.bigImpactTitle).text(getResources().getString(R.string.title_great_job));
        aq.id(R.id.descriptionText).text("You nailed all scenes. Lets finish the movie");
        aq.id(R.id.actionButton).text("CREATE MOVIE");
        aq.id(R.id.actionButton).getButton().setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_create_movie,0,0,0);

        //region *** Bind to UI event handlers ***

        // Make the movie
        aq.id(R.id.actionButton).clicked(onClickedActionButton);

        // Retake button.
        aq.id(R.id.overlayRetakeSceneButton).clicked(onClickedRetakeButton);

        // Oops, no button.
        aq.id(R.id.overlaySeeOopsNoRetakeButton).clicked(onClickedOopsNoButton);

        // Confirm retake scene button.
        aq.id(R.id.overlayConfirmRetakeSceneButton).clicked(onClickedConfirmRetakeSceneButton);

        // See preview button
        aq.id(R.id.overlaySeePreviewButton).clicked(onClickedSeePreviewButton);
        aq.id(R.id.overlaySeePreview2Button).clicked(onClickedSeePreviewButton);

        //endregion
    }

    public void showAreYouSureYouWantToRetakeSceneDlg() {
        aq.id(R.id.overlayRetakeAreYouSureDlg).visibility(View.VISIBLE);
    }

    public void hideAreYouSureYouWantToRetakeSceneDlg() {
        aq.id(R.id.overlayRetakeAreYouSureDlg).visibility(View.GONE);
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
    // Pressed the CREATE MOVIE button.
    final View.OnClickListener onClickedActionButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            Resources res = getResources();
            ProgressDialog pd;
            pd = new ProgressDialog(RecorderOverlayFinishedAllSceneMessageDlgActivity.this);
            pd.setTitle(res.getString(R.string.pd_title_please_wait));
            pd.setMessage(res.getString(R.string.pd_msg_creating_movie));
            pd.setCancelable(false);
            pd.show();
            HomageServer.sh().renderRemake(remake.getOID());
            RecorderOverlayFinishedAllSceneMessageDlgActivity.this.pd = pd;
        }
    };


    //
    // Pressed the retake scene button
    final View.OnClickListener onClickedRetakeButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            showAreYouSureYouWantToRetakeSceneDlg();
            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id" , Integer.toString(scene.getSceneID()));
            HMixPanel.sh().track("RERetakeLast",props);
        }
    };

    //
    // User regretted and pressed the "Oops, no" and doesn't want to retake the scene.
    final View.OnClickListener onClickedOopsNoButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id" , Integer.toString(scene.getSceneID()));
            HMixPanel.sh().track("REOopsNope",props);
            hideAreYouSureYouWantToRetakeSceneDlg();

        }
    };


    //
    // User confirmed she want to retake the scene.
    final View.OnClickListener onClickedConfirmRetakeSceneButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            setResult(ResultCode.RETAKE_SCENE.getValue());
            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id" , Integer.toString(scene.getSceneID()));
            HMixPanel.sh().track("YeahRetakeThisScene",props);
            finish();
            RecorderOverlayFinishedAllSceneMessageDlgActivity.this.overridePendingTransition(
                    R.anim.animation_fadeout_with_zoom,
                    R.anim.animation_fadeout_with_zoom);
        }
    };

    //
    // pressed the see preview button
    final View.OnClickListener onClickedSeePreviewButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Footage footage = remake.findFootage(scene.getSceneID());
            if (footage == null) return;
            Log.d(TAG, String.format("User want to see preview video: %s", footage.rawLocalFile));

            HashMap props = new HashMap<String,String>();
            props.put("story" , remake.getStory().name);
            props.put("remake_id" , remake.getOID());
            props.put("scene_id" , Integer.toString(scene.getSceneID()));
            HMixPanel.sh().track("RESeePreview",props);

            // Open video player.
            FullScreenVideoPlayerActivity.openFullScreenVideoForFile(
                    RecorderOverlayFinishedAllSceneMessageDlgActivity.this,
                    footage.rawLocalFile, HEvents.H_SCENE, null,
                    true
            );
        }
    };

    //endregion
}

