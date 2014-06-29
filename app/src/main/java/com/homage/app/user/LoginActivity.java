package com.homage.app.user;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.app.recorder.RecorderActivity;
import com.homage.networking.server.HomageServer;
import com.homage.networking.server.Server;
import com.homage.views.ActivityHelper;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();

    public static final String SK_ALLOW_GUEST_LOGIN = "allowGuestLogin";

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);


    AQuery aq;
    ProgressDialog pd;

    boolean allowGuestLogin = false;

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

        // More settings
        Bundle b = getIntent().getExtras();
        if (b!=null) {
            allowGuestLogin = b.getBoolean(SK_ALLOW_GUEST_LOGIN, false);
        }

        if (allowGuestLogin) {
            aq.id(R.id.loginCancelButton).text(R.string.login_guest_button);
        }

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.loginButton).clicked(onClickedLoginButton);
        aq.id(R.id.loginCancelButton).clicked(onClickedCancelButton);
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
        lbm.registerReceiver(onUserLogin, new IntentFilter(HomageServer.INTENT_USER_CREATION));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onUserLogin);
    }

    // Observers handlers
    private BroadcastReceiver onUserLogin = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pd.dismiss();
            Bundle b = intent.getExtras();
            boolean success = b.getBoolean("success", false);
            if (success) {
                Toast.makeText(LoginActivity.this, "Logged in user.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Login failed.", Toast.LENGTH_LONG).show();
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
        pd.setCancelable(true);
        pd.show();

        // Store preferred login email
        SharedPreferences p = HomageApplication.getSettings(this);
        SharedPreferences.Editor editor = p.edit();

        editor.putString(HomageApplication.SK_EMAIL, email);

        // TODO: encrypt passwords.
        editor.putString(HomageApplication.SK_PASSWORD, password);

        editor.commit();

        // Login user request
        HomageServer.sh().loginUser(email, password);
    }

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
                login(user, password);
            }
        }
    };

    private View.OnClickListener onClickedCancelButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LoginActivity.this.finish();
        }
    };

    //endregion
}
