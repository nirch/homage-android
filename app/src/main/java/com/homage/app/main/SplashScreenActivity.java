package com.homage.app.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.user.LoginActivity;
import com.homage.networking.server.HomageServer;

public class SplashScreenActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 400;

    AQuery aq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        aq = new AQuery(this);

        final boolean needsLoginScreen = false;
        final boolean debugLogin = true;

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                if (needsLoginScreen) {
                    Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                    intent.putExtra(LoginActivity.SK_ALLOW_GUEST_LOGIN, true);
                    SplashScreenActivity.this.startActivity(intent);
                    SplashScreenActivity.this.finish();
                } else {
                    if (debugLogin) {
                        HomageServer.sh().loginUser("testandroid@gmail.com","123456");
                    }

                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    SplashScreenActivity.this.startActivity(intent);
                    SplashScreenActivity.this.finish();
                }
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}