package com.homage.app.user;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.MainActivity;
import com.homage.app.player.FullScreenVideoPlayerActivity;
import com.homage.app.player.VideoPlayerFragment;
import com.homage.app.recorder.RecorderActivity;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.uploader.UploadManager;

import java.util.List;

public class MyStoriesFragment extends Fragment {
    String TAG = "TAG_MyStoriesFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    List<Remake> remakes;
    ListView myStoriesListView;
    AQuery aq;
    ProgressDialog pd;

    static boolean createdOnce = false;

    private User user;

    public static MyStoriesFragment newInstance(int sectionNumber, User user) {
        MyStoriesFragment fragment;
        fragment = new MyStoriesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.user = user;
        Log.d(fragment.TAG, String.format("Me screen for user: %s", user.getTag()));
        return fragment;
    }

    private void initialize() {
        aq = new AQuery(rootView);

        // Set the list adapter for the stories list view.
        remakes = user.allAvailableRemakes();
        myStoriesListView = aq.id(R.id.myStoriesListView).getListView();
        myStoriesListView.setAdapter(adapter);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        //aq.id(R.id.remakesGridView).itemClicked(onItemClicked);
        //aq.id(R.id.makeYourOwnButton).clicked(onClickedMakeYourOwnButton);
        //endregion
    }

    public void refresh() {
        remakes = user.allAvailableRemakes();
        adapter.notifyDataSetChanged();
    }

    public void updateRenderProgressState() {

    }

    private void openRecorderForExistingRemake(Remake remake) {
        Log.d(TAG, "continue remake");
        Intent myIntent = new Intent(getActivity(), RecorderActivity.class);
        Bundle b = new Bundle();
        b.putString("remakeOID", remake.getOID());
        myIntent.putExtras(b);
        getActivity().startActivity(myIntent);
    }

    private void openRecorderForNewRemake(Story story) {
        Log.d(TAG, "new remake");
        MainActivity mainActivity = (MainActivity)getActivity();
        mainActivity.remakeStory(story);
    }

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
            if (rowView == null) rowView = inflater.inflate(R.layout.list_row_my_story, myStoriesListView, false);
            final Remake remake = (Remake)getItem(i);
            final Story story = remake.getStory();
            AQuery aq = new AQuery(rowView);
            aq.id(R.id.storyName).text(story.name);
            aq.id(R.id.storyImage).image(remake.thumbnailURL, true, true, 200, R.drawable.glass_dark);

            // Delete
            aq.id(R.id.myDeleteButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, String.format("my story, clicked delete: %s", remake.getOID()));
                    MainActivity mainActivity = (MainActivity)getActivity();
                    mainActivity.askUserIfWantToDeleteRemake(remake);
                }
            });

            // Share
            aq.id(R.id.myShareButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, String.format("my story, clicked share: %s", remake.getOID()));

                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("text/html");
                    sharingIntent.putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            Html.fromHtml(String.format(
                                    "<br />\n" +
                                    "Check out this video I created with #HomageApp<br />\n" +
                                    "<br />\n" +
                                    "#%s,<br />\n" +
                                    "#%sHomageApp<br /><br />\n" +
                                    "\n" +
                                    "http://play.homage.it/%s",

                                    story.name, story.name, remake.getOID()))
                    );
                    startActivity(Intent.createChooser(sharingIntent,"Share using"));
                }
            });

            // Remake
            aq.id(R.id.myResetButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, String.format("my story, clicked remake: %s", remake.getOID()));

                    switch (remake.status) {
                        case 1: // IN PROGRESS
                            // Open recorder with this remake.
                            openRecorderForExistingRemake(remake);
                            break;

                        case 4: // TIMEOUT
                            // Open recorder with this remake.
                            openRecorderForExistingRemake(remake);
                            break;

                        case 3: // DONE
                            // Open recorder with a new remake.
                            openRecorderForNewRemake(story);
                            break;

                        default:
                            Log.e(TAG, String.format("Wrong status for remake in my stories. %s", remake.getOID()));
                    }
                }
            });

            // Play
            aq.id(R.id.myPlayButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, String.format("my story, clicked play: %s", remake.getOID()));
                    Intent myIntent = new Intent(getActivity(), FullScreenVideoPlayerActivity.class);
                    Uri videoURL = Uri.parse(remake.videoURL);
                    myIntent.putExtra(VideoPlayerFragment.K_FILE_URL, videoURL.toString());
                    myIntent.putExtra(VideoPlayerFragment.K_ALLOW_TOGGLE_FULLSCREEN, false);
                    myIntent.putExtra(VideoPlayerFragment.K_FINISH_ON_COMPLETION, true);
                    startActivity(myIntent);
                }
            });

            return rowView;
        }




        @Override
        public boolean isEmpty() {
            return remakes.size() == 0;
        }
    };


    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_my_stories, container, false);
        initialize();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
        UploadManager.sh().checkUploader();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity main = (MainActivity)getActivity();
                main.refetchRemakesForCurrentUser();
            }
        }, 500);
    }
}