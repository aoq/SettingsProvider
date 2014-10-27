package com.aokyu.dev.settings.provider;

import com.aokyu.dev.settings.provider.task.AbstractTask;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* package */ class SettingsLoader extends AbstractTask {

    private ContentResolver mContentResolver;

    private LoaderListener mListener;

    public SettingsLoader(Context context, LoaderListener l) {
        mContentResolver = context.getContentResolver();
        mListener = l;
    }

    @Override
    public void execute() throws InterruptedException {
        Map<Uri, Setting> map = new ConcurrentHashMap<Uri, Setting>();
        Cursor cursor = null;
        try {
            cursor = SettingLoader.loadAll(mContentResolver);

            if (cursor == null) {
                return;
            }

            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                CursorUtils.cursorRowToContentValues(cursor, values);
                Setting setting = new Setting(values);
                long settingId = setting.getId();
                Uri uri = Uri.withAppendedPath(SettingsContract.CONTENT_URI,
                        String.valueOf(settingId));
                map.put(uri, setting);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            if (mListener != null) {
                mListener.onLoadFinished(map);
            }
        }
    }

    public interface LoaderListener {
        public void onLoadFinished(Map<Uri, Setting> map);
    }
}
