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
package com.homage.app.Main;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.homage.app.R;
import com.homage.networking.server.HomageServer;

public class StoriesActivity extends Activity {
    String TAG = getClass().getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region *** Layout initializations ***
        Log.d(TAG, "Started the stories activity.");

        // Set the content layout
        setContentView(R.layout.activity_main_stories);
        //endregion

        // Refetch the stories in the background.
        HomageServer.sh().refetchStories();
    }
}
