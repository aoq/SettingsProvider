/*
 * Copyright (c) 2015 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.settings;

import com.aokyu.settings.provider.SettingsContract;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The memory cache for settings to access quickly.
 * Note that the cache is not completely synchronized with
 * the original records in the database.
 * The cache does not usually have difference from original records
 * because the settings database will not be frequently updated.
 * However, the cache might not be synchronized if you immediately access
 * records after updating records.
 *
 * The cache is constructed through the change notifications of
 * the {@link com.aokyu.settings.provider.SettingsProvider}.
 * That is, this cache depends on the implementation of
 * the {@link com.aokyu.settings.provider.SettingsProvider}.
 * @see com.aokyu.settings.provider.SettingsProvider#notifyChange(Set)
 */
/* package */ class SettingsCache {

    private Context mContext;
    private ContentResolver mContentResolver;

    private boolean mLoaded = false;

    /**
     * The memory cache for settings.
     */
    private Map<Uri, Setting> mMap = new ConcurrentHashMap<Uri, Setting>();

    /**
     * The memory cache for settings that are currently changing on the database.
     */
    private Map<String, Object> mTempMap = new ConcurrentHashMap<String, Object>();

    private SettingsObserver mObserver;

    private List<CacheListener> mCacheListeners = new CopyOnWriteArrayList<CacheListener>();

    /**
     * Create a new memory cache for settings.
     * Note that the cache becomes available after loading.
     * @param context The application context.
     */
    public SettingsCache(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mObserver = new SettingsObserver(mContext, this);
        mContentResolver.registerContentObserver(
                SettingsContract.CONTENT_URI, true, mObserver);
        startLoadingFromDatabase();
    }

    /**
     * Starts loading settings from the database.
     * Note that the operation will be executed asynchronously.
     */
    private void startLoadingFromDatabase() {
        synchronized (this) {
            mLoaded = false;
        }

        Thread loader = new Thread(new Runnable() {
            @Override
            public void run() {
                loadFromDatabase();
            }
        });
        loader.start();
    }

    /**
     * Loads settings from the database into the cache.
     */
    private synchronized void loadFromDatabase() {
        Map<Uri, Setting> map = SettingsLoader.loadAll(mContentResolver);
        if (map != null) {
            mMap.putAll(map);
        }
        mLoaded = true;
        notifyAll();
    }

    public void addCacheListener(CacheListener l) {
        if (l == null) {
            throw new IllegalArgumentException("listener should not be null");
        }

        if (!mCacheListeners.contains(l)) {
            mCacheListeners.add(l);
        }
    }

    public void removeCacheListener(CacheListener l) {
        if (l == null) {
            throw new IllegalArgumentException("listener should not be null");
        }

        if (mCacheListeners.contains(l)) {
            mCacheListeners.remove(l);
        }
    }

    /**
     * Destroys the internal state of this cache.
     * This method should be called after this cache has been unnecessary.
     * No other methods may be called after destroying.
     */
    public void destroy() {
        mCacheListeners.clear();
        mContentResolver.unregisterContentObserver(mObserver);
    }

    /**
     * Waits for a loading completion.
     */
    private void awaitLoading() {
        while (!mLoaded) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
    }

    public synchronized Map<String, ?> getAllAsMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        Set<Entry<Uri, Setting>> entries = mMap.entrySet();
        for (Entry<Uri, Setting> entry : entries) {
            Setting cache = entry.getValue();
            String key = cache.getKey();
            Object value = cache.getValue();
            map.put(key, value);
        }
        return map;
    }

    public synchronized boolean contains(String key) {
        if (mTempMap.containsKey(key)) {
            return true;
        }

        awaitLoading();
        Set<Entry<Uri, Setting>> entries = mMap.entrySet();
        for (Entry<Uri, Setting> entry : entries) {
            Setting cache = entry.getValue();
            String cacheKey = cache.getKey();
            if (cacheKey.equals(key)) {
                return true;
            }
        }

        return false;
    }

    public synchronized void put(String key, Object value) {
        mTempMap.put(key, value);

        awaitLoading();
        Set<Entry<Uri, Setting>> entries = mMap.entrySet();
        Uri uri = null;
        for (Entry<Uri, Setting> entry : entries) {
            Setting cache = entry.getValue();
            String cacheKey = cache.getKey();
            if (cacheKey.equals(key)) {
                uri = entry.getKey();
                break;
            }
        }

        if (uri != null) {
            mMap.remove(uri);
        }
    }

    public synchronized void remove(String key) {
        mTempMap.remove(key);

        awaitLoading();
        Set<Entry<Uri, Setting>> entries = mMap.entrySet();
        Uri uri = null;
        for (Entry<Uri, Setting> entry : entries) {
            Setting cache = entry.getValue();
            String cacheKey = cache.getKey();
            if (cacheKey.equals(key)) {
                uri = entry.getKey();
                break;
            }
        }

        if (uri != null) {
            mMap.remove(uri);
        }
    }

    public synchronized Setting get(String key) {
        if (mTempMap.containsKey(key)) {
            Object value = mTempMap.get(key);
            return new Setting(key, value);
        }

        awaitLoading();
        Set<Entry<Uri, Setting>> entries = mMap.entrySet();
        Uri uri = null;
        for (Entry<Uri, Setting> entry : entries) {
            Setting cache = entry.getValue();
            String cacheKey = cache.getKey();
            if (cacheKey.equals(key)) {
                uri = entry.getKey();
                break;
            }
        }

        if (uri != null) {
            return mMap.get(uri);
        } else {
            return null;
        }
    }

    public synchronized void clear() {
        mMap.clear();
        mTempMap.clear();
    }

    private synchronized void put(Uri uri, Setting setting) {
        String key = setting.getKey();
        mTempMap.remove(key);

        awaitLoading();
        if (uri != null) {
            mMap.put(uri, setting);
        }

        dispatchInsertedOrUpdated(setting);
    }

    private synchronized void remove(Uri uri) {
        awaitLoading();
        if (uri != null) {
            Setting removed = mMap.remove(uri);
            String removedKey = removed.getKey();
            mTempMap.remove(removedKey);
        }

        Setting setting = mMap.get(uri);
        dispatchRemoved(setting);
    }


    private void dispatchRemoved(Setting setting) {
        for (CacheListener l : mCacheListeners) {
            l.onRemoved(setting);
        }
    }

    private void dispatchInsertedOrUpdated(Setting setting) {
        for (CacheListener l : mCacheListeners) {
            l.onInsertedOrUpdated(setting);
        }
    }

    /**
     * The observer for the settings database to update the cache status.
     */
    private static class SettingsObserver extends ContentObserver {

        private SettingsCache mCache;
        private ContentResolver mContentResolver;

        public SettingsObserver(Context context, SettingsCache cache) {
            // Callbacks will be called on the main thread.
            super(new Handler(context.getMainLooper()));
            mContentResolver = context.getContentResolver();
            mCache = cache;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (mCache) {
                mCache.awaitLoading();
            }

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
                cursor = SettingsLoader.loadCursor(mContentResolver, uri);
                if (cursor == null) {
                    return;
                }

                int count = cursor.getCount();
                if (count == 0) {
                    onSettingRemoved(uri);
                    return;
                }

                if (cursor.moveToNext()) {
                    Setting setting = Setting.cursorRowToSetting(cursor);
                    onSettingInsertedOrUpdated(uri, setting);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        /**
         * Called when a setting was removed.
         * @param uri The {@link Uri} of the removed record.
         */
        private void onSettingRemoved(Uri uri) {
            mCache.remove(uri);
        }

        /**
         * Called when a setting was newly inserted or updated.
         * @param uri The {@link Uri} of the inserted or updated record.
         * @param setting The inserted or updated setting.
         */
        private void onSettingInsertedOrUpdated(Uri uri, Setting setting) {
            mCache.put(uri, setting);
        }
    }

    /**
     * The interface to notify of changes for the settings cache.
     */
    public interface CacheListener {

        /**
         * Called when the setting was removed.
         * @param setting The removed setting.
         */
        public void onRemoved(Setting setting);

        /**
         * Called when the setting was inserted or updated.
         * @param setting The changed setting.
         */
        public void onInsertedOrUpdated(Setting setting);
    }
}
