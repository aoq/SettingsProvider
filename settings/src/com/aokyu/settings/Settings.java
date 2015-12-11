/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.settings;

import com.aokyu.settings.SettingsCache.CacheListener;
import com.aokyu.settings.provider.SettingsContract;
import com.aokyu.settings.task.AbstractTask;
import com.aokyu.settings.task.SerialExecutor;

import android.content.ContentProvider;
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
 * This class can access or modify primitive data stored in the database.
 * Unlike the Android default implementation of {@link SharedPreferences},
 * this class can be used across multiple processes since the {@link ContentProvider} is used for
 * storing primitive data. Note that changes may not be applied for actual values yet,
 * if changed values are accessed immediately from multiple processes at the moment of changing
 * values.
 * You should use {@link OnSharedPreferenceChangeListener} to observe changes for settings.
 * @see SettingsEditor
 */
public class Settings implements SharedPreferences {

    private static final String TASK_NAME = "Settings";

    private static volatile SharedPreferences sHelper;

    /**
     * The executor for editing actual values on the database.
     * @see #onApply(Commit)
     */
    private static volatile SerialExecutor sExecutor = new SerialExecutor(TASK_NAME);

    private Context mContext;

    /**
     * The memory cache for settings.
     */
    private SettingsCache mCache;

    /**
     * The listener manager.
     */
    private SettingsChangeListeners mChangeListeners;

    /**
     * Returns an implementation of {@link SharedPreferences} using {@link ContentProvider}.
     *
     * @param context The application context.
     * @return an implementation of {@link SharedPreferences}.
     */
    public static SharedPreferences getInstance(Context context) {
        if (sHelper == null) {
            synchronized (Settings.class) {
                sHelper = new Settings(context);
            }
        }
        return sHelper;
    }

    /**
     * Create a new instance of {@link SharedPreferences}.
     *
     * @param context The application context.
     */
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

    /**
     * This class holds {@link OnSharedPreferenceChangeListener}s and dispatches callbacks for
     * changes of shared preferences.
     *
     * @see OnSharedPreferenceChangeListener
     */
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

    /**
     * Called when changes need to be applied to the database asynchronously.
     *
     * @param commit The commit to apply.
     * @see Editor#apply()
     */
    private void onApply(Commit commit) {
        commit.cache(mCache);
        sExecutor.execute(commit);
    }

    /**
     * Called when changes need to be applied to the database synchronously.
     *
     * @param commit The commit to apply.
     * @return true if the new values were successfully written to the database.
     * @see Editor#commit()
     */
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

    /**
     * The edit operation types.
     * @see Commit
     */
    private enum EditType {
        /**
         * The type for insert or update operations.
         * @see InsertOrUpdate
         */
        INSERT_OR_UPDATE,

        /**
         * The type for remove operations.
         * @see Remove
         */
        REMOVE,

        /**
         * The type for cleanup operations.
         * @see Clear
         */
        CLEAR
    }

    /**
     * A task to commit changes.
     */
    private static final class Commit extends AbstractTask {

        private ContentResolver mContentResolver;

        /**
         * A cleanup task is treated distinctively from other tasks
         * since the cleanup should be done first on this commit.
         * @see Editor#clear()
         */
        private Clear mClearOperation;

        /**
         * Edit tasks excluding a cleanup task.
         */
        private List<Edit> mEditOperations = new ArrayList<Edit>();

        /**
         * Creates a new commit.
         * @param context The application context used to get the {@link ContentResolver}.
         */
        public Commit(Context context) {
            mContentResolver = context.getContentResolver();
        }

        /**
         * Adds an edit operation to this commit.
         * @param edit An edit operation.
         */
        public void add(Edit edit) {
            EditType type = edit.getType();
            switch (type) {
                case INSERT_OR_UPDATE:
                case REMOVE:
                    mEditOperations.add(edit);
                    break;
                case CLEAR:
                    mClearOperation = (Clear) edit;
                    break;
                default:
                    break;
            }
        }

