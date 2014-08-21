/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider.task;

public abstract class AbstractTask implements Runnable {

    protected String mTaskId;
    protected TaskListener mListener;

    public AbstractTask() {}

    public abstract void execute() throws InterruptedException;

    /* package */ void setId(String id) {
        mTaskId = id;
    }

    /* package */ String getId() {
        return mTaskId;
    }

    /* package */ void setListener(TaskListener l) {
        mListener = l;
    }

    @Override
    public void run() {
        onStart(mTaskId);
        try {
            execute();
        } catch (InterruptedException e) {
            onCancel(mTaskId);
        } finally {
            onComplete(mTaskId);
        }
    }

    protected void onStart(String taskId) {
        if (mListener != null) {
            mListener.onStart(mTaskId);
        }
    }

    protected void onCancel(String taskId) {
        if (mListener != null) {
            mListener.onCancel(mTaskId);
        }
    }

    protected void onComplete(String taskId) {
        if (mListener != null) {
            mListener.onComplete(mTaskId);
        }
    }

    protected void throwInterruptedExceptionIfNeeded() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        } else {
            // Do nothing.
        }
    }

    /* package */ interface TaskListener {
        public void onStart(String taskId);
        public void onCancel(String taskId);
        public void onComplete(String taskId);

    }
}
