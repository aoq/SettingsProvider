/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider;

import com.aokyu.dev.settings.provider.task.AbstractTask;
import com.aokyu.dev.settings.provider.task.SerialExecutor;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class can access and modify preference data stored in the database.
 * This class can be used across multiple processes through the content provider.
 * TODO: Should implement a memory cache to reduce the time to access settings.
 *
 * @see SettingsEditor
 */
public class Settings implements SharedPreferences {

    private static final String TASK_NAME = "Settings";

    private static volatile SharedPreferences sHelper;

    private static volatile SerialExecutor sExecutor = new SerialExecutor(TASK_NAME);

    private Context mContext;
    private ContentResolver mContentResolver;

    private SettingsChangeListeners mChangeListeners;

    private static final String[] PROJECTION = {
        SettingsContract._ID,
        SettingsContract.KEY,
        SettingsContract.TYPE,
        SettingsContract.VALUE
    };

    private static final class SettingLoader {

        private static final String SELECTION = SettingsContract.KEY + "=?";
        private static final String SORT_ORDER = SettingsContract._ID + " DESC LIMIT 1";

        public static Cursor load(ContentResolver resolver, String key) {
            return resolver.query(SettingsContract.CONTENT_URI, PROJECTION,
                    SELECTION, new String[] { key }, SORT_ORDER);
        }

        public static Cursor load(ContentResolver resolver, Uri settingUri) {
            return resolver.query(settingUri, PROJECTION, null, null, SORT_ORDER);
        }

        public static Cursor loadAll(ContentResolver resolver) {
            return resolver.query(SettingsContract.CONTENT_URI, PROJECTION,
                    null, null, null);
        }
    }

    public static SharedPreferences getInstance(Context context) {
        if (sHelper == null) {
            synchronized(Settings.class) {
                sHelper = new Settings(context);
            }
        }
        return sHelper;
    }

    private Settings(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mChangeListeners = new SettingsChangeListeners(mContext, this);
    }

    @Override
    protected void finalize() throws Throwable {
        // Just in case some objects are not release.
        try {
            mChangeListeners.destroy();
        } finally {
            super.finalize();
        }
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
        mChangeListeners.registerOnSharedPreferenceChangeListener(l);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
        mChangeListeners.unregisterOnSharedPreferenceChangeListener(l);
    }

