package com.homage.app.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.GridView;
import android.widget.BaseAdapter;
import android.widget.AdapterView;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.MainActivity;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.app.recorder.RecorderActivity;
import com.homage.device.Device;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;
import com.homage.app.player.FullScreenVideoPlayerActivity;

import java.util.HashMap;
import java.util.List;

public class StoryDetailsFragment extends Fragment {
    public String TAG = "TAG_StoryDetailsFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    AQuery aq;
    List<Remake> remakes;
    GridView remakesGridView;

    static boolean createdOnce = false;
    public Story story;

    BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return remakes.size();
        }

        @Override
        public Object getItem(int i) {
            return remakes.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }


        @Override
        public View getView(int i, View rowView, ViewGroup viewGroup) {
            if (rowView == null)
                rowView = inflater.inflate(R.layout.list_row_remake, remakesGridView, false);
            final Remake remake = (Remake) getItem(i);
            AQuery aq = new AQuery(rowView);
            aq.id(R.id.remakeImage).image(remake.thumbnailURL, true, true, 200, R.drawable.glass_dark);

            aq.id(R.id.reportButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showReportDialogForRemake(remake.getOID().toString());
                }
            });

            aq.id(R.id.watchRemakeButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, String.format("remakeOID: %s", remake.getOID()));
                    playRemakeMovie(remake.getOID());
                }
            });

            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    };

    public static StoryDetailsFragment newInstance(int sectionNumber, Story story) {
        StoryDetailsFragment fragment;
        fragment = new StoryDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.story = story;
        Log.d(fragment.TAG, String.format("Showing story details: %s", story.name));

        return fragment;
    }

    private void initialize() {
        aq = new AQuery(rootView);
        aq.id(R.id.storyImageView).image(story.thumbnail, true, true, 200, R.drawable.glass_dark);
        aq.id(R.id.storyDescription).text(story.description);

        remakes = story.getRemakes();
        remakesGridView = aq.id(R.id.remakesGridView).getGridView();
        remakesGridView.setAdapter(adapter);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        //aq.id(R.id.remakesGridView).itemClicked(onItemClicked);
        aq.id(R.id.makeYourOwnButton).clicked(onClickedMakeYourOwnButton);
        //endregion
    }

    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_story_details, container, false);
        initialize();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity main = (MainActivity)getActivity();
                main.refetchRemakesForStory(story);
            }
        }, 50);
    }

    @Override
    public void onResume() {
        super.onResume();
        //initObservers();
    }

    @Override
    public void onPause() {
        super.onPause();
        //removeObservers();
    }

    public void refreshData() {
        // Just an example of refreshing the data from local storage,
        // after it was fetched from server and parsed.
        List<Remake> remakes = story.getRemakes();
        adapter.notifyDataSetChanged();
        Log.d(TAG, String.format("%d remakes for this story", remakes.size()));
    }
    //endregion

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------
    private AdapterView.OnItemClickListener onItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Remake remake = remakes.get(i);
            if (remake == null) return;
            Log.d(TAG, String.format("remakeOID: %s", remake.getOID()));
            playRemakeMovie(remake.getOID());
        }
    };


    //endregion


    //region video player calls
    private void playStoryMovie() {
        Intent myIntent = new Intent(this.getActivity(), FullScreenVideoPlayerActivity.class);
        Uri videoURL = Uri.parse(story.video);
        myIntent.putExtra(VideoPlayerFragment.K_FILE_URL, videoURL.toString());
        myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
        myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, true);
        startActivity(myIntent);
    }

    private void playRemakeMovie(String remakeID) {
        Intent myIntent = new Intent(this.getActivity(), FullScreenVideoPlayerActivity.class);
        Remake remake = Remake.findByOID(remakeID);
        Uri videoURL = Uri.parse(remake.videoURL);
        myIntent.putExtra(VideoPlayerFragment.K_FILE_URL, videoURL.toString());
        myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
        myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, true);
        startActivity(myIntent);
    }

    //
    final View.OnClickListener onClickedMakeYourOwnButton = new View.OnClickListener() {
        @Override
        public void onClick(View button) {
           createNewRemake();
        }
    };

    private void createNewRemake() {
        if (story == null) return;

        User user = User.getCurrent();
        if (user == null) return;

        Remake unfinishedRemake = user.unfinishedRemakeForStory(story);
        MainActivity main = (MainActivity) this.getActivity();
        if (unfinishedRemake == null) {
            // No info about an unfinished remake exists in local storage.
            // Create a new remake.
            main.sendRemakeStoryRequest(story);
        } else {
            // An unfinished remake exists. Ask user if she want to continue this remake
            // or start a new one.
            main.askUserIfWantToContinueRemake(unfinishedRemake);
        }
    }

    private void showReportDialogForRemake(final String remakeID)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle(R.string.report_abusive_remake_title);
        builder.setItems(
                new CharSequence[] {"yes" , "no"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                reportAsInappropriate(remakeID);
                                break;
                            case 1:
                                break;

                        }
                    }
                });
        builder.create().show();
    }

    private void reportAsInappropriate(String remakeID)
    {
        HomageServer.sh().reportRemakeAsInappropriate(remakeID);
    }


}
//endregion

