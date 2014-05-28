package com.homage.app.recorder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.androidquery.util.Constants;
import com.homage.app.R;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;

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

        //endregion
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
            setResult(ResultCode.RETAKE_SCENE.getValue());
            finish();
            RecorderOverlayFinishedSceneMessageDlgActivity.this.overridePendingTransition(
                    R.anim.animation_fadeout_with_zoom,
                    R.anim.animation_fadeout_with_zoom);
        }
    };

    //
    // Pressed the test button (used for debugging)
    final View.OnClickListener onClickedActionButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            setResult(ResultCode.NEXT_SCENE.getValue());
            finish();
            RecorderOverlayFinishedSceneMessageDlgActivity.this.overridePendingTransition(
                    R.anim.animation_fadeout_with_zoom,
                    R.anim.animation_fadeout_with_zoom);
        }
    };
    //endregion
}

