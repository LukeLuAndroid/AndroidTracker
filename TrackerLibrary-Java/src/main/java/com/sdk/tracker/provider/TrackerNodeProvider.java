package com.sdk.tracker.provider;


import com.sdk.annotation.Tracker;

import java.lang.reflect.Method;

public interface TrackerNodeProvider {
    TrackerNode newInstance(Object current, String cls, String methodName, String[] paramCls, String[] paramNames, String group, String tag, Object... args);

    TrackerNode newInstance(Tracker tracker, Object current, String cls, String methodName, String[] paramCls, String[] paramNames, String group, Method method, Object... args);
}