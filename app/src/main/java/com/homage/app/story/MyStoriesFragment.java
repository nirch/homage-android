package com.homage.app.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

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

import java.io.File;
import java.util.List;

public class MyStoriesFragment extends Fragment {
    String TAG = "TAG_MyStoriesFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    List<Remake> remakes;
    ListView myStoriesListView;
    AQuery aq;

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
        remakes = user.allAvailableRemakesLatestOnTop();
        myStoriesListView = aq.id(R.id.myStoriesListView).getListView();
        myStoriesListView.setAdapter(adapter);
    }

    public void refresh() {
        if (remakes == null) {
            remakes = user.allAvailableRemakesLatestOnTop();
        } else {
            remakes.clear();
            remakes.addAll(user.allAvailableRemakesLatestOnTop());
        }

        if (remakes.size() == 0) {
            aq.id(R.id.noRemakesMessage).visibility(View.VISIBLE);
        } else {
            aq.id(R.id.noRemakesMessage).visibility(View.GONE);
        }
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

            if (i>3) {
                aq.id(R.id.storyImage).image(remake.thumbnailURL, true, true, 200, R.drawable.glass_dark, null, R.anim.animation_fadein_with_zoom);
            } else {
                aq.id(R.id.storyImage).image(remake.thumbnailURL, true, true, 200, R.drawable.glass_dark);
            }

            if (remake.status == Remake.Status.DONE.getValue()) {
                aq.id(R.id.myPlayButton).visibility(View.VISIBLE);
                aq.id(R.id.myShareButton).visibility(View.VISIBLE);
            } else {
                aq.id(R.id.myPlayButton).visibility(View.INVISIBLE);
                aq.id(R.id.myShareButton).visibility(View.INVISIBLE);
            }

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
                    User user = User.getCurrent();
                    if (user.isGuest()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        AlertDialog alert = builder.setMessage(R.string.share_signed_in_only)
                                .setTitle(R.string.join_us_title)
                                .setPositiveButton(R.string.ok_got_it, null)
                                .create();
                        alert.show();
                        return;
                    }
                    shareRemake(remake);
                }
            });

            // Remake
            aq.id(R.id.myResetButton).clicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, String.format("my story, clicked remake: %s", remake.getOID()));
                    MainActivity mainActivity = (MainActivity)getActivity();

                    switch (remake.status) {
                        case 1: // IN PROGRESS
                            // Open recorder with this remake.
                            mainActivity.askUserIfWantToContinueRemake(remake);
                            break;

                        case 4: // TIMEOUT
                            // Open recorder with this remake.
                            mainActivity.askUserIfWantToContinueRemake(remake);
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
                    FullScreenVideoPlayerActivity.openFullScreenVideoForURL(getActivity(), remake.videoURL, remake.thumbnailURL, true);
                }
            });

            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return remakes.size() == 0;
        }
    };

    //region *** Share ***
    private void shareRemake(final Remake sharedRemake) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        CharSequence items[] = new CharSequence[] {"Email Message", "Send Link (plain text)"};
        adb.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int n) {
            }
        });
        adb.setNegativeButton("Cancel", null);
        adb.setPositiveButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ListView lw = ((AlertDialog)dialog).getListView();
                Integer selectedPosition = lw.getCheckedItemPosition();
                switch (selectedPosition) {
                    case 0:
                        shareRemakeUsingEmailMessage(sharedRemake);
                        break;
                    default:
                        shareRemakeUsingPlainText(sharedRemake);

                }
            }
        });
        adb.setTitle("Share video");
        adb.show();
    }

    private void shareRemakeUsingEmailMessage(Remake sharedRemake) {
        Story story = sharedRemake.getStory();
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("message/rfc822");
        Spanned html = Html.fromHtml(
                new StringBuilder()
                        .append("<p><b>Check out this video I created with the Homage App for android</b></p>")
                        .append(String.format("<p><a href='%s'>%s</a></p>", sharedRemake.videoURL, sharedRemake.videoURL))
                        .toString());
        sharingIntent.putExtra(
                Intent.EXTRA_TEXT,
                html
        );
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(
                "Check my awesome video : %s",
                story.name));
        startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }

    private void shareRemakeUsingPlainText(Remake sharedRemake) {
        Story story = sharedRemake.getStory();
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(
                Intent.EXTRA_TEXT,
                String.format(
                        "Check out this video I created with #HomageApp\n" +
                                "\n" +
                                "#%s\n" +
                                "#%sHomageApp\n" +
                                "\n" +
                                "http://play.homage.it/%s",
                        story.name, story.name, sharedRemake.getOID()
                )
        );
        startActivity(Intent.createChooser(sharingIntent,"Share using"));
    }
    //endregion

    //region *** fragment life cycle related
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_my_stories, container, false);
        initialize();

        // Allow orientation change.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Show actionbar
        getActivity().getActionBar().show();

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