        /**
         * The cache operation should be executed on the same execution context as
         * {@link Editor#apply()} or {@link Editor#commit()}.
         */
        public void cache(SettingsCache cache) {
            if (mClearOperation != null) {
                cache.clear();
            }

            for (Edit edit : mEditOperations) {
                EditType type = edit.getType();
                switch (type) {
                    case INSERT_OR_UPDATE:
                        InsertOrUpdate editOperation = (InsertOrUpdate) edit;
                        String editKey = editOperation.getKey();
                        Object editValue = editOperation.getValue();
                        if (!TextUtils.isEmpty(editKey)) {
                            cache.put(editKey, editValue);
                        }
                        break;
                    case REMOVE:
                        Remove removeOperation = (Remove) edit;
                        String removeKey = removeOperation.getKey();
                        if (!TextUtils.isEmpty(removeKey)) {
                            cache.remove(removeKey);
                        }
                        break;
                    case CLEAR:
                    default:
                        break;
                }
            }
        }

        /**
         * Executes this commit for the database.
         */
        @Override
        public void execute() throws InterruptedException {
            ArrayList<ContentProviderOperation> operations =
                    new ArrayList<ContentProviderOperation>();

            // The cleanup operation should be done first.
            if (mClearOperation != null) {
                ContentProviderOperation operation = mClearOperation.build();
                if (operation != null) {
                    operations.add(operation);
                }
                mClearOperation = null;
            }

            for (Edit edit : mEditOperations) {
                ContentProviderOperation operation = edit.build();
                if (operation != null) {
                    operations.add(operation);
                }
            }
            mEditOperations.clear();

            String authority = SettingsContract.CONTENT_URI.getAuthority();
            try {
                mContentResolver.applyBatch(authority, operations);
            } catch (RemoteException e) {
            } catch (OperationApplicationException e) {
            }
        }
    }

    /**
     * An base operation class for editing.
     */
    private static abstract class Edit {

        private static final Uri CONTENT_URI = SettingsContract.CONTENT_URI;

        protected ContentResolver mContentResolver;

        /**
         * Returns the type of this operation.
         * @return the type of this operation.
         */
        public abstract EditType getType();

        /**
         * Returns a {@link ContentProviderOperation} for this operation.
         * @return a {@link ContentProviderOperation} for this operation.
         */
        public abstract ContentProviderOperation build();

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

    /**
     * An insert or update operation to the database.
     */
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

        @Override
        public EditType getType() {
            return EditType.INSERT_OR_UPDATE;
        }

        @Override
        public ContentProviderOperation build() {
            String key = mValues.getAsString(SettingsContract.KEY);
            Cursor cursor = null;
            try {
                cursor = SettingsLoader.loadCursor(mContentResolver, key);
                if (cursor == null) {
                    return null;
                }

                int count = cursor.getCount();
                if (count == 0) {
                    return newInsert(mValues);
                }

                if (cursor.moveToNext()) {
                    Setting setting = Setting.cursorRowToSetting(cursor);
                    long id = setting.getId();
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

    /**
     * A removal operation to the database.
     */
    private static final class Remove extends Edit {

        private String mKey;

        public Remove(Context context, String key) {
            super(context);
            mKey = key;
        }

        private String getKey() {
            return mKey;
        }

        @Override
        public EditType getType() {
            return EditType.REMOVE;
        }

        @Override
        public ContentProviderOperation build() {
            String where = SettingsContract.KEY + "=?";
            String[] selectionArgs = new String[] { mKey };
            return newDelete(where, selectionArgs);
        }
    }

    /**
     * A cleanup operation to the database.
     */
    private static class Clear extends Edit {

        public Clear(Context context) {
            super(context);
        }

        @Override
        public EditType getType() {
            return EditType.CLEAR;
        }

        @Override
        public ContentProviderOperation build() {
            return newDelete(null, null);
        }
    }
}
