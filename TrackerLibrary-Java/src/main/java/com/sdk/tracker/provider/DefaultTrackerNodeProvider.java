package com.sdk.tracker.provider;

import com.sdk.annotation.Tracker;

import java.lang.reflect.Method;

public class DefaultTrackerNodeProvider implements TrackerNodeProvider {

    @Override
    public TrackerNode newInstance(Object current, String cls, String methodName, String[] paramCls, String[] paramNames, String groups, String tag, Object... args) {
        return new DefaultTrackerNode(current, cls, methodName, paramCls, paramNames, groups, tag, args);
}

    @Override
    public TrackerNode newInstance(Tracker tracker, Object current, String cls, String methodName, String[] paramCls, String[] paramNames, String group, Method method, Object... args) {
        if (tracker != null) {
            return new ReflectTrackerNode(current, cls, methodName, paramCls, paramNames, group, tracker.tag(), method, args);
        } else {
            return null;
        }
    }
}
