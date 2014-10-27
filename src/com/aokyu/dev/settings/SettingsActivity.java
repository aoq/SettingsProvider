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
    implements OnSharedPreferenceChangeListener {

    private Context mContext;
    private SharedPreferences mPreferences;

    private Spinner mFruitSpinner;
    private EditText mFruitEdit;

    private Spinner mNumberSpinner;
    private EditText mNumberEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_screen);

        mContext = getApplicationContext();
        mPreferences = Settings.getInstance(mContext);
        Resources res = mContext.getResources();

        mFruitSpinner = (Spinner) findViewById(R.id.fruit_spinner);
        mFruitSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

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

        });
        mFruitEdit = (EditText) findViewById(R.id.fruit_edit);

        String defaultValue = res.getString(R.string.apple);
        String value = mPreferences.getString(SettingKey.KEY_SETTING_FRUIT, defaultValue);
        mFruitEdit.setText(value);

        mNumberSpinner = (Spinner) findViewById(R.id.number_spinner);
        mNumberSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Resources res = mContext.getResources();
                String[] numbers = res.getStringArray(R.array.numbers);
                mPreferences.edit()
                    .putString(SettingKey.KEY_SETTING_NUMBER, numbers[position])
                    .apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}

        });
        mNumberEdit = (EditText) findViewById(R.id.number_edit);

        String defaultNumber = res.getString(R.string.number_three);
        String number = mPreferences.getString(SettingKey.KEY_SETTING_NUMBER, defaultNumber);
        mNumberEdit.setText(number);
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
        if (TextUtils.isEmpty(key)) {
            return;
        }

        Resources res = mContext.getResources();
        if (key.equals(SettingKey.KEY_SETTING_FRUIT)) {
            String defaultValue = res.getString(R.string.apple);
            String value = mPreferences.getString(SettingKey.KEY_SETTING_FRUIT, defaultValue);
            mFruitEdit.setText(value);
        } else if (key.equals(SettingKey.KEY_SETTING_NUMBER)) {
            String defaultValue = res.getString(R.string.number_three);
            String value = mPreferences.getString(SettingKey.KEY_SETTING_NUMBER, defaultValue);
            mNumberEdit.setText(value);
        }
    }
}
