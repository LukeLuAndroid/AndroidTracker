package com.sdk.tracker.injector;

import android.util.Log;

import com.sdk.tracker.Trackers;
import com.sdk.tracker.core.Command;
import com.sdk.tracker.provider.TrackerNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerDefaultInjector {

    private static final String TAG = "TrackerDefaultInjector";
    private static Map<String, TrackerNode> nodeMap = new HashMap<>();
    protected static Map<String, String> groupMap = new HashMap<>();

    /**
     * 是否允许插装
     *
     * @param methodClass
     * @param methodName
     * @param paramNames
     * @param groups      方法分组
     * @return
     */
    public static boolean isEnable(String methodClass, String methodName, String[] paramNames, String[] groups) {
        StackTraceElement[] traceElements = new Throwable().getStackTrace();
        if (traceElements.length > 1) {
            methodClass = traceElements[1].getClassName();
            methodName = traceElements[1].getMethodName();
        }

        String group = Command.instance().getGroupPermit(groups);
        if (!"".equals(group)) {
            String methodId = Trackers.getMethodInfo(methodClass, methodName, paramNames);
            groupMap.put(methodId, group);
            Command.instance().addGetLogCommand(methodId);
            return true;
        }

        if (Command.instance().contains(Trackers.getMethodInfo(methodClass, methodName, paramNames), methodClass)) {
            return true;
        }

        return false;
    }

    public static void insertFront(Object o, String methodClass, String methodName, String[] paramNames, String[] groups, String tag, Object... args) {
        long startTime = System.currentTimeMillis();

        StackTraceElement[] traceElements = new Throwable().getStackTrace();
        if (traceElements.length > 1) {
            methodClass = traceElements[1].getClassName();
            methodName = traceElements[1].getMethodName();
        }

        String methodInfo = getMethodInfo(methodClass, methodName, paramNames);

        String group = groupMap.get(methodInfo);

        TrackerNode trackerNode = Trackers.instance().getTrackerNode(o, methodClass, methodName, null, paramNames, group, tag, args);
        nodeMap.put(methodInfo, trackerNode);

        List<String> scripts = Trackers.instance().getScript(trackerNode, methodClass, methodName, paramNames);
        for (String script : scripts) {
            trackerNode.execute(script, "before", null, args);
        }

        if (Trackers.instance().isDebug()) {
            Log.e(TAG, "insert front cost = " + (System.currentTimeMillis() - startTime) + ",methodName=" + methodName);
        }
    }

    public static void insertBack(Object o, String methodClass, String methodName, String[] paramNames, String[] groups, String tag, Object result, Object... args) {
        long startTime = System.currentTimeMillis();

        StackTraceElement[] traceElements = new Throwable().getStackTrace();
        if (traceElements.length > 1) {
            methodClass = traceElements[1].getClassName();
            methodName = traceElements[1].getMethodName();
        }

        String methodInfo = getMethodInfo(methodClass, methodName, paramNames);

        String group = groupMap.get(methodInfo);
        TrackerNode node = nodeMap.get(methodInfo);
        if (node == null) {
            node = Trackers.instance().getTrackerNode(o, methodClass, methodName, null, paramNames, group, tag, args);
        }

        List<String> scripts = Trackers.instance().getScript(node, methodClass, methodName, paramNames);
        for (String script : scripts) {
            node.execute(script, "after", result, args);
        }

        nodeMap.remove(methodInfo);
        groupMap.remove(methodClass + "." + methodName);
        if (Trackers.instance().isDebug()) {
            Log.e(TAG, "insert back cost = " + (System.currentTimeMillis() - startTime) + ",methodName=" + methodName);
        }
    }

    /**
     * 获取方法唯一标志信息
     *
     * @param methodClass
     * @param methodName
     * @param paramClassNames
     * @return
     */
    private static String getMethodInfo(String methodClass, String methodName, String[] paramClassNames) {
        StringBuilder buffer = new StringBuilder(methodClass);
        buffer.append(".").append(methodName).append("(");
        for (int i = 0; i < paramClassNames.length; i++) {
            if (i != 0) {
                buffer.append(", ");
            }
            buffer.append(paramClassNames[i]);
        }
        buffer.append(")");
        return buffer.toString();
    }
}
