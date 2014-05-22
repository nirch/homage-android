/*
    $$\   $$\  $$$$$$\  $$\      $$\  $$$$$$\   $$$$$$\  $$$$$$$$\
    $$ |  $$ |$$  __$$\ $$$\    $$$ |$$  __$$\ $$  __$$\ $$  _____|
    $$ |  $$ |$$ /  $$ |$$$$\  $$$$ |$$ /  $$ |$$ /  \__|$$ |
    $$$$$$$$ |$$ |  $$ |$$\$$\$$ $$ |$$$$$$$$ |$$ |$$$$\ $$$$$\
    $$  __$$ |$$ |  $$ |$$ \$$$  $$ |$$  __$$ |$$ |\_$$ |$$  __|
    $$ |  $$ |$$ |  $$ |$$ |\$  /$$ |$$ |  $$ |$$ |  $$ |$$ |
    $$ |  $$ | $$$$$$  |$$ | \_/ $$ |$$ |  $$ |\$$$$$$  |$$$$$$$$\
    \__|  \__| \______/ \__|     \__|\__|  \__| \______/ \________|
             ____  ____  ___  __  ____  ____  ____  ____
            (  _ \(  __)/ __)/  \(  _ \(    \(  __)(  _ \
             )   / ) _)( (__(  O ))   / ) D ( ) _)  )   /
            (__\_)(____)\___)\__/(__\_)(____/(____)(__\_)

                            Activity
*/
package com.homage.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.amazonaws.services.s3.model.Bucket;
import com.homage.media.CameraHelper;
import com.homage.media.CameraManager;
import com.homage.views.ActivityHelper;

import java.io.IOException;
import java.util.List;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;


/*
 * This class consists ...
 *
 * <p>The...</p>
 *
 * @author  Homage
 * @author  Aviv Wolf
 * @since   0.1
 */
public class RecorderActivity extends Activity {
    String TAG = getClass().getName();

    // For showing the camera preview on screen.
    private TextureView recPreview;

    private AmazonS3Client s3Client = new AmazonS3Client(
            new BasicAWSCredentials("test", "test"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the recorder full screen activity.");

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        // Set the content layout
        setContentView(R.layout.activity_recorder);
        //endregion

        new S3Test().execute();

        //region *** Camera video recording preparation ***
        if (recPreview==null) {
            // We will show the video feed of the camera
            // on a preview texture in the background.
            recPreview = (TextureView)findViewById(R.id.preview_view);

            // Tell the camera manager to prepare in the background.
            CameraManager cm = CameraManager.getInstance();
            cm.prepareVideoAsync();
        }
        //endregion

        //region *** Bind to UI event handlers ***
        ((Button)findViewById(R.id.recorder_test_button)).setOnClickListener(onUIClickedTestButton);
        //endregion
    }

    private class S3Test extends AsyncTask {
        @Override
        protected Object doInBackground(Object... arg0) {
            List<Bucket> bucketsList = s3Client.listBuckets();

            Log.d(TAG, bucketsList.toString());
            return null;
        }
    }

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------

    //
    // Pressed the test button (used for debugging)
    final View.OnClickListener onUIClickedTestButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
            Intent myIntent = new Intent(RecorderActivity.this, RecorderOverlayDlgActivity.class);
            RecorderActivity.this.startActivity(myIntent);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }
    };
    //endregion

}
