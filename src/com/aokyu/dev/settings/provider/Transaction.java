/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transaction {

    private final boolean mBatch;
    private List<SQLiteDatabase> mDatabasesForTransaction;
    private Map<String, SQLiteDatabase> mDatabaseMap;
    private boolean mDirty;
    private boolean mYieldFailed;

    private List<Uri> mDirtyUris = new ArrayList<Uri>();

    public Transaction(boolean batch) {
        mBatch = batch;
        mDatabasesForTransaction = new ArrayList<SQLiteDatabase>();
        mDatabaseMap = new HashMap<String, SQLiteDatabase>();
        mDirty = false;
    }

    public boolean isBatch() {
        return mBatch;
    }

    public boolean isDirty() {
        return mDirty;
    }

    public void markDirty() {
        mDirty = true;
    }

    public void markDirty(Uri uri) {
        if (uri != null) {
            mDirtyUris.add(uri);
        }
        markDirty();
    }

    public void markDirty(List<Uri> uris) {
        if (uris != null && !uris.isEmpty()) {
            mDirtyUris.addAll(uris);
        }
        markDirty();
    }

    public List<Uri> getDirtyUris() {
        return mDirtyUris;
    }

    public void markYieldFailed() {
        mYieldFailed = true;
    }

    public void startTransactionForDb(SQLiteDatabase db, String tag, SQLiteTransactionListener l) {
        if (!hasDbInTransaction(tag)) {
            mDatabasesForTransaction.add(db);
            mDatabaseMap.put(tag, db);
            if (l != null) {
                db.beginTransactionWithListener(l);
            } else {
                db.beginTransaction();
            }
        }

    }

    public boolean hasDbInTransaction(String tag) {
        return mDatabaseMap.containsKey(tag);
    }

    public SQLiteDatabase getDbForTag(String tag) {
        return mDatabaseMap.get(tag);
    }

    public SQLiteDatabase removeDbForTag(String tag) {
        SQLiteDatabase db = mDatabaseMap.get(tag);
        mDatabaseMap.remove(tag);
        mDatabasesForTransaction.remove(db);
        return db;
    }

    public void markSuccessful(boolean callerIsBatch) {
        if (!mBatch || callerIsBatch) {
            for (SQLiteDatabase db : mDatabasesForTransaction) {
                db.setTransactionSuccessful();
            }
        }
    }

    public void finish(boolean callerIsBatch) {
        if (!mBatch || callerIsBatch) {
            for (SQLiteDatabase db : mDatabasesForTransaction) {
                if (mYieldFailed && !db.isDbLockedByCurrentThread()) {
                    continue;
                }
                db.endTransaction();
            }
            mDatabasesForTransaction.clear();
            mDatabaseMap.clear();
            mDirty = false;
            if (mDirtyUris != null) {
                mDirtyUris.clear();
            }
        }
    }

}
