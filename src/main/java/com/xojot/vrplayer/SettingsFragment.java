package com.xojot.vrplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment {
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.preference);
        SwitchPreference switchPreference = (SwitchPreference) findPreference(getString(R.string.pref_stop_auto_pan_key));
        switchPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object obj) {
                boolean booleanValue = (Boolean) obj;
                Editor edit = SettingsFragment.this.getActivity().getSharedPreferences("Prefs", 0).edit();
                edit.putBoolean(SettingsFragment.this.getString(R.string.pref_stop_auto_pan_key), booleanValue);
                edit.apply();
                return true;
            }
        });
        SwitchPreference switchPreference2 = (SwitchPreference) findPreference(getString(R.string.pref_ask_format_open_media_key));
        switchPreference2.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object obj) {
                boolean booleanValue = (Boolean) obj;
                Editor edit = SettingsFragment.this.getActivity().getSharedPreferences("Prefs", 0).edit();
                edit.putBoolean(SettingsFragment.this.getString(R.string.pref_ask_format_open_media_key), booleanValue);
                edit.apply();
                return true;
            }
        });
        Preference findPreference = findPreference(getString(R.string.pref_app_version_key));
        findPreference.setSummary(BuildConfig.VERSION_NAME);
        findPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            int debugModeCount;

            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences sharedPreferences = SettingsFragment.this.getActivity().getSharedPreferences("Prefs", Context.MODE_PRIVATE);
                boolean z = sharedPreferences.getBoolean(SettingsFragment.this.getString(R.string.pref_app_version_key), false);
                Editor edit = sharedPreferences.edit();
                String string = SettingsFragment.this.getString(R.string.pref_app_version_key);
                if (this.debugModeCount == 5 && !z) {
                    edit.putBoolean(string, true);
                    edit.apply();
                    Toast.makeText(SettingsFragment.this.getActivity(), R.string.toast_enter_debug_mode, Toast.LENGTH_SHORT).show();
                } else if (z) {
                    edit.putBoolean(string, false);
                    edit.apply();
                    Toast.makeText(SettingsFragment.this.getActivity(), R.string.toast_exit_debug_mode, Toast.LENGTH_SHORT).show();
                    this.debugModeCount = 0;
                }
                this.debugModeCount++;
                return true;
            }
        });
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Prefs", 0);
        Editor edit = sharedPreferences.edit();
        String string = getString(R.string.pref_stop_auto_pan_key);
        boolean z = sharedPreferences.getBoolean(string, true);
        edit.putBoolean(string, z);
        edit.apply();
        switchPreference.setChecked(z);
        string = getString(R.string.pref_ask_format_open_media_key);
        boolean z2 = sharedPreferences.getBoolean(string, true);
        edit.putBoolean(string, z2);
        edit.apply();
        switchPreference2.setChecked(z2);
        string = getString(R.string.pref_app_version_key);
        edit.putBoolean(string, sharedPreferences.getBoolean(string, false));
        edit.apply();
    }
}
