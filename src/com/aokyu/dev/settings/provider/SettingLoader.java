package com.aokyu.dev.settings.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class SettingLoader {

    private static final String[] PROJECTION = {
        SettingsContract._ID,
        SettingsContract.KEY,
        SettingsContract.TYPE,
        SettingsContract.VALUE
    };

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
