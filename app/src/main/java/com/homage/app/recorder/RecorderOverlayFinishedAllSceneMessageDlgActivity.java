package com.homage.app.recorder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.androidquery.util.Constants;
import com.homage.app.R;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;

public class RecorderOverlayFinishedAllSceneMessageDlgActivity extends RecorderOverlayDlgActivity {
    String TAG = "TAG_" + getClass().getName();

    private Remake remake;
    private Story story;

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
            // TODO: implement make movie request to server here and error handling.
            setResult(ResultCode.MOVIE_MARKED_BY_USER_FOR_CREATION.getValue());
            finish();
            RecorderOverlayFinishedAllSceneMessageDlgActivity.this.overridePendingTransition(
                    R.anim.animation_fadeout_with_zoom,
                    R.anim.animation_fadeout_with_zoom);
        }
    };
    //endregion
}

