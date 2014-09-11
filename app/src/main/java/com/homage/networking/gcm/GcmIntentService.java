/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.homage.networking.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.homage.app.R;
import com.homage.app.main.MainActivity;
import com.homage.app.main.SplashScreenActivity;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    static final int PushMessageTypeMovieReady = 0;
    static final int PushMessageTypeMovieFailed = 1;
    static final int PushMessageTypeNewStory = 2;
    static final int PushMessageTypeGeneralMessage = 3;


    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "TAG_GCM";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                // TODO: handle errors

            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                // TODO: handle message type deleted

            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // Post notification of received message.
                handleNotification(extras);
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void handleNotification(Bundle extras) {
        Log.d(TAG, "Got GCM message");

        //
        // Got a message. Take some info about it from the extras parameters.
        //
        Intent intent = null;
        Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String title = extras.getString("title","Homage");
        String text = extras.getString("text","Keep calm and check homage");
        int pushType = extras.getInt("type", PushMessageTypeGeneralMessage);

        //
        // We want to show a notification to the user.
        //
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSound(alertSound);

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);


        //
        // Create an intent that defines what happens when the user
        // selects the notification.
        // It also depends on the state of the application
        intent = createIntentForTheMessage(pushType, extras);

        // What to do if the user clicks the notification.
        if (intent != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    0
            );
            mBuilder.setContentIntent(contentIntent);
        }

        // Show the notification
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private Intent createIntentForTheMessage(int pushType, Bundle extras) {

        Intent intent = null;

        if (pushType == PushMessageTypeMovieReady || pushType == PushMessageTypeMovieFailed) {

        } else if (pushType == PushMessageTypeNewStory) {

        } else {
            // General message.
            intent = new Intent(this, SplashScreenActivity.class);
        }

//        ActivityManager mngr = (ActivityManager)getSystemService( ACTIVITY_SERVICE );
//        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(4);

        return intent;
    }
}
