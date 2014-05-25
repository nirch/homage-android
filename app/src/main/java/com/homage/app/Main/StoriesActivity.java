/**
       Homage.
     ,         ,
     |\\\\ ////|
     | \\\V/// |
     |  |~~~|  |
     |  |===|  |
     |  |   |  |
     |  |   |  |
      \ |   | /
       \|===|/
        '---'
   Stories Activity
 */
package com.homage.app.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.recorder.RecorderActivity;
import com.homage.app.recorder.RecorderOverlayDlgActivity;
import com.homage.model.Remake;
import com.homage.model.Story;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;

import java.util.List;

public class StoriesActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();
    LayoutInflater inflater;
    List<Story> stories;
    ListView storiesListView;
    AQuery aq;
    ProgressDialog pd;

    BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return stories.size();
        }

        @Override
        public Object getItem(int i) {
            return stories.get(i);
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
            if (rowView == null) rowView = inflater.inflate(R.layout.list_row_story, storiesListView, false);
            Story story = (Story)getItem(i);
            AQuery aq = new AQuery(rowView);
            aq.id(R.id.storyName).text(story.name);
            return rowView;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

    };

    //region *** ACTIVITY LIFECYCLE ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // AQeury
        aq = new AQuery(this);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the stories activity.");

        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Set the content layout
        setContentView(R.layout.activity_main_stories);
        //endregion

        // Set the list adapter for the stories list view.
        stories = Story.listAll(Story.class);
        storiesListView = (ListView)findViewById(R.id.storiesListView);
        storiesListView.setAdapter(adapter);

        // Refetch the stories in the background.
        HomageServer.sh().refetchStories();

        //region *** Bind to UI event handlers ***
        aq.id(R.id.storiesListView).itemClicked(onItemClicked);
        //endregion
    }

    @Override
    protected void onResume() {
        super.onResume();
        initObservers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeObservers();
    }
    //endregion


    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onStoriesUpdated, new IntentFilter(HomageServer.INTENT_STORIES));
        lbm.registerReceiver(onRemakeCreation, new IntentFilter(HomageServer.INTENT_REMAKE_CREATION));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onStoriesUpdated);
        lbm.unregisterReceiver(onRemakeCreation);
    }

    // Observers handlers
    private BroadcastReceiver onStoriesUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        stories = Story.listAll(Story.class);
        aq.id(R.id.loadingStoriesProgress).getProgressBar().setVisibility(View.INVISIBLE);
        adapter.notifyDataSetChanged();
        }
    };

    private BroadcastReceiver onRemakeCreation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pd.dismiss();
            boolean success = intent.getBooleanExtra(Server.SR_SUCCESS, false);
            if (success) {
                Intent myIntent = new Intent(StoriesActivity.this, RecorderActivity.class);
                StoriesActivity.this.startActivity(myIntent);
                overridePendingTransition(R.anim.animation_fadein_with_zoom, R.anim.animation_fadeout_with_zoom);
            }
        }
    };


    // Remakes
    private void remakeStoryAtIndex(int index) {
        Resources res = getResources();
        User user = User.getCurrent();
        Story story = stories.get(index);
        Remake unFinishedRemake = user.unfinishedRemakeForStory(story);
        if (unFinishedRemake == null) {
            // No unfinished remakes.
            // Show a please wait dlg and tell server to create a new remake.
            pd = new ProgressDialog(this);
            pd.setTitle(res.getString(R.string.pd_title_please_wait));
            pd.setMessage(res.getString(R.string.pd_msg_preparing_remake));
            pd.setCancelable(true);
            pd.show();

            // Send the request to the server.
            HomageServer.sh().createRemake(story.getOID(), user.getOID());
            return;
        }

        //Story story = stories.get(index);
        //HomageServer.sh().createRemake(story.getOID(), User.current().getOID());
    }

    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------
    private AdapterView.OnItemClickListener onItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // Remake the story
            remakeStoryAtIndex(i);
        }
    };
    //endregion
}

