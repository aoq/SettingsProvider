/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider.task;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {

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
