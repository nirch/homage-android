package com.homage.app.user;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.app.ActionBar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

import com.androidquery.AQuery;
import com.homage.app.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TextReaderActivity extends Activity {
    String TAG = "TAG_" + getClass().getName();

    public static final String SK_TITLE_ID = "titleID";
    public static final String SK_RAW_TEXT_ID = "rawTextID";

    AQuery aq;

    int titleID;
    int rawTextID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_reader);

        aq = new AQuery(this);

        // Get settings
        Bundle b = getIntent().getExtras();
        titleID = b.getInt(SK_TITLE_ID, 0);
        rawTextID = b.getInt(SK_RAW_TEXT_ID, 0);

        // Init the action bar.
        initCustomActionBar();

        // Scrolling
        aq.id(R.id.textView).getTextView().setMovementMethod(new ScrollingMovementMethod());

        // Set the text
        if (rawTextID>0)
            aq.id(R.id.textView).text(readRawText(rawTextID));
    }

    private void initCustomActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_view);
        AQuery aq = new AQuery(actionBar.getCustomView());
        aq.id(R.id.navButton).visibility(View.GONE);
        actionBar.setHomeButtonEnabled(false);

        if (titleID>0)
            aq.id(R.id.appTitle).text(titleID);

    }


    private String readRawText(int rawTextIdentifier){
        String s;
        InputStream inputStream = getResources().openRawResource(rawTextIdentifier);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            s = "Text unavailable";
        }
        s = byteArrayOutputStream.toString();
        return s;
    }
}
