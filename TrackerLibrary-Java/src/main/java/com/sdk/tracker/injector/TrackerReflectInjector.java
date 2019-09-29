package com.sdk.tracker.injector;

import android.util.Log;

import com.sdk.tracker.Trackers;
import com.sdk.tracker.core.Command;
import com.sdk.tracker.provider.TrackerNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerReflectInjector {

    private static Map<String, Boolean> methodMap = new HashMap<>();
    protected static Map<String, String> groupMap = new HashMap<>();

    /**
     * 是否允许插装
     *
     * @param methodClass
     * @param methodName
     * @param args
     * @return
     */
    public static boolean isEnable(String methodClass, String methodName, String[] paramNames, String[] groups, Object... args) {
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        if (stacks.length > 1) {
            methodClass = stacks[1].getClassName();
            methodName = stacks[1].getMethodName();
        }

        String methodId = Trackers.getMethodInfo(methodClass, methodName, paramNames);

        boolean isEnable = false;
        String group = Command.instance().getGroupPermit(groups);
        if (!"".equals(group)) {
            groupMap.put(methodId, group);
            Command.instance().addGetLogCommand(methodId);
            return true;
        }

        if (Command.instance().contains(methodId, methodClass)) {
            isEnable = true;
        }

        if (isEnable) {
            Boolean result = methodMap.get(methodClass + "." + methodName);
            return result == null || result.booleanValue();
        } else {
            methodMap.remove(methodClass + "." + methodName);
            return false;
        }
    }

    /**
     * 反射调用当前methodName方法
     *
     * @param o
     * @param methodClass
     * @param methodName
     * @param paramClassNames 参数的Class信息
     * @param args
     * @return
     */
    public static Object invoke(Object o, String methodClass, String methodName, String[] paramClassNames, String[] paramNames, Object... args) {
        long startTime = System.currentTimeMillis();

        //获取堆栈信息
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        if (stacks.length > 1) {
            methodClass = stacks[1].getClassName();
            methodName = stacks[1].getMethodName();
        }

        String methodId = Trackers.getMethodInfo(methodClass, methodName, paramNames);
        String group = groupMap.get(methodId);

        TrackerNode trackerNode = Trackers.instance().getTrackerNode(o, methodClass, methodName, paramClassNames, paramNames, group, args);
        if (trackerNode == null) {
            return null;
        }
        
        List<String> scripts = Trackers.instance().getScript(trackerNode, methodClass, methodName, paramNames);
        for (String script : scripts) {
            trackerNode.execute(script, "before", null, args);
        }

        methodMap.put(methodClass + "." + methodName, false);

        Object result = null;
        if (trackerNode != null) {
            TrackerNode.TrackerInvoker invoker = trackerNode.invoker(o);
            if (invoker != null) {
                result = invoker.invoke();
            }
        }

        methodMap.remove(methodClass + "." + methodName);

        for (String script : scripts) {
            trackerNode.execute(script, "after", result, args);
        }
        if (Trackers.instance().isDebug()) {
            Log.e("Tracker", "invoke cost=" + (System.currentTimeMillis() - startTime) + ",method=" + methodName);
        }
        return result;
    }


}
