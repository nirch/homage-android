package com.homage.app.recorder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.androidquery.util.Constants;
import com.homage.app.R;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.networking.analytics.HEvents;
import com.homage.networking.analytics.HMixPanel;

import java.util.HashMap;

public class RecorderOverlayRetakeSceneQuestionDlgActivity extends RecorderOverlayDlgActivity {
    String TAG = "TAG_" + getClass().getName();

    private Remake remake;
    private Story story;
    private Scene scene;

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

        aq.id(R.id.overlayRetakeAreYouSureDlg).visibility(View.VISIBLE);
        aq.id(R.id.descriptionContainer).visibility(View.GONE);
        aq.id(R.id.overlayIcon).visibility(View.GONE);

        //region *** Bind to UI event handlers ***
        // Oops, no button.
        aq.id(R.id.overlaySeeOopsNoRetakeButton).clicked(onClickedOopsNoButton);

        // Confirm retake scene button.
        aq.id(R.id.overlayConfirmRetakeSceneButton).clicked(onClickedConfirmRetakeSceneButton);

        // See preview button
        aq.id(R.id.overlaySeePreview2Button).clicked(onClickedSeePreviewButton);
        //endregion
    }

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // User regretted and pressed the "Oops, no" and doesn't want to retake the scene.
    final View.OnClickListener onClickedOopsNoButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            setResult(ResultCode.NOP.getValue());
            finish();
        }
    };


    //
    // User confirmed she want to retake the scene.
    final View.OnClickListener onClickedConfirmRetakeSceneButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            Intent data = new Intent();
            data.putExtra("sceneID",scene.getSceneID());
            setResult(ResultCode.RETAKE_SCENE.getValue(), data);
            finish();
//            RecorderOverlayRetakeSceneQuestionDlgActivity.this.overridePendingTransition(
//                    R.anim.animation_fadeout_with_zoom,
//                    R.anim.animation_fadeout_with_zoom);
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
                    RecorderOverlayRetakeSceneQuestionDlgActivity.this,
                    footage.rawLocalFile, HEvents.H_SCENE, null,
                    true
            );
        }
    };
    //endregion
}

