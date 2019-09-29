package com.guuidea.tracker.provider;

import com.guuidea.annotation.Tracker;

import java.lang.reflect.Method;

public interface TrackerNodeProvider {
    TrackerNode newInstance(Object current, String cls, String methodName, String[] paramCls, String[] groups, String tag, Object... args);

    TrackerNode newInstance(Tracker tracker, Object current, String cls, String methodName, String[] paramCls, Method method, Object... args);
}