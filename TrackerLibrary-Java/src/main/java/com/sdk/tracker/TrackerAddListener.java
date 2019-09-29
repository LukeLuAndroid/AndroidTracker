package com.sdk.tracker;

public interface TrackerAddListener {
    /**
     * 添加埋点
     */
    boolean addTracker(String className, String methodName, String group, String tag, Object... args);
}
