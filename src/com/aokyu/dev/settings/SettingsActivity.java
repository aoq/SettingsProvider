/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings;

import com.aokyu.dev.settings.provider.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;

public class SettingsActivity extends Activity
    implements OnSharedPreferenceChangeListener, OnItemSelectedListener {

    private Context mContext;
    private SharedPreferences mPreferences;

    private Spinner mSpinner;
    private EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_screen);

        mContext = getApplicationContext();
        mPreferences = Settings.getInstance(mContext);

        mSpinner = (Spinner) findViewById(R.id.fruit_spinner);
        mSpinner.setOnItemSelectedListener(this);
        mEdit = (EditText) findViewById(R.id.fruit_edit);

        String defaultValue = mContext.getString(R.string.apple);
        String value = mPreferences.getString(SettingKey.KEY_SETTING_FRUIT, defaultValue);
        mEdit.setText(value);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!TextUtils.isEmpty(key) && key.equals(SettingKey.KEY_SETTING_FRUIT)) {
            String defaultValue = mContext.getString(R.string.apple);
            String value = mPreferences.getString(SettingKey.KEY_SETTING_FRUIT, defaultValue);
            mEdit.setText(value);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Resources res = mContext.getResources();
        String[] names = res.getStringArray(R.array.fruit_names);
        mPreferences.edit()
            .putString(SettingKey.KEY_SETTING_FRUIT, names[position])
            .apply();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

}
