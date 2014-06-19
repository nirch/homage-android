package com.homage.app.user;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.views.ActivityHelper;

public class LoginActivity extends Activity {
    String TAG = "TAG_"+getClass().getName();

    AQuery aq;

    //region *** Lifecycle ***
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make this activity, full screen with no title bar.
        ActivityHelper.goFullScreen(this);

        setContentView(R.layout.activity_login);

        aq = new AQuery(this);

        //region *** Bind to UI event handlers ***
        /**********************************/
        /** Binding to UI event handlers **/
        /**********************************/
        aq.id(R.id.loginButton).clicked(onClickedLoginButton);
        aq.id(R.id.loginCancelButton).clicked(onClickedCancelButton);
        //endregion
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
            ActivityHelper.unimplementedMessage(LoginActivity.this);
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
