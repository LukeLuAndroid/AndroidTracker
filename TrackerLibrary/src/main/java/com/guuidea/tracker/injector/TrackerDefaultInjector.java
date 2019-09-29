package com.guuidea.tracker.injector;

import android.util.Log;

import com.guuidea.tracker.Trackers;
import com.guuidea.tracker.core.Command;
import com.guuidea.tracker.provider.TrackerNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerDefaultInjector {

    private static final String TAG = "TrackerDefaultInjector";
    private static Map<String, TrackerNode> nodeMap = new HashMap<>();

    /**
     * 是否允许插装
     *
     * @param methodClass
     * @param methodName
     * @param paramClasses
     * @param group        方法分组
     * @param args
     * @return
     */
    public static boolean isEnable(String methodClass, String methodName, String[] paramClasses, String[] group, Object... args) {
        if (Command.instance().isGroupPermit(group)) {
            return true;
        }

        if (Command.instance().contains(methodClass + "." + methodName, methodClass)) {
            return true;
        }

        return false;
    }

    public static void insertFront(Object o, String methodClass, String methodName, String[] paramClassNames, String[] groups, String tag, Object... args) {
        long startTime = System.currentTimeMillis();

        String methodInfo = getMethodInfo(methodClass, methodName, paramClassNames);
        TrackerNode trackerNode = Trackers.instance().getTrackerNode(o, methodClass, methodName, paramClassNames, groups, tag, args);
        nodeMap.put(methodInfo, trackerNode);

        List<String> scripts = Trackers.instance().getScript(trackerNode, methodClass, methodName);
        for (String script : scripts) {
            trackerNode.execute(script, "before", null, args);
        }

        Log.e(TAG, "insert front cost = " + (System.currentTimeMillis() - startTime)+ ",methodName=" + methodName);
    }

    public static void insertBack(Object o, String methodClass, String methodName, String[] paramClassNames, String[] groups, String tag, Object result, Object... args) {
        long startTime = System.currentTimeMillis();
        String methodInfo = getMethodInfo(methodClass, methodName, paramClassNames);

        TrackerNode node = nodeMap.get(methodInfo);
        if (node == null) {
            return;
        }
        List<String> scripts = Trackers.instance().getScript(node, methodClass, methodName);
        for (String script : scripts) {
            node.execute(script, "after", result, args);
        }

        nodeMap.remove(methodInfo);
        Log.e(TAG, "insert front back = " + (System.currentTimeMillis() - startTime)+ ",methodName=" + methodName);
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
