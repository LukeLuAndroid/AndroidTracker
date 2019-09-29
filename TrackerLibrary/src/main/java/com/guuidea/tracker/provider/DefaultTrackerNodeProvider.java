package com.guuidea.tracker.provider;

import com.guuidea.annotation.Tracker;

import java.lang.reflect.Method;

public class DefaultTrackerNodeProvider implements TrackerNodeProvider {

    @Override
    public TrackerNode newInstance(Object current, String cls, String methodName, String[] paramCls, String[] groups, String tag, Object... args) {
        return new DefaultTrackerNode(current, cls, methodName, paramCls, groups, tag, args);
    }

    @Override
    public TrackerNode newInstance(Tracker tracker, Object current, String cls, String methodName, String[] paramCls, Method method, Object... args) {
        if (tracker != null) {
            return new ReflectTrackerNode(current, cls, methodName, paramCls, tracker.group(), tracker.tag(), method, args);
        } else {
            return null;
        }
    }
}
