package com.homage.app.user;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.facebook.*;
import com.facebook.model.*;
import com.facebook.widget.LoginButton;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;
import com.homage.app.main.WelcomeScreenActivity;
import com.homage.model.User;
import com.homage.networking.analytics.HMixPanel;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;
import com.homage.views.ActivityHelper;

import org.bson.types.ObjectId;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();

    public static final String SK_ALLOW_GUEST_LOGIN = "allowGuestLogin";
    public static final String SK_START_MAIN_ACTIVITY_AFTER_LOGIN = "startMainActivityAfterLogin";

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public final static int HMFaceBookConnect       = 0;
    public final static int HMMailConnect           = 1;
    public final static int HMGuestConnect          = 2;
    public final static int HMTwitterConnect        = 3;
    public final static int HMSameConnect = 4;


    AQuery aq;
    ProgressDialog pd;

    boolean allowGuestLogin = false;
    boolean startMainActivityAfterLogin = false;

    int login_method;

    //region *** Lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        setContentView(R.layout.activity_login);

        aq = new AQuery(this);

        // Set default email field
        SharedPreferences p = HomageApplication.getSettings(this);
        String email = p.getString(HomageApplication.SK_EMAIL, "").trim();
        aq.id(R.id.loginMail).text(email);

        //
        // More settings
        //
        Bundle b = getIntent().getExtras();
        if (b!=null) {
            // Flag that indicates if this screen allows to login as a guest user
            // (if not, button will be cancel button)
            allowGuestLogin = b.getBoolean(SK_ALLOW_GUEST_LOGIN, false);

            // Flash that indicates if after login, the main activity (or welcome screen)
            // should be opened.
            startMainActivityAfterLogin = b.getBoolean(SK_START_MAIN_ACTIVITY_AFTER_LOGIN, false);
        }

        if (allowGuestLogin) {
            aq.id(R.id.loginCancelButton).text(R.string.login_guest_button);
        }

        //
        // Facebook
        //

        // By default, be logged out of facebook when opening the login screen.
        Session session = Session.getActiveSession();
        if (session != null) session.closeAndClearTokenInformation();
        LoginButton fbLoginButton = (LoginButton)findViewById(R.id.facebookLoginButton);
        fbLoginButton.setVisibility(View.GONE);
        // Initialize the button (after waiting for a while)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initFaceLoginButton();
            }
        }, 800);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.loginButton).clicked(onClickedLoginButton);
        aq.id(R.id.loginCancelButton).clicked(onClickedCanceOrGuestButton);
        aq.id(R.id.termsOfServiceButton).clicked(onClickedTOSButton);
        aq.id(R.id.privacyPolicyButton).clicked(onClickedPrivacyPolicyButton);
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

    //region *** Observers ***
    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onUserCreation, new IntentFilter(HomageServer.INTENT_USER_CREATION));
        lbm.registerReceiver(onUserUpdate, new IntentFilter(HomageServer.INTENT_USER_UPDATED));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onUserCreation);
        lbm.unregisterReceiver(onUserUpdate);
    }

    private void loginAsGuest() {
        Log.d(TAG, "Will login as guest");
        Resources res = getResources();
        pd = new ProgressDialog(this);
        pd.setTitle(res.getString(R.string.login_pd_title));
        pd.setMessage(res.getString(R.string.login_pd_signing_in_guest));
        pd.setCancelable(false);
        pd.show();
        HomageServer.sh().createGuest();
    }

    private boolean shouldExcludethisAdressFromMixpanel(String userEmail)
    {
        ArrayList<String> mailAdresses = new ArrayList<String>(Arrays.asList("aviv.israel@gmail.com", "yoavcaspin@gmail.com","nir@homage.it","tomer@homage.it","yoav@homage.it","nirh2@yahoo.com","nir.channes@gmail.com","ranpeer@gmail.com","tomer.harry@gmail.com","hiorit@gmail.com"));

        for (String mail : mailAdresses)
        {
           if (mail.equals(userEmail)) return true;
        }

        return false;
    }

    private void registerLoginAnalyticsForUser(User user) {

        String userOID = user.getOID().toString();
        HMixPanel mp = HMixPanel.sh();
        mp.identify(userOID);

        if (user.email != null) {
            HashMap props = new HashMap<String,String>();
            props.put("email",user.email);
            props.put("homage_id",user.getOID().toString());
            HMixPanel.sh().registerSuperProperties(props);
            HMixPanel.sh().setPeople(props);

            if (shouldExcludethisAdressFromMixpanel(user.email))
            {
                props = new HashMap<String,String>();
                props.put("$ignore","true");
                HMixPanel.sh().registerSuperProperties(props);
            }
        } else {
            HashMap props = new HashMap<String, String>();
            props.put("email", "guest");
            props.put("homage_id", user.getOID().toString());
        }

        if (user.getOID().equals(User.getCurrent().getOID().toString())) {
            HashMap props = new HashMap<String,String>();
            props.put("login_method" , Integer.toString(HMSameConnect));
            HMixPanel.sh().track("UserLogin",props);
            return;
        }

        HashMap props = new HashMap<String,String>();
        props.put("login_method",Integer.toString(login_method));
        if (user.isFirstUse) {
            HMixPanel.sh().track("UserSignup",props);
        } else {
            HMixPanel.sh().track("UserLogin",props);
        }
    }


    // Observers handlers
    private BroadcastReceiver onUserCreation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (pd != null) pd.dismiss();
            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);
            if (success) {
                User user = User.getCurrent();
                if (user == null) return;
                Intent startIntent;
                if (user.isGuest()) {
                    // If a guest user just logged in, show the welcome screen.
                    startIntent = new Intent(LoginActivity.this, WelcomeScreenActivity.class);
                    Toast.makeText(LoginActivity.this, "Logged in Guest.", Toast.LENGTH_LONG).show();
                } else {
                    // Go to the Main activity screen.
                    startIntent = new Intent(LoginActivity.this, MainActivity.class);
                    Toast.makeText(LoginActivity.this, String.format("Hello %s", user.getTag()), Toast.LENGTH_LONG).show();
                }

                if (HomageApplication.getInstance().currentSessionID != null)
                {
                    HomageServer.sh().updateAppInfo(user.getOID());
                    String sessionID = new ObjectId().toString();
                    String userID    = user.getOID().toString();
                    HomageServer.sh().reportSessionBegin(sessionID,userID);
                    HomageApplication.getInstance().currentSessionID = sessionID;
                }

                registerLoginAnalyticsForUser(user);

                LoginActivity.this.startActivity(startIntent);
                overridePendingTransition(0, 0);
                LoginActivity.this.finish();
            } else {
                HashMap<String, Object> responseInfo = (HashMap<String, Object>)intent.getSerializableExtra(Server.SR_RESPONSE_INFO);
                Integer statusCode = (Integer)responseInfo.get("status_code");
                if (statusCode == null) statusCode = -1;

                String message = loginErrorStringForStatusCode(statusCode);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }
    };

    private BroadcastReceiver onUserUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (pd != null) pd.dismiss();
            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);
            HashMap<String, Object> requestInfo = (HashMap<String, Object>)
                    intent.getSerializableExtra(Server.SR_REQUEST_INFO);

            if (success) {
                User user = User.getCurrent();
                if (user == null) return;

                final String previousUserID = (String)requestInfo.get("user_id");

                Intent startIntent;
                startIntent = new Intent(LoginActivity.this, MainActivity.class);
                Toast.makeText(LoginActivity.this, String.format("Hello %s", user.getTag()), Toast.LENGTH_LONG).show();

                if (!previousUserID.equals(user.getOID().toString())) {
                    HMixPanel.sh().createAliasForUser(user,previousUserID);
                    HMixPanel.sh().identify(user.getOID());

                    String sessionID = HomageApplication.getInstance().currentSessionID;
                    String userID    = user.getOID().toString();
                    HomageServer.sh().reportSessionUpdate(sessionID,userID);
                }

                if (user.email != null) {
                    HashMap props = new HashMap<String,String>();
                    props.put("email",user.email);
                    props.put("homage_id",user.getOID().toString());
                    HMixPanel.sh().registerSuperProperties(props);
                    HMixPanel.sh().setPeople(props);

                    if (shouldExcludethisAdressFromMixpanel(user.email))
                    {
                        props = new HashMap<String,String>();
                        props.put("$ignore","true");
                        HMixPanel.sh().registerSuperProperties(props);
                    }
                } else {
                    HashMap props = new HashMap<String, String>();
                    props.put("email", "unknown");
                    props.put("homage_id", user.getOID().toString());
                }

                HashMap props = new HashMap<String,String>();
                props.put("login_method",Integer.toString(login_method));
                HMixPanel.sh().track("UserUpdate",props);

                LoginActivity.this.startActivity(startIntent);
                overridePendingTransition(0, 0);
                LoginActivity.this.finish();
            } else {
                HashMap<String, Object> responseInfo = (HashMap<String, Object>)intent.getSerializableExtra(Server.SR_RESPONSE_INFO);
                Integer statusCode = (Integer)responseInfo.get("status_code");
                String message = loginErrorStringForStatusCode(statusCode);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }
    };

    //endregion

    //region *** User & Password ***
    boolean validatedUserPassword(String user, String password) {
        aq.id(R.id.textErrorMessage).text("");

        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(user);
        if (!matcher.find()) {
            aq.id(R.id.textErrorMessage).text(R.string.login_error_email_format);
            return false;
        }

        if (password.length() < 6) {
            aq.id(R.id.textErrorMessage).text(R.string.login_error_password_format);
            return false;
        }

        return true;
    }

    void login(String email, String password) {
        Resources res = getResources();
        pd = new ProgressDialog(this);
        pd.setTitle(res.getString(R.string.login_pd_title));
        pd.setMessage(res.getString(R.string.login_pd_signing_in));
        pd.setCancelable(false);
        pd.show();

        // Store preferred login email
        SharedPreferences p = HomageApplication.getSettings(this);
        SharedPreferences.Editor editor = p.edit();
        editor.putString(HomageApplication.SK_EMAIL, email);
        // editor.putString(HomageApplication.SK_PASSWORD, password); no need to save password
        editor.commit();

        // Create or update user, depending on flow.
        // If not logged in, will call the create user server method.
        User user = User.getCurrent();
        if (user == null) {
            // Not logged in. Create user with email + password.
            HomageServer.sh().createUser(email, password, null);
        } else {
            // Update user upon join
            HomageServer.sh().updateUserUponJoin(user, email, password, null);
        }
    }

    private String loginErrorStringForStatusCode(int statusCode) {
        Resources res = getResources();
        String message;
        switch (statusCode) {
            case -1: // Network IO error
                message = res.getString(R.string.login_error_server_unreachable);
                break;
            case 401:  // Unauthorized
            case 1001: // Incorrect password
                message = res.getString(R.string.login_error_authorization_failed);
                break;
            case 1004: // Existing fb user
                message = res.getString(R.string.login_error_existing_fb_user);
                break;
            default:
                message = res.getString(R.string.login_error_unknown);
                break;
        }
        return message;
    }


    //endregion

    //region *** Facebook ***
    private void initFaceLoginButton() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.homage.app", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d(TAG, String.format("MY KEY HASH: %s", Base64.encodeToString(md.digest(), Base64.DEFAULT)));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Name not found", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "No such algorithm", e);
        }

        // Set permissions
        List<String> permissions = Arrays.asList("email,user_birthday,public_profile");
        Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(LoginActivity.this, permissions);
        LoginButton loginButton = (LoginButton)findViewById(R.id.facebookLoginButton);
        loginButton.setReadPermissions(Arrays.asList("public_profile","email", "user_birthday"));
        loginButton.setVisibility(View.VISIBLE);
        aq.id(R.id.facebookLoginButton).animate(R.anim.animation_fadein_with_zoom);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Resources res = getResources();
                login_method = HMFaceBookConnect;
                pd = new ProgressDialog(LoginActivity.this);
                pd.setTitle(res.getString(R.string.login_pd_title));
                pd.setMessage(res.getString(R.string.login_pd_signing_in));
                pd.setCancelable(false);
                pd.show();
            }
        });

        loginButton.setSessionStatusCallback(new Session.StatusCallback() {
            // callback when session changes state
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (exception != null) {
                    if (pd != null) pd.dismiss();
                    Toast.makeText(LoginActivity.this, "Facebook login failed.", Toast.LENGTH_LONG).show();
                }

                if (state.isOpened()) {
                    updateFacebookUserInfo(session);
                } else if (state.isClosed()) {
                    Log.d(TAG, "Logged out from facebook.");
                }
            }
        });
    }

    private void updateFacebookUserInfo(final Session session) {
        // make request to the /me API
        Request request = Request.newMeRequest(
                session,
                new Request.GraphUserCallback() {
                    // callback after Graph API response with user object
                    @Override
                    public void onCompleted(
                            GraphUser fbUser,
                            Response response) {

                        // Got facebook user info.
                        if (fbUser != null) {
                            loginFaceBookUser(session, fbUser);
                        }

                    }
                }
        );
        Request.executeBatchAsync(request);
    }

    private void loginFaceBookUser(Session session, GraphUser fbUser) {
        /*
        Example for facebook user info
        {
            "birthday" = "06/06/19XX";
            "first_name" = Test;
            "id" = 3XXXX9952537XXXX;
            "last_name" = XXXX;
            "link" = "https://www.facebook.com/app_scoped_user_id/XXX/";
            "name" = "Gaga Baba";
        };
         */
        HashMap<String, String>fbInfo = new HashMap<String, String>();
        fbInfo.put("first_name", fbUser.getFirstName());
        fbInfo.put("id", fbUser.getId());
        fbInfo.put("last_name", fbUser.getLastName());
        fbInfo.put("link", fbUser.getLink());
        fbInfo.put("name", fbUser.getName());
        fbInfo.put("birthday", fbUser.getBirthday());

        String email = (String)fbUser.asMap().get("email");
        fbInfo.put("email", email);

        // Get current logged in user
        User localStorageUser = User.getCurrent();
        if (localStorageUser == null) {
            // Search the user by email
            localStorageUser = User.findByEmail(email);
        }

        if (localStorageUser != null) {
            // Already exists in local storage.
            HomageServer.sh().updateUserUponJoin(localStorageUser, email, null, fbInfo);
        } else {
            // Create new user with facebook info.
            HomageServer.sh().createUser(email, null, fbInfo);
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

//    private Session.StatusCallback faceBookCallback = new Session.StatusCallback() {
//        @Override
//        public void call(Session session, SessionState state, Exception exception) {
//            onSessionStateChange(session, state, exception);
//        }
//    };
//
//    private UiLifecycleHelper uiHelper;
//    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
//        if (state.isOpened()) {
//            Log.d(TAG, "Logged in to facebook.");
//            updateFacebookUserInfo(session);
//        } else if (state.isClosed()) {
//            Log.d(TAG, "Logged out from facebook.");
//        }
//    }

    /*
    Session.openActiveSession(this, true, new Session.StatusCallback() {

        // callback when session changes state
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (state.isOpened()) {
                updateFacebookUserInfo(session);
            } else if (state.isClosed()) {
                Log.d(TAG, "Logged out...");
            }
        }
    });
    */
    //endregion

    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
    private View.OnClickListener onClickedLoginButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String user = aq.id(R.id.loginMail).getText().toString().trim();
            String password = aq.id(R.id.loginPassword).getText().toString().trim();
            if (validatedUserPassword(user, password)) {
                login_method = HMMailConnect;
                login(user, password);
            }
        }
    };

    private View.OnClickListener onClickedCanceOrGuestButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            User user = User.getCurrent();

            //
            // User not logged in.
            // Login as a guest.
            //
            if (allowGuestLogin && user == null) {
                // Pressed guest login.
                Log.d(TAG, "Pressed login as guest.");
                login_method = HMGuestConnect;
                loginAsGuest();
                return;
            }

            //
            // Login screen when already signed in as guest.
            // Pressing the cancel button will just close the login screen.
            //
            if (user != null) {
                finish();
            }
        }
    };

    private View.OnClickListener onClickedTOSButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent startIntent = new Intent(LoginActivity.this, TextReaderActivity.class);

            Bundle b = new Bundle();
            b.putInt(TextReaderActivity.SK_TITLE_ID, R.string.text_title_tos);
            b.putInt(TextReaderActivity.SK_RAW_TEXT_ID, R.raw.en_text_terms_of_service);
            startIntent.putExtras(b);

            LoginActivity.this.startActivity(startIntent);
            overridePendingTransition(0, 0);
        }
    };

    private View.OnClickListener onClickedPrivacyPolicyButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent startIntent = new Intent(LoginActivity.this, TextReaderActivity.class);

            Bundle b = new Bundle();
            b.putInt(TextReaderActivity.SK_TITLE_ID, R.string.text_title_privacy);
            b.putInt(TextReaderActivity.SK_RAW_TEXT_ID, R.raw.en_text_privacy_policy);
            startIntent.putExtras(b);

            LoginActivity.this.startActivity(startIntent);
            overridePendingTransition(0, 0);
        }
    };
    //endregion
}
