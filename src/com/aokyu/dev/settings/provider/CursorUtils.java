/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Static utility methods for dealing with {@link Cursor}s.
 */
public class CursorUtils {

    private CursorUtils() {}

    /**
     * Reads the contents of a cursor row and store them in a {@link ContentValues}.
     * A content is not stored if null.
     * @param cursor The cursor to read from.
     * @param values The {@link ContentValues} to put the row into.
     */
    public static void  cursorRowToContentValues(Cursor cursor, ContentValues values) {
        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
            if (!cursor.isNull(i)) {
                int type = cursor.getType(i);
                switch (type) {
                    case Cursor.FIELD_TYPE_FLOAT:
                        values.put(columns[i], cursor.getFloat(i));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        values.put(columns[i], cursor.getInt(i));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        values.put(columns[i], cursor.getString(i));
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        values.put(columns[i], cursor.getBlob(i));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                    default:
                        break;
                }
            }
        }
    }
}
