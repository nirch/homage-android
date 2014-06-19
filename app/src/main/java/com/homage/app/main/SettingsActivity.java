package com.homage.app.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.app.ActionBar;
import android.util.Log;
import android.view.View;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.views.ActivityHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsActivity extends PreferenceActivity {
    String TAG = "TAG_"+getClass().getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Custom actionbar layout
        initCustomActionBar();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(
                android.R.id.content,
                new SettingsFragment()
        ).commit();

    }

    private void initCustomActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_view);
        AQuery aq = new AQuery(actionBar.getCustomView());
        aq.id(R.id.navButton).visibility(View.GONE);
        aq.id(R.id.appTitle).text(R.string.settings_title);
        aq.id(R.id.appTitleIcon).image(R.drawable.settings_icon);
        actionBar.setHomeButtonEnabled(true);
    }

    public class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(HomageApplication.SETTINGS_NAME);
            addPreferencesFromResource(R.xml.settings);

            // Bind to preference click listener
            findPreference("settings_feedback").setOnPreferenceClickListener(onClickedPreference);
            findPreference("settings_export_db").setOnPreferenceClickListener(onClickedExportDatabase);
        }

        private Preference.OnPreferenceClickListener onClickedPreference = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, String.format("Clicked %s", preference.getKey()));
                return false;
            }
        };

        private Preference.OnPreferenceClickListener onClickedExportDatabase = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                File Db = new File("/data/data/com.homage.app/databases/Homage2.db");
                File file = new File(Environment.getExternalStoragePublicDirectory("Documents")+"/exported.db");
                file.setWritable(true);
                try {
                    copy(Db, file);
                    ActivityHelper.message(
                            getActivity(),
                            R.string.debug_exported_db_title,
                            R.string.debug_exported_db_description
                    );
                } catch (Exception ex) {
                    Log.d(TAG, "Failed exporting database",ex);
                    ActivityHelper.message(
                            getActivity(),
                            R.string.debug_exported_db_failed_title,
                            R.string.debug_exported_db_failed_description
                    );
                }
                return false;
            }

            private void copy(File src, File dst) throws IOException {
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dst);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        };
    }
}
