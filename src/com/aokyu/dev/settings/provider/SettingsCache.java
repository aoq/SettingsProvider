package com.aokyu.dev.settings.provider;

import com.aokyu.dev.settings.provider.SettingsLoader.LoaderListener;
import com.aokyu.dev.settings.provider.task.SerialExecutor;

import android.content.ContentResolver;
import android.content.ContentValues;
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

public class SettingsCache extends ContentObserver implements LoaderListener {

    private Context mContext;
    private ContentResolver mContentResolver;

    private static final String TASK_NAME = "SettingsCache";

    private SerialExecutor mExecutor = new SerialExecutor(TASK_NAME);

    private boolean mLoaded = false;

    private Map<Uri, Setting> mMap;
    private Map<String, Object> mTempMap = new ConcurrentHashMap<String, Object>();

    private List<CacheListener> mCacheListeners = new CopyOnWriteArrayList<CacheListener>();

    public SettingsCache(Context context) {
        // Callbacks will be called on the main thread.
        super(new Handler(context.getMainLooper()));
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mContentResolver.registerContentObserver(
                SettingsContract.CONTENT_URI, true, this);
        startLodingFromDatabase();
    }

    private void startLodingFromDatabase() {
        if (mLoaded) {
            return;
        }

        SettingsLoader loader = new SettingsLoader(mContext, this);
        mExecutor.execute(loader);
    }

    @Override
    public void onLoadFinished(Map<Uri, Setting> map) {
        synchronized (this) {
            mLoaded = true;
            if (map != null) {
                mMap = map;
            } else {
                mMap = new ConcurrentHashMap<Uri, Setting>();
            }

            notifyAll();
        }
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

    public void destroy() {
        mCacheListeners.clear();
        mContentResolver.unregisterContentObserver(this);
        mExecutor.destroy();
    }

    private void awaitLoading() {
        while (!mLoaded) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
    }

    public Map<String, ?> getAllAsMap() {
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

    public boolean contains(String key) {
        synchronized (this) {
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
    }

    public void put(String key, Object value) {
        synchronized (this) {
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
    }

    public void remove(String key) {
        synchronized (this) {
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
    }

    public Setting get(String key) {
        synchronized (this) {
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
    }

    private void put(Uri uri, Setting setting) {
        synchronized (this) {
            String key = setting.getKey();
            mTempMap.remove(key);

            awaitLoading();
            if (uri != null) {
                mMap.put(uri, setting);
            }
        }
    }

    private void remove(Uri uri) {
        synchronized (this) {
            awaitLoading();
            if (uri != null) {
                Setting removed = mMap.remove(uri);
                String removedKey = removed.getKey();
                mTempMap.remove(removedKey);
            }
        }
    }

    public void clear() {
        mMap.clear();
        mTempMap.clear();
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

            int count = cursor.getCount();
            if (count == 0) {
                onRemoved(uri);
                return;
            }

            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                Setting setting = new Setting(values);
                onInsertedOrUpdated(uri, setting);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void onRemoved(Uri uri) {
        Setting setting = mMap.get(uri);
        remove(uri);
        dispatchRemoved(setting);
    }

    private void dispatchRemoved(Setting setting) {
        for (CacheListener l : mCacheListeners) {
            l.onRemoved(setting);
        }
    }

    private void onInsertedOrUpdated(Uri uri, Setting setting) {
        put(uri, setting);
        dispatchInsertedOrUpdated(setting);
    }

    private void dispatchInsertedOrUpdated(Setting setting) {
        for (CacheListener l : mCacheListeners) {
            l.onInsertedOrUpdated(setting);
        }
    }

    public interface CacheListener {
        public void onRemoved(Setting setting);
        public void onInsertedOrUpdated(Setting setting);
    }
}