    @Override
    public boolean contains(String key) {
        Cursor cursor = null;
        try {
            cursor = SettingLoader.load(mContentResolver, key);
            if (cursor == null) {
                return false;
            }

            if (cursor.moveToNext()) {
                return true;
            } else {
                return false;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public Map<String, ?> getAll() {
        Map<String, Object> map = new HashMap<String, Object>();
        Cursor cursor = null;
        try {
            cursor = SettingLoader.loadAll(mContentResolver);
            if (cursor == null) {
                return map;
            }

            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                Setting setting = new Setting(values);
                String key = setting.getKey();
                Object value = setting.getValue();
                map.put(key, value);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return map;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Cursor cursor = null;
        Setting setting = null;
        try {
            cursor = SettingLoader.load(mContentResolver, key);
            if (cursor == null) {
                return defValue;
            }

            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                setting = new Setting(values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (setting != null) {
            Boolean value = (Boolean) setting.getValue();
            return value.booleanValue();
        } else {
            return defValue;
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        Cursor cursor = null;
        Setting setting = null;
        try {
            cursor = SettingLoader.load(mContentResolver, key);
            if (cursor == null) {
                return defValue;
            }

            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                setting = new Setting(values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (setting != null) {
            Float value = (Float) setting.getValue();
            return value.floatValue();
        } else {
            return defValue;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        Cursor cursor = null;
        Setting setting = null;
        try {
            cursor = SettingLoader.load(mContentResolver, key);
            if (cursor == null) {
                return defValue;
            }

            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                setting = new Setting(values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (setting != null) {
            Integer value = (Integer) setting.getValue();
            return value.intValue();
        } else {
            return defValue;
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        Cursor cursor = null;
        Setting setting = null;
        try {
            cursor = SettingLoader.load(mContentResolver, key);
            if (cursor == null) {
                return defValue;
            }

            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                setting = new Setting(values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (setting != null) {
            Long value = (Long) setting.getValue();
            return value.longValue();
        } else {
            return defValue;
        }
    }

    @Override
    public String getString(String key, String defValue) {
        Cursor cursor = null;
        Setting setting = null;
        try {
            cursor = SettingLoader.load(mContentResolver, key);
            if (cursor == null) {
                return defValue;
            }

            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                setting = new Setting(values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (setting != null) {
            String value = (String) setting.getValue();
            return value;
        } else {
            return defValue;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Cursor cursor = null;
        Setting setting = null;
        try {
            cursor = SettingLoader.load(mContentResolver, key);
            if (cursor == null) {
                return defValues;
            }

            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                setting = new Setting(values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (setting != null) {
            Set<String> value = (Set<String>) setting.getValue();
            return value;
        } else {
            return defValues;
        }
    }

    public static final class SettingsChangeListeners extends ContentObserver {

        private ContentResolver mContentResolver;

        private SharedPreferences mPreferences;

        private Object mRegistrationLock = new Object();
        private boolean mRegistered = false;

        private List<OnSharedPreferenceChangeListener> mListeners =
                new CopyOnWriteArrayList<OnSharedPreferenceChangeListener>();

        public SettingsChangeListeners(Context context, SharedPreferences prefs) {
            // Callbacks will be called on the main thread.
            super(new Handler(context.getMainLooper()));
            mContentResolver = context.getContentResolver();
            mPreferences = prefs;
        }

        public void destroy() {
            mListeners.clear();
            if (mRegistered) {
                mContentResolver.unregisterContentObserver(this);
                mRegistered = false;
            }
        }

        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
            if (l == null) {
                throw new IllegalArgumentException("listener should not be null");
            }

            if (!mListeners.contains(l)) {
                mListeners.add(l);
            }

            if (!mListeners.isEmpty()) {
                onRegistered();
            }
        }

        private void onRegistered() {
            synchronized (mRegistrationLock) {
                if (!mRegistered) {
                    mContentResolver.registerContentObserver(
                            SettingsContract.CONTENT_URI, true, this);
                    mRegistered = true;
                }
            }
        }

        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
            if (l == null) {
                throw new IllegalArgumentException("listener should not be null");
            }

            if (mListeners.contains(l)) {
                mListeners.remove(l);
            }

            if (mListeners.isEmpty()) {
                onUnregistered();
            }
        }

        private void onUnregistered() {
            synchronized (mRegistrationLock) {
                if (mRegistered) {
                    mContentResolver.unregisterContentObserver(this);
                    mRegistered = false;
                }
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }

            String lastPath = uri.getLastPathSegment();
            try {
                Long.parseLong(lastPath);
            } catch (NumberFormatException e) {
                return;
            }

            Cursor cursor = null;
            try {
                cursor = SettingLoader.load(mContentResolver, uri);
                if (cursor == null) {
                    return;
                }

                if (cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    CursorUtils.cursorRowToContentValues(cursor, values);
                    Setting setting = new Setting(values);
                    dispatchSharedPreferenceChanged(setting);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private void dispatchSharedPreferenceChanged(Setting setting) {
            String key = setting.getKey();
            for (OnSharedPreferenceChangeListener l : mListeners) {
                l.onSharedPreferenceChanged(mPreferences, key);
            }
        }
    }

    @Override
    public Editor edit() {
        return new SettingsEditor(mContext, sExecutor);
    }

    /**
     * Note that an edit for settings will be executed on a single worker thread
     * if using {@link #apply()}.
     * In addition, you might not be able to get the edited settings immediately
     * because the edit will be executed asynchronously.
     */
    private static final class SettingsEditor implements Editor {

        private Context mContext;
        private SerialExecutor mExecutor;

        private Commit mCommit;

        public SettingsEditor(Context context, SerialExecutor executor) {
            mContext = context;
            mExecutor = executor;
            mCommit = new Commit(context);
        }

        @Override
        public void apply() {
            mExecutor.execute(mCommit);
        }

        @Override
        public boolean commit() {
            try {
                mCommit.execute();
            } catch (InterruptedException e) {
                return false;
            }
            return true;
        }

        @Override
        public Editor clear() {
            mCommit.add(new Clear(mContext));
            return this;
        }

        private Editor put(String key, Object value) {
            Setting setting = new Setting(key, value);
            ContentValues values = setting.toContentValues();
            mCommit.add(new InsertOrUpdate(mContext, values));
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            return put(key, value);
        }

        @Override
        public Editor putFloat(String key, float value) {
            return put(key, value);
        }

        @Override
        public Editor putInt(String key, int value) {
            return put(key, value);
        }

        @Override
        public Editor putLong(String key, long value) {
            return put(key, value);
        }

        @Override
        public Editor putString(String key, String value) {
            return put(key, value);
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            return put(key, values);
        }

        @Override
        public Editor remove(String key) {
            mCommit.add(new Remove(mContext, key));
            return this;
        }
    }

    private static final class Commit extends AbstractTask {

        private ContentResolver mContentResolver;

        private List<Edit> mEdits = new ArrayList<Edit>();
        private Clear mClear;

        public Commit(Context context) {
            mContentResolver = context.getContentResolver();
        }

        public void add(Edit edit) {
            if (edit instanceof InsertOrUpdate || edit instanceof Remove) {
                mEdits.add(edit);
            } else if (edit instanceof Clear) {
                mClear = (Clear) edit;
            }
        }

        @Override
        public void execute() throws InterruptedException {
            ArrayList<ContentProviderOperation> operations =
                    new ArrayList<ContentProviderOperation>();

            // The clearing operation should be done first.
            if (mClear != null) {
                ContentProviderOperation clearOperation = mClear.buildOperation();
                if (clearOperation != null) {
                    operations.add(clearOperation);
                }
            }

            List<ContentProviderOperation> editOperations = buildOperations(mEdits);
            if (!editOperations.isEmpty()) {
                operations.addAll(editOperations);
            }

            String authority = SettingsContract.CONTENT_URI.getAuthority();
            try {
                mContentResolver.applyBatch(authority, operations);
            } catch (RemoteException e) {
            } catch (OperationApplicationException e) {
            }
        }

        private List<ContentProviderOperation> buildOperations(List<? extends Edit> editList) {
            List<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            for (Edit edit : editList) {
                ContentProviderOperation operation = edit.buildOperation();
                if (operation != null) {
                    operations.add(operation);
                }
            }
            return operations;
        }
    }

    private static abstract class Edit {

        private static final Uri CONTENT_URI = SettingsContract.CONTENT_URI;

        protected ContentResolver mContentResolver;

        public abstract ContentProviderOperation buildOperation();

        public Edit(Context context) {
            mContentResolver = context.getContentResolver();
        }

        protected ContentProviderOperation newInsert(ContentValues values) {
            return ContentProviderOperation.newInsert(CONTENT_URI)
                    .withValues(values)
                    .build();
        }

        protected ContentProviderOperation newUpdate(
                String selection, String[] selectionArgs, ContentValues values) {
            return ContentProviderOperation.newUpdate(CONTENT_URI)
                    .withSelection(selection, selectionArgs)
                    .withValues(values)
                    .build();
        }

        protected ContentProviderOperation newDelete(String selection, String[] selectionArgs) {
            return ContentProviderOperation.newDelete(CONTENT_URI)
                    .withSelection(selection, selectionArgs)
                    .build();
        }
    }

    private static final class InsertOrUpdate extends Edit {

        private ContentValues mValues;

        public InsertOrUpdate(Context context, ContentValues values) {
            super(context);
            mValues = values;
        }

        public ContentProviderOperation buildOperation() {
            String key = mValues.getAsString(SettingsContract.KEY);
            Cursor cursor = null;
            try {
                cursor = SettingLoader.load(mContentResolver, key);
                if (cursor == null) {
                    return null;
                }

                int count = cursor.getCount();
                if (count == 0) {
                    return newInsert(mValues);
                }

                if (cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    CursorUtils.cursorRowToContentValues(cursor, values);
                    long id = values.getAsLong(SettingsContract._ID);
                    String where = SettingsContract._ID + "=?";
                    String[] selectionArgs = new String[] { String.valueOf(id) };
                    return newUpdate(where, selectionArgs, mValues);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return null;
        }
    }

    private static final class Remove extends Edit {

        private String mKey;

        public Remove(Context context, String key) {
            super(context);
            mKey = key;
        }

        public ContentProviderOperation buildOperation() {
            String where = SettingsContract.KEY + "=?";
            String[] selectionArgs = new String[] { mKey };
            return newDelete(where, selectionArgs);
        }
    }

    private static class Clear extends Edit {

        public Clear(Context context) {
            super(context);
        }

        @Override
        public ContentProviderOperation buildOperation() {
            return newDelete(null, null);
        }
    }
}
