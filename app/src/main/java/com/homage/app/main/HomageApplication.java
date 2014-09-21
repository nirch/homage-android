/**

 $$\   $$\  $$$$$$\  $$\      $$\  $$$$$$\   $$$$$$\  $$$$$$$$\
 $$ |  $$ |$$  __$$\ $$$\    $$$ |$$  __$$\ $$  __$$\ $$  _____|
 $$ |  $$ |$$ /  $$ |$$$$\  $$$$ |$$ /  $$ |$$ /  \__|$$ |
 $$$$$$$$ |$$ |  $$ |$$\$$\$$ $$ |$$$$$$$$ |$$ |$$$$\ $$$$$\
 $$  __$$ |$$ |  $$ |$$ \$$$  $$ |$$  __$$ |$$ |\_$$ |$$  __|
 $$ |  $$ |$$ |  $$ |$$ |\$  /$$ |$$ |  $$ |$$ |  $$ |$$ |
 $$ |  $$ | $$$$$$  |$$ | \_/ $$ |$$ |  $$ |\$$$$$$  |$$$$$$$$\
 \__|  \__| \______/ \__|     \__|\__|  \__| \______/ \________|

                   Android Application Class
 */
package com.homage.app.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.androidquery.callback.BitmapAjaxCallback;
import com.homage.app.recorder.CameraManager;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;
import com.homage.networking.uploader.UploadManager;
import com.orm.SugarApp;

