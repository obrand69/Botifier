package com.github.grimpy.botifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NotificationCompat;
import android.text.InputType;
import android.widget.EditText;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
    private SharedPreferences mSharedPref;

    private static String[] META = {"metadata_artist", "metadata_album", "metadata_title", "tts_value"};
    private List<String> mFields;
    private List<String> mValues;
    private Map<String,String> mPrefCache = new HashMap<String,String>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mSharedPref != null) {
            mSharedPref.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFields = Arrays.asList(getResources().getStringArray(R.array.metadata_fields));
        mValues = Arrays.asList(getResources().getStringArray(R.array.metadata_fields_values));


        addPreferencesFromResource(R.xml.botifier_preference);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        for (String prefkey : META) {
            String value = mSharedPref.getString(prefkey, "");
            setSummary(prefkey, value);
        }
        setTTSVisiblity();
    }

    private void setSummary(String prefkey, String value) {
        Preference pref = findPreference(prefkey);
        mPrefCache.put(prefkey, value);
        int idx = mValues.indexOf(value);
        if (idx >= 0) {
            value = mFields.get(idx);
        }
        pref.setSummary(value.replace("%", "%%"));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (!isAdded()) {
            return false;
        }
        String prefkey = preference.getKey();
        if (prefkey != null) {

            if (prefkey.equals(getString(R.string.action_makenotification)) ) {
                NotificationManager nManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder ncomp = new NotificationCompat.Builder(getActivity());
                ncomp.setContentTitle("My Notification");
                ncomp.setContentText(String.format("%s", new java.util.Date().getSeconds()));
                ncomp.setTicker("Botifier ticker test");
                ncomp.setSmallIcon(R.drawable.ic_launcher);
                ncomp.setAutoCancel(true);
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                ncomp.setSound(alarmSound);
                nManager.notify((int)System.currentTimeMillis(),ncomp.build());

            } else if (prefkey.equals(getString(R.string.action_blacklist))) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new BlackListFragment()).addToBackStack(null)
                        .commit();
            } else if (prefkey.equals(getString(R.string.action_filter_applications))) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new ApplicationFilterFragment()).addToBackStack(null)
                        .commit();
            }
        }
        return true;
    }


    private void setTTSVisiblity() {
        boolean tts = mSharedPref.getBoolean(getString(R.string.pref_tts_enabled), false);
        Preference pref = findPreference(getString(R.string.pref_tts_value));
        pref.setEnabled(tts);
        pref = findPreference(getString(R.string.pref_tts_bt_only));
        pref.setEnabled(tts);
        pref = findPreference(getString(R.string.pref_no_timeout));
        pref.setEnabled(tts);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        if (key.equals(getString(R.string.pref_blacklist)) || key.equals(getString(R.string.pref_tts_bt_only))) {
            return;
        } else if (key.equals(getString(R.string.pref_tts_enabled))) {
            setTTSVisiblity();
            return;
        }
        if (Arrays.asList(META).contains(key)) {
            String msg = sharedPreferences.getString(key, "");
            if (msg.equals("custom")) {
                final EditText input = new EditText(getActivity());
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(mPrefCache.get(key));
                input.selectAll();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(input);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String setting = input.getText().toString();
                        sharedPreferences.edit().putString(key, setting).apply();
                    }
                });
                builder.setTitle(R.string.custom_title);
                builder.setMessage(R.string.custom_description);
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                setSummary(key, msg);
            }

        }
    }
}
