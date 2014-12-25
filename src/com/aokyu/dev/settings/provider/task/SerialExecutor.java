/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider.task;

import com.aokyu.dev.settings.provider.task.AbstractTask.TaskListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SerialExecutor {

    private static final String DEFAULT_TASK_NAME = "Executor";

    private static final long SHUTDOWN_TIMEOUT_MILLIS = 1000;

    private ExecutorService mExecutor;
    private TaskManager mTaskManager;

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
        mTaskManager = new TaskManager();
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
        task.setListener(mTaskManager);
        mTaskManager.addTask(taskId, mExecutor.submit(task));
        return taskId;
    }

    private static class TaskManager implements TaskListener {

        private Map<String, Future<?>> mTaskMap = new ConcurrentHashMap<String, Future<?>>();

        public TaskManager() {}

        public void addTask(String taskId, Future<?> futureTask) {
            mTaskMap.put(taskId, futureTask);
        }

        @Override
        public void onStart(String taskId) {}

        @Override
        public void onCancel(String taskId) {
            mTaskMap.remove(taskId);
        }

        @Override
        public void onComplete(String taskId) {
            mTaskMap.remove(taskId);
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {

        private String mName;
        private final ThreadGroup mGroup;

        public NamedThreadFactory(String name) {
            mName = name;
            Thread currentThread = Thread.currentThread();
            mGroup = currentThread.getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread task = new Thread(mGroup, r, mName);
            if (task.isDaemon()) {
                task.setDaemon(false);
            }

            int priority = task.getPriority();
            if (priority != Thread.NORM_PRIORITY) {
                task.setPriority(Thread.NORM_PRIORITY);
            }
            return task;
        }
    }

}
