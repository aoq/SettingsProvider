/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider.task;

import com.aokyu.dev.settings.provider.task.AbstractTask.TaskListener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SerialExecutor implements TaskListener {

    private static final String DEFAULT_TASK_NAME = "Executor";

    private static final long SHUTDOWN_TIMEOUT_MILLIS = 1000;

    private ExecutorService mExecutor;

    private List<String> mIdList = new CopyOnWriteArrayList<String>();
    private Map<String, Future<?>> mTaskMap = new ConcurrentHashMap<String, Future<?>>();

    private String mTaskName;

    public SerialExecutor() {
        this(DEFAULT_TASK_NAME);
    }

    public SerialExecutor(String taskName) {
        mTaskName = taskName;
        ThreadFactory threadFactory = null;
        if (mTaskName == null) {
            threadFactory = Executors.defaultThreadFactory();
        } else {
            threadFactory = new NamedThreadFactory(mTaskName);
        }

        mExecutor = Executors.newSingleThreadExecutor(threadFactory);
    }

    public void destroy() {
        try {
            mExecutor.shutdown();
            if (mExecutor.awaitTermination(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                mExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            mExecutor.shutdownNow();
        }
    }

    public String getTaskName() {
        return mTaskName;
    }

    public String execute(AbstractTask task) {
        UUID uuid = UUID.randomUUID();
        String taskId = uuid.toString();
        task.setId(taskId);
        task.setListener(this);
        mIdList.add(taskId);
        Future<?> futureTask = mExecutor.submit(task);
        mTaskMap.put(taskId, futureTask);
        return taskId;
    }

    @Override
    public void onStart(String taskId) {}

    @Override
    public void onCancel(String taskId) {}

    @Override
    public void onComplete(String taskId) {
        mIdList.remove(taskId);
        mTaskMap.remove(taskId);
    }

}