import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class HomageApplication extends SugarApp {
    String TAG = "TAG_" + getClass().getName();

    public static final String GCM_PROJECT_NUMBER = "407919209902";

    public static final String SETTINGS_NAME = "homage_settings";

    public static final String SK_EMAIL = "email";
    public static final String SK_PASSWORD = "password";

    public static final int HM_STORY_DETAILS_TAB = 0;
    public static final int HM_ME_TAB            = 1;
    public static final int HM_WELCOME_SCREEN    = 2;
    public static final int HM_HOW_TO_TAB        = 3;
    public static final int HM_RECORDER_PREVIEW  = 4;
    public static final int HM_RECORDER_MENU     = 5;

    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_REG_ON_APP_VERSION = "reg_on_app_version";

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;

    public boolean wasInBackground;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 15000;

    private static Context instance;

    public String currentSessionID;
    private String deviceId;

    private UploadManager uploadManager;
    private String startingNavigationOn;

    static public final String GCM_SENDER_ID = "414832899241";

    public HomageApplication() {
        super();
        instance = this;
        uploadManager = UploadManager.sh();
    }

    public static HomageApplication getInstance() {
        return (HomageApplication)instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Started Homage android application.");
        Log.d(TAG, String.format("App version : %d", getAppVersion(getApplicationContext())));

        registerActivityLifecycleCallbacks(new MyActivityLifecycleCallbacks());
        // Limit memory cache
        BitmapAjaxCallback.setCacheLimit(12);

        // Initialize settings.
        initSettings();

        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();

        Thread.setDefaultUncaughtExceptionHandler(new HomageUnhandledExceptionHandler());

        // Upload service
        UploadManager.sh().checkUploader();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onFootageUploadSuccess, new IntentFilter(HomageServer.INTENT_FOOTAGE_UPLOAD_SUCCESS));

    }

    private BroadcastReceiver onFootageUploadSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);
            HashMap<String, Object> requestInfo = (HashMap<String, Object>)
                    intent.getSerializableExtra(Server.SR_REQUEST_INFO);

            if (!success) {
                // Successful upload to s3, but failed to report about it to server.
                // Will attempt (max three times) to inform server again.
                // Will wait a few seconds between each re post

                Integer attemptCount = (Integer)requestInfo.get("attemptCount");
                if (attemptCount >= 3) return;

                final String takeID = (String)requestInfo.get("takeID");
                final String remakeID = (String)requestInfo.get("remakeID");
                final Integer sceneID = (Integer)requestInfo.get("sceneID");

                attemptCount++;

                Log.d(TAG, String.format(
                        "Will re post about upload success for the %d time (in 15 seconds)",
                        attemptCount
                ));

                final HashMap<String, Object> userInfo = new HashMap<String, Object>();
                userInfo.put("attemptCount", attemptCount);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        HomageServer.sh().updateFootageUploadSuccess(remakeID, sceneID, takeID, userInfo);
                    }
                }, 15000);

            }
        }
    };

    protected void initSingletons() {
        // Homage Server
        HomageServer.sh().init(this);

        // Mixpanel
        HMixPanel.sh().init(this);
    }

    protected void initSettings() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // Unchanged development settings
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean(SettingsActivity.SKIP_STORY_DETAILS, false);
        e.putBoolean(SettingsActivity.UPLOADER_ACTIVE, true);
        e.commit();
    }

    public static Context getContext() {
        return instance;
    }

    public static SharedPreferences getSettings(Context context) {
        return context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    public static String getDeviceId() {
        return getDeviceId(getContext());
    }

    public static String getDeviceId(Context context) {
        String id = getUniqueID(context);
        if (id == null)
            id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return id;
    }

    private static String getUniqueID(Context context) {

        String telephonyDeviceId = "NoTelephonyId";
        String androidDeviceId = "NoAndroidId";

        // get telephony id
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephonyDeviceId = tm.getDeviceId();
            if (telephonyDeviceId == null) {
                telephonyDeviceId = "NoTelephonyId";
            }
        } catch (Exception e) {
        }

        // get internal android device id
        try {
            androidDeviceId = android.provider.Settings.Secure.getString(context.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            if (androidDeviceId == null) {
                androidDeviceId = "NoAndroidId";
            }
        } catch (Exception e) {

        }

        // build up the uuid
        try {
            String id = getStringIntegerHexBlocks(androidDeviceId.hashCode())
                    + "-"
                    + getStringIntegerHexBlocks(telephonyDeviceId.hashCode());

            return id;
        } catch (Exception e) {
            return "0000-0000-1111-1111";
        }
    }

    public static String getStringIntegerHexBlocks(int value) {
        String result = "";
        String string = Integer.toHexString(value);

        int remain = 8 - string.length();
        char[] chars = new char[remain];
        Arrays.fill(chars, '0');
        string = new String(chars) + string;

        int count = 0;
        for (int i = string.length() - 1; i >= 0; i--) {
            count++;
            result = string.substring(i, i + 1) + result;
            if (count == 4) {
                result = "-" + result;
                count = 0;
            }
        }

        if (result.startsWith("-")) {
            result = result.substring(1, result.length());
        }

        return result;
    }

    //region *** Unhandled exceptions ***

    public String getVersionName() {
        String versionName;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "?";
        }
        return versionName;
    }


    private class HomageUnhandledExceptionHandler implements Thread.UncaughtExceptionHandler {
        /*
         * Unhandled exceptions custom handler.
         * This is used for debugging and for making sure the camera is released.
         * The app will still crash with the default expected OS behaviour.
         * IMPORTANT: Please don't add ANY error handling hacks here!
         */
        private Thread.UncaughtExceptionHandler mDefaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // Log the top of the stack trace
            try {
                StackTraceElement element = ex.getStackTrace()[0];
                Log.e(TAG, String.format("App crashed. %s %s %d", ex.getMessage(), element.getClassName(), element.getLineNumber()));
            } catch(Exception uex) {}

            //CameraManager.sh().releaseMediaRecorder();
            CameraManager.sh().releaseCamera();
            mDefaultHandler.uncaughtException(thread, ex);
        }

    }

    @Override
    public void onLowMemory(){
        //clear all memory cached images when system is in low memory
        BitmapAjaxCallback.clearCache();
    }

    private static final class MyActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {

        int activityCounter = 0;
        CountDownTimer timer;

        String TAG = "TAG_MyActivityLifecycleCallbacks";

        public void onActivityCreated(Activity activity, Bundle bundle) {
            Log.v(TAG,"onActivityCreated:" + activity.getLocalClassName());
        }

        public void onActivityDestroyed(Activity activity) {
            Log.v(TAG,"onActivityDestroyed:" + activity.getLocalClassName());

            String userID = null;
            if (User.getCurrent() != null)
            {
                userID = User.getCurrent().getOID().toString();
            }

            String sessionID = HomageApplication.getInstance().currentSessionID;

            if (activityCounter == 0 && sessionID != null && userID != null) {
                if (timer != null) timer.cancel();
                Log.v(TAG,"activity counter: " + activityCounter);
                Log.v(TAG,"probably moving to background?");
                HomageServer.sh().reportSessionEnd(sessionID,userID);
                HomageApplication.getInstance().currentSessionID = null;
            }
        }

        public void onActivityPaused(Activity activity) {
            Log.v(TAG,"onActivityPaused:" + activity.getLocalClassName());

            String userID = null;
            if (User.getCurrent() != null)
            {
                userID = User.getCurrent().getOID().toString();
            }

            String sessionID = HomageApplication.getInstance().currentSessionID;

            activityCounter -= 1;
            Log.v(TAG,"activity counter:" + activityCounter);

            if (activityCounter == 0 && sessionID != null && userID != null) {
                Log.v(TAG,"creating countdown timer to end session");
                timer = new CountDownTimer(15000,1500) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.v(TAG,"tick ToCk");
                    }

                    @Override
                    public void onFinish() {
                        Log.v(TAG,"activity counter: " + activityCounter);
                        Log.v(TAG,"probably moving to background?");
                        String userID = User.getCurrent().getOID().toString();
                        String sessionID = HomageApplication.getInstance().currentSessionID;
                        HomageServer.sh().reportSessionEnd(sessionID,userID);
                        HomageApplication.getInstance().currentSessionID = null;
                        HMixPanel.sh().track("AppMovedToBackGround",null);
                    }
                };
                timer.start();
            }
        }

        public void onActivityResumed(Activity activity) {
            Log.v(TAG,"onActivityResumed:" + activity.getLocalClassName());

            if (timer != null) {
                Log.v(TAG,"canceling countdown timer. seesion will remain active");
                timer.cancel();
            }

            String sessionID = HomageApplication.getInstance().currentSessionID;
            activityCounter += 1;
            Log.v(TAG,"activity counter:" + activityCounter);

            if (User.getCurrent()!= null && sessionID == null)
            {
                String userID = User.getCurrent().getOID().toString();
                sessionID = new ObjectId().toString();
                HomageServer.sh().reportSessionBegin(sessionID,userID);
                HomageApplication.getInstance().currentSessionID = sessionID;
            }
        }

        public void onActivitySaveInstanceState(Activity activity,
                                                Bundle outState) {
            Log.v(TAG,"onActivitySaveInstanceState:" + activity.getLocalClassName());
        }

        public void onActivityStarted(Activity activity) {
            Log.v(TAG,"onActivityStarted:" + activity.getLocalClassName());
        }

        public void onActivityStopped(Activity activity) {
            Log.v(TAG,"onActivityStopped:" + activity.getLocalClassName());
        }
    }

    public void startActivityTransitionTimer() {
        this.mActivityTransitionTimer = new Timer();
        this.mActivityTransitionTimerTask = new TimerTask() {
            public void run() {
                HomageApplication.this.wasInBackground = true;
            }
        };

        this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    public void stopActivityTransitionTimer() {
        if (this.mActivityTransitionTimerTask != null) {
            this.mActivityTransitionTimerTask.cancel();
        }

        if (this.mActivityTransitionTimer != null) {
            this.mActivityTransitionTimer.cancel();
        }

        this.wasInBackground = false;
    }
    //endregion


    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    static public void storeRegistrationId(Context context, String regId) {
        SharedPreferences sp = getSettings(context);
        SharedPreferences.Editor e = sp.edit();
        e.putInt(PROPERTY_REG_ON_APP_VERSION, HomageApplication.getInstance().getAppVersion(context));
        e.putString(PROPERTY_REG_ID, regId);
        e.commit();

        // If user already logged in, update server about the new token.
        User currentUser = User.getCurrent();
        if (currentUser != null) {
            String deviceId = HomageApplication.getDeviceId(context);
            HomageServer.sh().updatePushToken(currentUser.getOID(), deviceId, regId, null);
        }
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    static public String getRegistrationId() {
        return getRegistrationId(getContext());
    }

    static public String getRegistrationId(Context context) {
        final SharedPreferences prefs = getSettings(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i("TAG_HomageApp", "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_REG_ON_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = HomageApplication.getInstance().getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i("TAG_HomageApp", "App version changed.");
            return "";
        }
        return registrationId;
    }


    public void preferStartingNavigationOn(String startingNavigationOn) {
        this.startingNavigationOn = startingNavigationOn;
    }

    public String getStartingNavigationOn() {
        return startingNavigationOn;
    }

    public void clearStartingNavigationOn() {
        startingNavigationOn = null;
    }
}