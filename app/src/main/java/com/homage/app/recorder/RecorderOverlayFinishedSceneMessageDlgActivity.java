package com.homage.app.recorder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.androidquery.util.Constants;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.analytics.HMixPanel;

import java.util.HashMap;

public class RecorderOverlayFinishedSceneMessageDlgActivity extends RecorderOverlayDlgActivity {
    String TAG = "TAG_" + getClass().getName();

    private Remake remake;
    private Story story;
    private Scene scene;
    int nextSceneID;
    private Scene nextScene;

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
        nextSceneID = remake.nextReadyForFirstRetakeSceneID();
        nextScene = story.findScene(nextSceneID);

        String nextSceneText = getResources().getString(R.string.at_the_next_scene_text);
        nextSceneText = String.format(nextSceneText, nextScene.context);

        aq.id(R.id.overlayIcon).image(R.drawable.recorder_icon_trophy);
        aq.id(R.id.bigImpactTitle).text(getResources().getString(R.string.title_great_job));
        aq.id(R.id.descriptionText).text(nextSceneText);
        aq.id(R.id.actionButton).text(R.string.button_shoot_scene);
        aq.id(R.id.actionButton).getButton().setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_next_scene,0,0,0);


        //region *** Bind to UI event handlers ***

        // Shoot the next scene.
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

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // Pressed the retake scene button
    final View.OnClickListener onClickedRetakeButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            showAreYouSureYouWantToRetakeSceneDlg();
        }
    };

    //
    // User regretted and pressed the "Oops, no" and doesn't want to retake the scene.
    final View.OnClickListener onClickedOopsNoButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            hideAreYouSureYouWantToRetakeSceneDlg();
        }
    };


    //
    // User confirmed she want to retake the scene.
    final View.OnClickListener onClickedConfirmRetakeSceneButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            setResult(ResultCode.RETAKE_SCENE.getValue());
            finish();
            RecorderOverlayFinishedSceneMessageDlgActivity.this.overridePendingTransition(
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
                    RecorderOverlayFinishedSceneMessageDlgActivity.this,
                    footage.rawLocalFile, HEvents.H_SCENE, null, HomageApplication.HM_RECORDER_PREVIEW,
                    true
            );
        }
    };

    //
    // Pressed the action button
    final View.OnClickListener onClickedActionButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            setResult(ResultCode.NEXT_SCENE.getValue());

            HashMap props = new HashMap<String,String>();
            props.put("story",remake.getStory().name);
            props.put("remake_id",remake.getOID());
            props.put("scene_id",Integer.toString(scene.getSceneID()));
            String eventName = String.format("REFinishedScene%d",scene.getSceneID());
            HMixPanel.sh().track(eventName,props);

            finish();
            RecorderOverlayFinishedSceneMessageDlgActivity.this.overridePendingTransition(
                    R.anim.animation_fadeout_with_zoom,
                    R.anim.animation_fadeout_with_zoom);
        }
    };
    //endregion
}

