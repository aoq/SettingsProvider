/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider;

import com.aokyu.dev.settings.provider.SettingsCache.CacheListener;
import com.aokyu.dev.settings.provider.task.AbstractTask;
import com.aokyu.dev.settings.provider.task.SerialExecutor;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
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
    private SettingsCache mCache;

    private SettingsChangeListeners mChangeListeners;

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
        mCache = new SettingsCache(mContext);
        mChangeListeners = new SettingsChangeListeners(mContext, this);
        mCache.addCacheListener(mChangeListeners);
    }

    @Override
    protected void finalize() throws Throwable {
        // Just in case some objects are not release.
        try {
            mCache.destroy();
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
        return mCache.contains(key);
    }

    @Override
    public Map<String, ?> getAll() {
        return mCache.getAllAsMap();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Setting setting = mCache.get(key);
        if (setting != null) {
            Object value = setting.getValue();
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                throw new IllegalStateException("setting is " + setting.getClass());
            }
        } else {
            return defValue;
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        Setting setting = mCache.get(key);
        if (setting != null) {
            Object value = setting.getValue();
            if (value instanceof Float) {
                return (Float) value;
            } else {
                throw new IllegalStateException("setting is " + setting.getClass());
            }
        } else {
            return defValue;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        Setting setting = mCache.get(key);
        if (setting != null) {
            Object value = setting.getValue();
            if (value instanceof Integer) {
                return (Integer) value;
            } else {
                throw new IllegalStateException("setting is " + setting.getClass());
            }
        } else {
            return defValue;
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        Setting setting = mCache.get(key);
        if (setting != null) {
            Object value = setting.getValue();
            if (value instanceof Long) {
                return (Long) value;
            } else {
                throw new IllegalStateException("setting is " + setting.getClass());
            }
        } else {
            return defValue;
        }
    }

    @Override
    public String getString(String key, String defValue) {
        Setting setting = mCache.get(key);
        if (setting != null) {
            Object value = setting.getValue();
            if (value instanceof String) {
                return (String) value;
            } else {
                throw new IllegalStateException("setting is " + setting.getClass());
            }
        } else {
            return defValue;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Setting setting = mCache.get(key);
        if (setting != null) {
            Object value = setting.getValue();
            if (value instanceof Set<?>) {
                return (Set<String>) value;
            } else {
                throw new IllegalStateException("setting is " + setting.getClass());
            }
        } else {
            return defValues;
        }
    }

    public static final class SettingsChangeListeners implements CacheListener {

        private SharedPreferences mPreferences;

        private List<OnSharedPreferenceChangeListener> mListeners =
                new CopyOnWriteArrayList<OnSharedPreferenceChangeListener>();

        public SettingsChangeListeners(Context context, SharedPreferences prefs) {
            mPreferences = prefs;
        }

        public void destroy() {
            mListeners.clear();
        }

        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
            if (l == null) {
                throw new IllegalArgumentException("listener should not be null");
            }

            if (!mListeners.contains(l)) {
                mListeners.add(l);
            }
        }

        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
            if (l == null) {
                throw new IllegalArgumentException("listener should not be null");
            }

            if (mListeners.contains(l)) {
                mListeners.remove(l);
            }
        }

        @Override
        public void onRemoved(Setting setting) {
            dispatchSharedPreferenceChanged(setting);
        }

        @Override
        public void onInsertedOrUpdated(Setting setting) {
            dispatchSharedPreferenceChanged(setting);
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
        return new SettingsEditor(mContext, this);
    }

    private void onApply(Commit commit) {
        commit.cache(mCache);
        sExecutor.execute(commit);
    }

    private boolean onCommit(Commit commit) {
        try {
            commit.cache(mCache);
            commit.execute();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    /**
     * Note that an edit for settings will be executed on a single worker thread
     * if using {@link #apply()}.
     */
    private static final class SettingsEditor implements Editor {

        private Context mContext;
        private Settings mSettings;

        private Commit mCommit;

        public SettingsEditor(Context context, Settings settings) {
            mContext = context;
            mSettings = settings;
            mCommit = new Commit(context);
        }

        @Override
        public void apply() {
            mSettings.onApply(mCommit);
        }

        @Override
        public boolean commit() {
            return mSettings.onCommit(mCommit);
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

        /**
         * The cache operation should be executed on the same execution context as
         * {@link Editor#apply()} or {@link Editor#commit()}.
         */
        public void cache(SettingsCache cache) {
            if (mClear != null) {
                cache.clear();
            }

            if (!mEdits.isEmpty()) {
                for (Edit edit : mEdits) {
                    if (edit instanceof InsertOrUpdate) {
                        InsertOrUpdate operation = (InsertOrUpdate) edit;
                        String key = operation.getKey();
                        Object value = operation.getValue();
                        if (!TextUtils.isEmpty(key)) {
                            cache.put(key, value);
                        }
                    } else if (edit instanceof Remove) {
                        Remove operation = (Remove) edit;
                        String key = operation.getKey();
                        if (!TextUtils.isEmpty(key)) {
                            cache.remove(key);
                        }
                    }
                }
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

        private String getKey() {
            if (mValues != null) {
                if (mValues.containsKey(SettingsContract.KEY)) {
                    return mValues.getAsString(SettingsContract.KEY);
                }
            }

            return null;
        }

        private Object getValue() {
            if (mValues != null) {
                if (mValues.containsKey(SettingsContract.VALUE)) {
                    return mValues.get(SettingsContract.VALUE);
                }
            }

            return null;
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

        private String getKey() {
            return mKey;
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
