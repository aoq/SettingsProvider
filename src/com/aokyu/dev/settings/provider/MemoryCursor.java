/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2014 Yu AOKI.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 *
 * NOTE: This file has been modified by Yu AOKI.
 * Modifications are licensed under the License.
 */

package com.aokyu.dev.settings.provider;

import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;

public class MemoryCursor extends AbstractWindowedCursor {

    private final String[] mColumnNames;

    public MemoryCursor(String name, String[] columnNames) {
        setWindow(new CursorWindow(name));
        if (columnNames != null) {
            mColumnNames = columnNames.clone();
        } else {
            mColumnNames = null;
        }
    }

    public void fillFromCursor(Cursor cursor) {
        // "Resource leak" is displayed in Coverity,
        // but getwindow() does not need to close here.
        cursorFillWindow(cursor, 0, getWindow());
    }

    @Override
    public int getCount() {
        // "Resource leak" is displayed in Coverity,
        // but getwindow() does not need to close here.
        return getWindow().getNumRows();
    }

    @Override
    public String[] getColumnNames() {
        if (mColumnNames != null) {
            return mColumnNames.clone();
        } else {
            return null;
        }
    }

    private void cursorFillWindow(final Cursor cursor, int position, final CursorWindow window) {
        if (position < 0 || position >= cursor.getCount()) {
            return;
        }

        final int oldPos = cursor.getPosition();
        final int numColumns = cursor.getColumnCount();
        window.clear();
        window.setStartPosition(position);
        window.setNumColumns(numColumns);
        if (cursor.moveToPosition(position)) {
            do {
                if (!window.allocRow()) {
                    break;
                }

                for (int i = 0; i < numColumns; i++) {
                    final int type = cursor.getType(i);
                    final boolean success;
                    switch (type) {
                        case Cursor.FIELD_TYPE_NULL:
                            success = window.putNull(position, i);
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            success = window.putLong(cursor.getLong(i), position, i);
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            success = window.putDouble(cursor.getDouble(i), position, i);
                            break;
                        case Cursor.FIELD_TYPE_BLOB: {
                            final byte[] value = cursor.getBlob(i);
                            success = value != null ? window.putBlob(value, position, i)
                                    : window.putNull(position, i);
                            break;
                        }
                        default: // assume value is convertible to String
                        case Cursor.FIELD_TYPE_STRING: {
                            final String value = cursor.getString(i);
                            success = value != null ? window.putString(value, position, i)
                                    : window.putNull(position, i);
                            break;
                        }
                    }
                    if (!success) {
                        window.freeLastRow();
                        break;
                    }
                }
                position += 1;
            } while (cursor.moveToNext());
        }
        cursor.moveToPosition(oldPos);
    }
}
