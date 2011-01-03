/*
 * Copyright (C) 2008-2009 Google Inc.
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;
import android.text.TextUtils;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class InputLanguageSelection extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    private String mSelectedLanguages;
    private ArrayList<Loc> mAvailableLanguages = new ArrayList<Loc>();
    private static final String[] BLACKLIST_LANGUAGES = {
        "ko", "ja", "zh", "el"
    };

    SharedPreferences prefs;

    public boolean onPreferenceClick(Preference preference) {
        PreferenceGroup parent = getPreferenceScreen();
        int count = parent.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            CheckBoxPreference pref = (CheckBoxPreference) parent.getPreference(i);
            if (pref == preference && pref.isChecked()) {
                //show chooser with the available alternative keyboards for this locale
                final Locale locale = mAvailableLanguages.get(i).locale;
                Resources res = getLocaledResources(locale);
                boolean[] alts = getAlts(locale);
                String items[] = res.getStringArray(R.array.alt_keyboards_entries);
                if (alts[0] || alts[1]) {
                    new AlertDialog.Builder(this)
                        .setTitle(R.string.dlg_select_keyboard)
                        .setSingleChoiceItems(items, prefs.getInt(locale.getLanguage(), 0), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                prefs.edit().putInt(locale.getLanguage(), i).commit();
                                dialogInterface.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                }
                restoreLocale(res);
            }
        }

        return false;
    }

    private static class Loc implements Comparable<Object> {
        static Collator sCollator = Collator.getInstance();

        String label;
        Locale locale;

        public Loc(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }

        @Override
        public String toString() {
            return this.label;
        }

        public int compareTo(Object o) {
            return sCollator.compare(this.label, ((Loc) o).label);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.language_prefs);
        // Get the settings preferences
        SharedPreferences sp = prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedLanguages = sp.getString(LatinIME.PREF_SELECTED_LANGUAGES, "");
        String[] languageList = mSelectedLanguages.split(",");
        mAvailableLanguages = getUniqueLocales();
        PreferenceGroup parent = getPreferenceScreen();
        for (int i = 0; i < mAvailableLanguages.size(); i++) {
            CheckBoxPreference pref = new CheckBoxPreference(this);
            Locale locale = mAvailableLanguages.get(i).locale;
            pref.setTitle(LanguageSwitcher.toTitleCase(locale.getDisplayName(locale)));
            boolean checked = isLocaleIn(locale, languageList);
            pref.setChecked(checked);
            pref.setOnPreferenceClickListener(this);
            if (hasDictionary(locale)) {
                pref.setSummary(R.string.has_dictionary);
            }
            parent.addPreference(pref);
        }
        Toast.makeText(this, "To change the active keyboard toggle the selected language", Toast.LENGTH_LONG).show();
    }

    private boolean isLocaleIn(Locale locale, String[] list) {
        String lang = get5Code(locale);
        for (int i = 0; i < list.length; i++) {
            if (lang.equalsIgnoreCase(list[i])) return true;
        }
        return false;
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

    private boolean hasDictionary(Locale locale) {
        Resources res = getLocaledResources(locale);
        boolean haveDictionary = false;

        int[] dictionaries = LatinIME.getDictionary(res);
        BinaryDictionary bd = new BinaryDictionary(this, dictionaries, Suggest.DIC_MAIN);

        // Is the dictionary larger than a placeholder? Arbitrarily chose a lower limit of
        // 4000-5000 words, whereas the LARGE_DICTIONARY is about 20000+ words.
        if (bd.getSize() > Suggest.LARGE_DICTIONARY_THRESHOLD / 4) {
            haveDictionary = true;
        }
        bd.close();
        restoreLocale(res);
        return haveDictionary;
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

    private String get5Code(Locale locale) {
        String country = locale.getCountry();
        return locale.getLanguage()
                + (TextUtils.isEmpty(country) ? "" : "_" + country);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save the selected languages
        String checkedLanguages = "";
        PreferenceGroup parent = getPreferenceScreen();
        int count = parent.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            CheckBoxPreference pref = (CheckBoxPreference) parent.getPreference(i);
            if (pref.isChecked()) {
                Locale locale = mAvailableLanguages.get(i).locale;
                checkedLanguages += get5Code(locale) + ",";
            }
        }
        if (checkedLanguages.length() < 1) checkedLanguages = null; // Save null
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = sp.edit();
        editor.putString(LatinIME.PREF_SELECTED_LANGUAGES, checkedLanguages);
        SharedPreferencesCompat.apply(editor);
    }

    ArrayList<Loc> getUniqueLocales() {
        String[] locales = getAssets().getLocales();
        for (String s : locales)
            System.out.println(s);
        Arrays.sort(locales);
        ArrayList<Loc> uniqueLocales = new ArrayList<Loc>();

        final int origSize = locales.length;
        Loc[] preprocess = new Loc[origSize];
        int finalSize = 0;
        for (int i = 0 ; i < origSize; i++ ) {
            String s = locales[i];
            int len = s.length();
            if (len == 5 || s.equals("bg")) {
                Locale l;
                String language = s.substring(0, 2);
                if (!s.equals("bg")) {
                    String country = s.substring(3, 5);
                    l = new Locale(language, country);
                } else {
                    l = new Locale("bg", "BG");
                }

                // Exclude languages that are not relevant to LatinIME
                if (arrayContains(BLACKLIST_LANGUAGES, language)) continue;

                if (finalSize == 0) {
                    preprocess[finalSize++] =
                            new Loc(LanguageSwitcher.toTitleCase(l.getDisplayName(l)), l);
                } else {
                    // check previous entry:
                    //  same lang and a country -> upgrade to full name and
                    //    insert ours with full name
                    //  diff lang -> insert ours with lang-only name
                    if (preprocess[finalSize-1].locale.getLanguage().equals(
                            language)) {
                        preprocess[finalSize-1].label = LanguageSwitcher.toTitleCase(
                                preprocess[finalSize-1].locale.getDisplayName());
                        preprocess[finalSize++] =
                                new Loc(LanguageSwitcher.toTitleCase(l.getDisplayName()), l);
                    } else {
                        String displayName;
                        if (s.equals("zz_ZZ")) {
                        } else {
                            displayName = LanguageSwitcher.toTitleCase(l.getDisplayName(l));
                            preprocess[finalSize++] = new Loc(displayName, l);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < finalSize ; i++) {
            uniqueLocales.add(preprocess[i]);
        }
        return uniqueLocales;
    }

    private boolean arrayContains(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) return true;
        }
        return false;
    }
}
