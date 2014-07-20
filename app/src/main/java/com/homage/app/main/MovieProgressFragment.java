package com.homage.app.main;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amazonaws.services.s3.transfer.Upload;
import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.model.Footage;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;
import com.homage.networking.uploader.UploadManager;
import com.homage.networking.uploader.UploaderService;

import java.util.HashMap;

public class MovieProgressFragment extends Fragment {
    String TAG = "TAG_MovieProgressFragment";

    View rootView;
    AQuery aq;
    Remake remake;
    Story story;
    String followingRemakeOID;
    String preparingText;
    boolean finished;
    boolean needToContinueOnResume;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_make_movie_progress, container, false);
        rootView.setVisibility(View.GONE);
        aq = new AQuery(rootView);
        needToContinueOnResume = false;

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.movieProgressDismissButton).clicked(onPressedDismissButton);
        aq.id(R.id.movieProgressTouchButton).clicked(onPressedTouchButton);
        //endregion

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        initObservers();
        updateUI();
        if (followingRemakeOID != null && needToContinueOnResume) {
            if (followingRemakeOID.equals(remake.getOID())) {
                followRemakeWithOID(followingRemakeOID);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        removeObservers();
        needToContinueOnResume = true;
    }

    //region *** Observers ***
    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(onRemakeUpdated, new IntentFilter(HomageServer.INTENT_REMAKE));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(onRemakeUpdated);
    }

    // Observers handlers
    private BroadcastReceiver onRemakeUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();

            if (remake == null) return;
            if (followingRemakeOID == null) return;

            boolean success = b.getBoolean("success", false);
            if (success) {
                HashMap<String, Object> responseInfo = (HashMap<String, Object>)
                        intent.getSerializableExtra(Server.SR_RESPONSE_INFO);
                String relatedToRemakeWithOID = (String)responseInfo.get("remakeOID");
                if (relatedToRemakeWithOID == null) return;

                if (relatedToRemakeWithOID.equals(followingRemakeOID)) {
                    // Update info about the remake
                    remake = Remake.findByOID(followingRemakeOID);
               }
            }

            // Continue following, but only if the remake
            // is still in the correct status
            if (remake.status == Remake.Status.IN_PROGRESS.getValue() ||
                    remake.status == Remake.Status.RENDERING.getValue()) {
                followRemakeWithOID(followingRemakeOID);
            } else {
                Log.d(TAG, String.format(
                        "New interesting status %d for followed remake: %s",
                        remake.status,
                        remake.getOID()
                        ));
            }

            // Update the UI according to the remake info
            updateUI();
        }
    };

    //endregion
    public void showProgressForRemake(Remake remake) {
        // Stop following previous remakes.

        // Start following remake.
        finished = false;
        this.remake = remake;
        this.story = remake.getStory();
        followingRemakeOID = remake.getOID();

        // Show the progress bar
        rootView.setVisibility(View.VISIBLE);
        aq.id(R.id.movieProgressDismissButton).visibility(View.VISIBLE);
        aq.id(R.id.movieProgress).visibility(View.VISIBLE);
        Resources res = getResources();
        String f = res.getString(R.string.rendering_progress_preparing);
        preparingText = String.format(f, story.name);

        // Follow the
        followRemakeWithOID(followingRemakeOID);

        updateUI();
    }

    private void followRemakeWithOID(final String remakeOID) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (followingRemakeOID == null) return;
                if (remakeOID == null) return;
                if (!followingRemakeOID.equals(remakeOID)) return;
                HomageServer.sh().refetchRemake(remakeOID, null);
            }
        }, 5000);
    }

    private void updateUI() {
        if (remake == null) {
            stopAndHide();
            return;
        }

        if (remake.status == Remake.Status.DONE.getValue()) {

            finishedRemakeSuccess();

        } else if (
                remake.status == Remake.Status.IN_PROGRESS.getValue() ||
                remake.status == Remake.Status.RENDERING.getValue()) {

            // Still preparing the movie.
            aq.id(R.id.movieProgressTouchButton).text(preparingText);

        } else if (remake.status == Remake.Status.DELETED.getValue()) {

            stopAndHide();

        } else if (remake.status == Remake.Status.TIMEOUT.getValue()) {

            failedRemake();

        }
    }

    private void stopFollowing() {
        followingRemakeOID = null;
        aq.id(R.id.movieProgress).visibility(View.GONE);
    }

    private void finishedRemakeSuccess() {
        stopFollowing();
        aq.id(R.id.movieProgressTouchButton).text(R.string.rendering_progress_ready);
    }

    private void stopAndHide() {
        stopFollowing();
        remake = null;
        story = null;
        rootView.setVisibility(View.GONE);
    }

    private void failedRemake() {
        stopFollowing();
        aq.id(R.id.movieProgressTouchButton).text(R.string.rendering_progress_failed);
        aq.id(R.id.movieProgressDismissButton).visibility(View.GONE);
    }

    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
    View.OnClickListener onPressedDismissButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            stopAndHide();
        }
    };

    View.OnClickListener onPressedTouchButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (remake == null) {
                stopAndHide();
                return;
            }

            if (remake.status == Remake.Status.IN_PROGRESS.getValue() ||
                remake.status == Remake.Status.RENDERING.getValue()) {
                // Movie render still in progress.
                // Do nothing when the user touches the progress bar.
                return;
            }

            if (remake.status == Remake.Status.TIMEOUT.getValue()) {
                stopAndHide();
                return;
            }

            if (remake.status == Remake.Status.DONE.getValue()) {
                stopAndHide();
                MainActivity mainActivity = (MainActivity)getActivity();
                mainActivity.showMyStories();
            }
        }
    };


    //endregion
}
