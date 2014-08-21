/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class SettingsContract implements BaseColumns {

    /**
     * Cannot be instantiated.
     */
    private SettingsContract() {}

    /**
     * The authority for the settings provider.
     */
    public static final String AUTHORITY = "com.aokyu.dev.settings";

    /**
     * An URI to the authority for the settings provider.
     */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * The content:// style URI for this table.
     */
    public static final Uri CONTENT_URI =
            Uri.withAppendedPath(AUTHORITY_URI, "settings");

    /**
     * The MIME type of the results from {@link #CONTENT_URI}.
     */
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/vnd.aokyu.settings";

    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/vnd.aokyu.settings";

    /**
     * @see BaseColumns#_ID
     */
    public static final String _ID = "_id";

    /**
     * The key of a setting.
     * <P>Type: TEXT</P>
     */
    public static final String KEY = "key";

    /**
     * The type of the value. The type is a string representation of class names.
     * <P>Type: TEXT</P>
     */
    public static final String TYPE = "type";

    /**
     * The value of the mapping with the specified key.
     * <P>Type: TEXT</P>
     */
    public static final String VALUE = "value";
}
