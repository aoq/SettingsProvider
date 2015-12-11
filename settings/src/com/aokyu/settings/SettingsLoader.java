/*
 * Copyright (c) 2015 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.settings;

import com.aokyu.settings.provider.SettingsContract;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* package */ class SettingsLoader {

    private static final String[] PROJECTION = {
        SettingsContract._ID,
        SettingsContract.KEY,
        SettingsContract.TYPE,
        SettingsContract.VALUE
    };

    private static final String SELECTION = SettingsContract.KEY + "=?";

    /**
     * This clause is used to speed up queries.
     */
    private static final String SORT_ORDER = SettingsContract._ID + " DESC LIMIT 1";

    private SettingsLoader() {}

    /**
     * Loads the {@link Cursor} of the {@link Uri}.
     *
     * @param resolver The {@link ContentResolver}.
     * @param uri The {@link Uri} to use for the query.
     * @return the {@link Cursor} of the {@link Uri}.
     */
    public static Cursor load(ContentResolver resolver, Uri uri) {
        return resolver.query(uri, PROJECTION, null, null, SORT_ORDER);
    }

    public static Cursor loadCursor(ContentResolver resolver, Uri settingUri) {
        return resolver.query(settingUri, PROJECTION, null, null, SORT_ORDER);
    }

    /**
     * Loads the {@link com.aokyu.settings.Setting} for the key.
     *
     * @param resolver The {@link ContentResolver}.
     * @return the {@link com.aokyu.settings.Setting} for the key.
     */
    public static Setting load(ContentResolver resolver, String key) {
        Setting setting = null;
        Cursor cursor = null;
        try {
            cursor = resolver.query(SettingsContract.CONTENT_URI, PROJECTION,
                    SELECTION, new String[] { key }, SORT_ORDER);

            if (cursor == null) {
                return null;
            }

            while (cursor.moveToNext()) {
                setting = Setting.cursorRowToSetting(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return setting;
    }

    public static Cursor loadCursor(ContentResolver resolver, String key) {
        return resolver.query(SettingsContract.CONTENT_URI, PROJECTION,
                SELECTION, new String[] { key }, SORT_ORDER);
    }

    /**
     * Loads the {@link Setting}s on the database.
     *
     * @param resolver The {@link ContentResolver}.
     * @return the {@link Setting}s on the database.
     */
    public static Map<Uri, Setting> loadAll(ContentResolver resolver) {
        Map<Uri, Setting> map = new ConcurrentHashMap<Uri, Setting>();
        Cursor cursor = null;
        try {
            cursor = resolver.query(SettingsContract.CONTENT_URI, PROJECTION, null, null, null);
            if (cursor == null) {
                return null;
            }

            while (cursor.moveToNext()) {
                Setting setting = Setting.cursorRowToSetting(cursor);
                long id = setting.getId();
                Uri uri = Uri.withAppendedPath(SettingsContract.CONTENT_URI, String.valueOf(id));
                map.put(uri, setting);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return map;
    }
}
