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
import com.homage.FileHandler.VideoHandler;
import com.homage.app.R;
import com.homage.app.Utils.constants;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.main.SplashScreenActivity;
import com.homage.networking.server.HomageServer;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int PushMessageTypeMovieReady = 0;
    public static final int PushMessageTypeMovieFailed = 1;
    static final int PushMessageTypeNewStory = 2;
    static final int PushMessageTypeGeneralMessage = 3;


    public static int NOTIFICATION_ID = 1;
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
        Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String title = extras.getString("title","Homage");
        String text = extras.getString("text","Keep calm and check homage");
        String pushTypeString = extras.getString("type", "3");
        int pushType = Integer.parseInt(pushTypeString);

        //
        // We want to show a notification to the user.
        //
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSound(alertSound)
                        .setAutoCancel(true);

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);



        Intent intent = null;
        Intent mainIntent = null;

        /*
            Example:
            {:
                type=>0, :
                title=>"Video is Ready!", :
                remake_id=>"5415863ab8fef16bc5000012", :
                story_id=>"53ce9bc405f0f6e8f2000655", :
                text=>"Your Street Fighter Video is Ready!"
            }
         */

        Context context = getApplicationContext();
        Bundle moreInfo = new Bundle();

        if (pushType == PushMessageTypeMovieReady || pushType == PushMessageTypeMovieFailed) {

            NOTIFICATION_ID += 1;

            String storyId = extras.getString("story_id", null);
            String remakeId = extras.getString("remake_id", null);
            intent = new Intent(context, SplashScreenActivity.class);
            mainIntent = new Intent(HomageServer.GOT_PUSH_REMAKE_SUCCESS_INTENT);

            moreInfo.putString("push_message_type", String.valueOf(pushType));
            moreInfo.putString("story_id", storyId);
            moreInfo.putString("remake_id", remakeId);
            if(pushType == PushMessageTypeMovieReady) {
                moreInfo.putString("title", context.getResources().getString(R.string.title_got_push_message));
                moreInfo.putString("text", context.getResources().getString(R.string.title_got_push_message_msg));
            }else if(pushType == PushMessageTypeMovieFailed) {
                moreInfo.putString("title", context.getResources().getString(R.string.title_got_push_message_failed));
                moreInfo.putString("text", context.getResources().getString(R.string.title_got_push_message_msg_failed));
            }
            moreInfo.putString(MainActivity.SK_START_MAIN_WITH, "MyStories");

            // Update the remake info.
            if (remakeId != null) {
                HomageServer.sh().refetchRemake(remakeId, null);
            }

            intent.putExtras(moreInfo);

            // Send an intent to be caught by main activity and refresh the ME Screen
            // This intent will pass along the status of the remake and the info about it.

            mainIntent.putExtra(constants.MORE_INFO, moreInfo);
            LocalBroadcastManager.getInstance(context).sendBroadcast(mainIntent);

        } else if (pushType == PushMessageTypeNewStory) {
            intent = new Intent(context, SplashScreenActivity.class);
        } else {
            // General message.
            intent = new Intent(context, SplashScreenActivity.class);
        }

        // What to do if the user clicks the notification.
        if (intent != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            mBuilder.setContentIntent(contentIntent);

        }

        // Show the notification
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }
}
