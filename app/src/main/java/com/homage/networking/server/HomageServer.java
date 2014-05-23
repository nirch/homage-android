/**
    _________       Homage
   /   _____/ ______________  __ ___________
   \_____  \_/ __ \_  __ \  \/ // __ \_  __ \
   /        \  ___/|  | \/\   /\  ___/|  | \/
  /_______  /\___  >__|    \_/  \___  >__|
          \/     \/ By: Aviv Wolf.  \/

 A singleton class wrapping requests to Homage's web service.
 Derived from the abstract class Server

    Supported methods:
    ------------------

        - fetchStories :

 */
package com.homage.networking.server;

import android.util.Log;

public class HomageServer extends Server {
    String TAG = getClass().getName();

    //region *** singleton pattern ***
    private static HomageServer instance = new HomageServer();
    public static HomageServer sharedInstance() {
        if(instance == null) instance = new HomageServer();
        return instance;
    }
    public static HomageServer sh() {
        return HomageServer.sharedInstance();
    }
    //endregion

    // =======
    // Stories
    // =======
    public void refetchStories() {
        Log.d(TAG, "Refetching stories");

    }


}
