package com.homage.app.recorder;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.androidquery.util.Constants;
import com.homage.app.R;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.homage.networking.analytics.HMixPanel;

import java.util.HashMap;

public class RecorderOverlaySceneMessageDlgActivity extends RecorderOverlayDlgActivity {
    String TAG = "TAG_" + getClass().getName();

    private Remake remake;
    private Story story;
    private Scene scene;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // hide impact buttons
        aq.id(R.id.retakeAndPreviewButtonsContainer).visibility(Constants.INVISIBLE);

        Bundle b = getIntent().getExtras();
        String remakeOID = b.getString("remakeOID");
        int sceneID = b.getInt("sceneID");

        remake = Remake.findByOID(remakeOID);

        if (remake == null) {
            Log.e(TAG, String.format("Remake not found %s", remakeOID));
            return;
        }


        story = remake.getStory();
        scene = story.findScene(sceneID);

        String title = getResources().getString(R.string.title_scene_number);
        title = String.format(title, sceneID);

        aq.id(R.id.overlayIcon).image(R.drawable.icon_scene_description);
        aq.id(R.id.bigImpactTitle).text(title);
        aq.id(R.id.descriptionText).text(scene.context);
        aq.id(R.id.actionButton).text(R.string.button_shoot_scene);

        //region *** Bind to UI event handlers ***
        aq.id(R.id.actionButton).clicked(onClickedActionButton);

        HashMap props = new HashMap<String,String>();
        props.put("story",remake.getStory().name);
        props.put("remake_id",remakeOID);
        props.put("scene_id",Integer.toString(sceneID));
        HMixPanel.sh().track("RESceneDescriptionStart",props);
        //endregion
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        HMixPanel.sh().track("RESceneDescriptionStart",null);
    }

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // Pressed the test button (used for debugging)
    final View.OnClickListener onClickedActionButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            finish();
            RecorderOverlaySceneMessageDlgActivity.this.overridePendingTransition(
                    R.anim.animation_fadeout_with_zoom,
                    R.anim.animation_fadeout_with_zoom);
        }
    };
    //endregion
}

