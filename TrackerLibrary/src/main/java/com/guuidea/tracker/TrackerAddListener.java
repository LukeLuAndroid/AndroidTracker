package com.guuidea.tracker;

public interface TrackerAddListener {
    /**
     * 添加埋点
     */
    boolean addTracker(String className, String methodName, Object... args);
}
