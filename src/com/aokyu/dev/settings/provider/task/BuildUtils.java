/*
 * Copyright (c) 2014 Yu AOKI
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.aokyu.dev.settings.provider.task;

import android.os.Build;

/**
 * Static utility methods for the current build.
 * Unlike {@link Build.VERSION_CODES}.*, you can use the methods on any SDK environment.
 */
public class BuildUtils {

    private static final int ICE_CREAM_SANDWICH = 14;
    private static final int ICE_CREAM_SANDWICH_MR1 = 15;
    private static final int JELLY_BEAN = 16;
    private static final int JELLY_BEAN_MR1 = 17;
    private static final int JELLY_BEAN_MR2 = 18;
    private static final int KITKAT = 19;

    private BuildUtils() {}

    public static int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static String getBuildBrand() {
        return Build.BRAND;
    }

    public static String getBuildModel() {
        return Build.MODEL;
    }

    public static String getBuildId() {
        return Build.ID;
    }

    public static String getBuildType() {
        return Build.TYPE;
    }

    public static boolean requiresSdkVersion(int version) {
        if (Build.VERSION.SDK_INT >= version) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean requiresIceCreamSandwich() {
        return requiresSdkVersion(ICE_CREAM_SANDWICH);
    }

    public static boolean requiresIceCreamSandwichMr1() {
        return requiresSdkVersion(ICE_CREAM_SANDWICH_MR1);
    }

    public static boolean requiresJellyBean() {
        return requiresSdkVersion(JELLY_BEAN);
    }

    public static boolean requiresJellyBeanMr1() {
        return requiresSdkVersion(JELLY_BEAN_MR1);
    }

    public static boolean requiresJellyBeanMr2() {
        return requiresSdkVersion(JELLY_BEAN_MR2);
    }

    public static boolean requiresKitKat() {
        return requiresSdkVersion(KITKAT);
    }
}
