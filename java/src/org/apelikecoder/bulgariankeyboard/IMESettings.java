/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apelikecoder.bulgariankeyboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import org.apelikecoder.bulgariankeyboard.R;
import org.apelikecoder.bulgariankeyboard.voice.SettingsUtil;
import org.apelikecoder.bulgariankeyboard.voice.VoiceInputLogger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.text.AutoText;
import android.util.Log;
import android.widget.Toast;

public class IMESettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        DialogInterface.OnDismissListener, Preference.OnPreferenceClickListener{

    private static final String SELECT_KEYBOARD_MODE = "select_keyboard_mode";
    
    private static final String QUICK_FIXES_KEY = "quick_fixes";
    private static final String PREDICTION_SETTINGS_KEY = "prediction_settings";
    private static final String VOICE_SETTINGS_KEY = "voice_mode";
    /* package */ static final String PREF_SETTINGS_KEY = "settings_key";

    private static final String TAG = "IMESettings";

    // Dialog ids
    private static final int VOICE_INPUT_CONFIRM_DIALOG = 0;

    private CheckBoxPreference mQuickFixes;
    private ListPreference mVoicePreference;
    private ListPreference mSettingsKeyPreference;
    private boolean mVoiceOn;

    private VoiceInputLogger mLogger;

    private boolean mOkClicked = false;
    private String mVoiceModeOff;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs);
        mQuickFixes = (CheckBoxPreference) findPreference(QUICK_FIXES_KEY);
        mVoicePreference = (ListPreference) findPreference(VOICE_SETTINGS_KEY);
        mSettingsKeyPreference = (ListPreference) findPreference(PREF_SETTINGS_KEY);
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);
        findPreference(SELECT_KEYBOARD_MODE).setOnPreferenceClickListener(this);
        mVoiceModeOff = getString(R.string.voice_mode_off);
        mVoiceOn = !(prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff).equals(mVoiceModeOff));
        mLogger = VoiceInputLogger.getLogger(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int autoTextSize = AutoText.getSize(getListView());
        if (autoTextSize < 1) {
            ((PreferenceGroup) findPreference(PREDICTION_SETTINGS_KEY))
                    .removePreference(mQuickFixes);
        }
        if (!LatinIME.VOICE_INSTALLED
                || !SpeechRecognizer.isRecognitionAvailable(this)) {
            getPreferenceScreen().removePreference(mVoicePreference);
        } else {
            updateVoiceModeSummary();
        }
        updateSettingsKeySummary();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        (new BackupManager(this)).dataChanged();
        // If turning on voice input, show dialog
        if (key.equals(VOICE_SETTINGS_KEY) && !mVoiceOn) {
            if (!prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff)
                    .equals(mVoiceModeOff)) {
                showVoiceConfirmation();
            }
        }
        mVoiceOn = !(prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff).equals(mVoiceModeOff));
        updateVoiceModeSummary();
        updateSettingsKeySummary();
    }

    private void updateSettingsKeySummary() {
        mSettingsKeyPreference.setSummary(
                getResources().getStringArray(R.array.settings_key_modes)
                [mSettingsKeyPreference.findIndexOfValue(mSettingsKeyPreference.getValue())]);
    }

    private void showVoiceConfirmation() {
        mOkClicked = false;
        showDialog(VOICE_INPUT_CONFIRM_DIALOG);
    }

    private void updateVoiceModeSummary() {
        mVoicePreference.setSummary(
                getResources().getStringArray(R.array.voice_input_modes_summary)
                [mVoicePreference.findIndexOfValue(mVoicePreference.getValue())]);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case VOICE_INPUT_CONFIRM_DIALOG:
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (whichButton == DialogInterface.BUTTON_NEGATIVE) {
                            mVoicePreference.setValue(mVoiceModeOff);
                            mLogger.settingsWarningDialogCancel();
                        } else if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                            mOkClicked = true;
                            mLogger.settingsWarningDialogOk();
                        }
                        updateVoicePreference();
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.voice_warning_title)
                        .setPositiveButton(android.R.string.ok, listener)
                        .setNegativeButton(android.R.string.cancel, listener);

                // Get the current list of supported locales and check the current locale against
                // that list, to decide whether to put a warning that voice input will not work in
                // the current language as part of the pop-up confirmation dialog.
                String supportedLocalesString = SettingsUtil.getSettingsString(
                        getContentResolver(),
                        SettingsUtil.LATIN_IME_VOICE_INPUT_SUPPORTED_LOCALES,
                        LatinIME.DEFAULT_VOICE_INPUT_SUPPORTED_LOCALES);
                ArrayList<String> voiceInputSupportedLocales =
                        LatinIME.newArrayList(supportedLocalesString.split("\\s+"));
                boolean localeSupported = voiceInputSupportedLocales.contains(
                        Locale.getDefault().toString());

                if (localeSupported) {
                    String message = getString(R.string.voice_warning_may_not_understand) + "\n\n" +
                            getString(R.string.voice_hint_dialog_message);
                    builder.setMessage(message);
                } else {
                    String message = getString(R.string.voice_warning_locale_not_supported) +
                            "\n\n" + getString(R.string.voice_warning_may_not_understand) + "\n\n" +
                            getString(R.string.voice_hint_dialog_message);
                    builder.setMessage(message);
                }

                AlertDialog dialog = builder.create();
                dialog.setOnDismissListener(this);
                mLogger.settingsWarningDialogShown();
                return dialog;
            default:
                Log.e(TAG, "unknown dialog " + id);
                return null;
        }
    }

    public void onDismiss(DialogInterface dialog) {
        mLogger.settingsWarningDialogDismissed();
        if (!mOkClicked) {
            // This assumes that onPreferenceClick gets called first, and this if the user
            // agreed after the warning, we set the mOkClicked value to true.
            mVoicePreference.setValue(mVoiceModeOff);
        }
    }

    private void updateVoicePreference() {
        boolean isChecked = !mVoicePreference.getValue().equals(mVoiceModeOff);
        if (isChecked) {
            mLogger.voiceInputSettingEnabled();
        } else {
            mLogger.voiceInputSettingDisabled();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(SELECT_KEYBOARD_MODE)) {
            List<Locale> locales = getEnabledLocales();
            final TreeMap<String, Locale> localesWithAlts = new TreeMap<String, Locale>();
            for (Locale l : locales) {
                Resources res = getLocaledResources(l);
                boolean[] alts = getAlts(l);
                if (alts[0] || alts[1])
                    localesWithAlts.put(l.getDisplayLanguage(l), l);
                restoreLocale(res);
            }
            if (localesWithAlts.isEmpty()) {
                Toast.makeText(this, R.string.no_keyboard_modes_available, Toast.LENGTH_SHORT).show();
            } else if (localesWithAlts.size() == 1) {
                showKeyboardModesDialog(localesWithAlts.get(localesWithAlts.keySet().iterator().next()));
            } else {
                new AlertDialog.Builder(this).setItems(localesWithAlts.keySet().toArray(new String[]{}), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Collection<Locale> locales = localesWithAlts.values();
                        int i = 0;
                        for (Locale l : locales) {
                            if (i++ == which)
                                showKeyboardModesDialog(l);
                        }
                    }
                }).create().show();
            }
            return true;
        }
        return false;
    }

    private void showKeyboardModesDialog(final Locale locale) {
        Resources res = getLocaledResources(locale);
        String items[] = res.getStringArray(R.array.alt_keyboards_entries);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        restoreLocale(res);
        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_select_keyboard)
            .setSingleChoiceItems(items, prefs.getInt(locale.getLanguage(), 0), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    prefs.edit().putInt(locale.getLanguage(), i).commit();
                    dialogInterface.dismiss();
                }
            })
            .setCancelable(true)
            .create()
            .show();
    }
    
    Locale saveLocale;
    private Resources getLocaledResources(Locale locale) {
        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        saveLocale = conf.locale;
        conf.locale = locale;
        res.updateConfiguration(conf, res.getDisplayMetrics());
        return res;
    }

    private void restoreLocale(Resources res) {
        Configuration conf = res.getConfiguration();
        conf.locale = saveLocale;
        res.updateConfiguration(conf, res.getDisplayMetrics());
        saveLocale = null;
    }

    private boolean[] getAlts(Locale locale) {
        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        Locale saveLocale = conf.locale;
        boolean[] alts = new boolean[]{false, false};
        conf.locale = locale;
        res.updateConfiguration(conf, res.getDisplayMetrics());
        try {
            res.getXml(R.xml.kbd_qwerty_alt);
            alts[0] = true;
        } catch (Resources.NotFoundException exz) {
        }
        try {
            res.getXml(R.xml.kbd_qwerty_alt2);
            alts[1] = true;
        } catch (Resources.NotFoundException exz) {
        }
        conf.locale = saveLocale;
        res.updateConfiguration(conf, res.getDisplayMetrics());
        return alts;
    }

    private List<Locale> getEnabledLocales() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String locales = sp.getString(LatinIME.PREF_SELECTED_LANGUAGES, "");
        String[] localesArray = locales.split(",");
        ArrayList<Locale> result = new ArrayList<Locale>();
        for (String l : localesArray)
            result.add(new Locale(l.substring(0, 2), l.substring(3)));
        return result;
    }
}
