/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.settings.sample;

import com.aokyu.settings.Settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import butterknife.OnItemSelected.Callback;

public class SettingsActivity extends AppCompatActivity
        implements OnSharedPreferenceChangeListener {

    private Context mContext;
    private SharedPreferences mPreferences;

    @Bind(R.id.fruit_spinner)
    Spinner mFruitSpinner;
    private boolean mFruitInitialized = false;
    @Bind(R.id.fruit_edit)
    EditText mFruitEdit;

    @Bind(R.id.number_spinner)
    Spinner mNumberSpinner;
    private boolean mNumberInitialized = false;
    @Bind(R.id.number_edit)
    EditText mNumberEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_settings);
        ButterKnife.bind(this);

        mContext = getApplicationContext();
        mPreferences = Settings.getInstance(mContext);
        Resources res = mContext.getResources();

        String defaultValue = res.getString(R.string.apple);
        String value = mPreferences.getString(SettingKey.KEY_SETTING_FRUIT, defaultValue);
        mFruitSpinner.setSelection(getFruitIndex(value));
        mFruitEdit.setText(value);

        String defaultNumber = res.getString(R.string.number_three);
        String number = mPreferences.getString(SettingKey.KEY_SETTING_NUMBER, defaultNumber);
        mNumberSpinner.setSelection(getNumberIndex(number));
        mNumberEdit.setText(number);
    }

    private int getNumberIndex(String number) {
        Resources res = mContext.getResources();
        String[] names = res.getStringArray(R.array.numbers);
        int index = 0;
        for (String name : names) {
            if (number.equals(name)) {
                return index;
            }
            index++;
        }

        throw new IllegalStateException("illegal selection : " + number);
    }

    private int getFruitIndex(String value) {
        Resources res = mContext.getResources();
        String[] names = res.getStringArray(R.array.fruit_names);
        int index = 0;
        for (String name : names) {
            if (value.equals(name)) {
                return index;
            }
            index++;
        }

        throw new IllegalStateException("illegal selection : " + value);
    }

    @OnItemSelected(value = R.id.fruit_spinner, callback = Callback.ITEM_SELECTED)
    void onFruitSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mFruitInitialized) {
            Resources res = mContext.getResources();
            String[] names = res.getStringArray(R.array.fruit_names);
            mPreferences.edit()
                    .putString(SettingKey.KEY_SETTING_FRUIT, names[position])
                    .apply();
        } else {
            mFruitInitialized = true;
        }
    }

    @OnItemSelected(value = R.id.fruit_spinner, callback = Callback.NOTHING_SELECTED)
    void onNoFruitSelected(AdapterView<?> parent) {}

    @OnItemSelected(value = R.id.number_spinner, callback = Callback.ITEM_SELECTED)
    void onNumberSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mNumberInitialized) {
            Resources res = mContext.getResources();
            String[] numbers = res.getStringArray(R.array.numbers);
            mPreferences.edit()
                    .putString(SettingKey.KEY_SETTING_NUMBER, numbers[position])
                    .apply();
        } else {
            mNumberInitialized = true;
        }
    }

    @OnItemSelected(value = R.id.number_spinner, callback = Callback.NOTHING_SELECTED)
    void onNoNumberSelected(AdapterView<?> parent) {}

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
