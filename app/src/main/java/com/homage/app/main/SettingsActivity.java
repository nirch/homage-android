package com.homage.app.main;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.app.ActionBar;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.facebook.Settings;
import com.homage.app.R;
import com.homage.app.user.TextReaderActivity;
import com.homage.model.User;
import com.homage.networking.server.HomageServer;
import com.homage.views.ActivityHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class SettingsActivity extends PreferenceActivity {
    String TAG = "TAG_"+getClass().getName();

    public static final String REMAKES_ARE_PUBLIC = "settings_remakes_are_public";
    public static final String UPLOADER_ACTIVE = "settings_uploader_active";
    public static final String SKIP_STORY_DETAILS = "settings_skip_story_details";

    protected ProgressDialog pd;

    SettingsFragment sf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Custom actionbar layout
        initCustomActionBar();

        sf = new SettingsFragment();

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(
                android.R.id.content,
                sf
        ).commit();
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


    //region *** Observers ***
    private void initObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(onUserPrefUpdated, new IntentFilter(HomageServer.INTENT_USER_PREFERENCES_UPDATE));
    }

    private void removeObservers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(onUserPrefUpdated);
    }

    private BroadcastReceiver onUserPrefUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (pd != null) pd.dismiss();
            sf.updateSettingsValues();
        }
    };
    //endregion

    private void initCustomActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_view);
        AQuery aq = new AQuery(actionBar.getCustomView());
        aq.id(R.id.navButton).visibility(View.GONE);
        aq.id(R.id.appTitle).text(R.string.settings_title);
        aq.id(R.id.appTitleIcon).image(R.drawable.settings_icon);
        actionBar.setHomeButtonEnabled(false);
    }

    private void showProgress() {
        Resources res = getResources();
        pd = new ProgressDialog(this);
        pd.setTitle(res.getString(R.string.pd_title_please_wait));
        pd.setMessage(res.getString(R.string.pd_msg_deleting_remake));
        pd.setCancelable(true);
        pd.show();
    }

    public static class SettingsFragment extends PreferenceFragment {
        String TAG = "TAG_"+getClass().getName();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(HomageApplication.SETTINGS_NAME);
            addPreferencesFromResource(R.xml.settings);

            // Update the values
            updateSettingsValues();

            // Bind to preference click listener
            findPreference("settings_remakes_are_public").setOnPreferenceClickListener(onClickedTogglePublic);
            findPreference("settings_feedback").setOnPreferenceClickListener(onClickedFeedback);
            findPreference("settings_terms_of_service").setOnPreferenceClickListener(onClickedTOC);
            findPreference("settings_privacy_policy").setOnPreferenceClickListener(onClickedPrivacy);
        }


        public void updateSettingsValues() {
            User user = User.getCurrent(true);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor e = sp.edit();
            CheckBoxPreference publicPref = (CheckBoxPreference)findPreference("settings_remakes_are_public");
            if (user.isPublic) {
                e.putBoolean(SettingsActivity.REMAKES_ARE_PUBLIC, true);
                publicPref.setChecked(true);
            } else {
                e.putBoolean(SettingsActivity.REMAKES_ARE_PUBLIC, false);
                publicPref.setChecked(false);
            }
            e.commit();
        }

        //region *** UI event handlers ***
        /**
         *  ==========================
         *      UI event handlers.
         *  ==========================
         */
        private Preference.OnPreferenceClickListener onClickedTogglePublic = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, String.format("Clicked %s", preference.getKey()));
                User user = User.getCurrent();

                if (user.isGuest()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    AlertDialog alert = builder.setMessage(R.string.publish_signed_in_only)
                            .setTitle(R.string.join_us_title)
                            .setPositiveButton(R.string.ok_got_it, null)
                            .create();
                    alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            updateSettingsValues();
                        }
                    });
                    alert.show();
                    return true;
                }

                SettingsActivity act = (SettingsActivity)getActivity();
                HashMap<String, String> parameters = new HashMap<String, String>();
                parameters.put("user_id", user.getOID());

                // Toggle
                parameters.put("is_public", !user.isPublic ? "1" : "0");

                HomageServer.sh().updateUserPreferences(parameters);
                act.showProgress();

                return false;
            }
        };

        private Preference.OnPreferenceClickListener onClickedFeedback = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, String.format("Clicked %s", preference.getKey()));

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"yoav@homage.it"});
                i.putExtra(Intent.EXTRA_SUBJECT, "feedback please!");
                i.putExtra(Intent.EXTRA_TEXT   , "");
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        };

        private Preference.OnPreferenceClickListener onClickedTOC = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, String.format("Clicked %s", preference.getKey()));
                Intent startIntent = new Intent(getActivity(), TextReaderActivity.class);
                Bundle b = new Bundle();
                b.putInt(TextReaderActivity.SK_TITLE_ID, R.string.text_title_tos);
                b.putInt(TextReaderActivity.SK_RAW_TEXT_ID, R.raw.en_text_terms_of_service);
                startIntent.putExtras(b);
                startActivity(startIntent);
                return false;
            }
        };

        private Preference.OnPreferenceClickListener onClickedPrivacy = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, String.format("Clicked %s", preference.getKey()));
                Intent startIntent = new Intent(getActivity(), TextReaderActivity.class);
                Bundle b = new Bundle();
                b.putInt(TextReaderActivity.SK_TITLE_ID, R.string.text_title_privacy);
                b.putInt(TextReaderActivity.SK_RAW_TEXT_ID, R.raw.en_text_privacy_policy);
                startIntent.putExtras(b);
                startActivity(startIntent);
                return false;
            }
        };

        private Preference.OnPreferenceClickListener onClickedExportDatabase = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String storageFileName = "HomageLocalStorage.db";
                File Db = new File("/data/data/com.homage.app/databases/"+storageFileName);
                File file = new File(Environment.getExternalStoragePublicDirectory("Documents")+"/"+storageFileName);
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
        //endregion

    }
}
