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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.homage.model.Story;
import com.homage.networking.server.HomageServer;

import java.util.List;

public class StoriesActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();
    LayoutInflater inflater;
    List<Story> stories;
    ListView storiesListView;
    AQuery aq;

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
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onStoriesUpdated);
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


    //region *** UI event handlers ***
    // -------------------
    // UI handlers.
    // -------------------
    private AdapterView.OnItemClickListener onItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Intent myIntent = new Intent(StoriesActivity.this, RecorderActivity.class);
            StoriesActivity.this.startActivity(myIntent);
            overridePendingTransition(R.anim.animation_fadein, R.anim.animation_fadeout);
        }
    };
    //endregion
}