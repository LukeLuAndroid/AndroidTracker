package com.guuidea.tracker.injector;

import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.guuidea.tracker.Trackers;
import com.guuidea.tracker.core.Command;
import com.guuidea.tracker.provider.TrackerNode;

import java.util.List;

public class TrackerReflectInjector {

    private static ArrayMap<String, Boolean> methodMap = new ArrayMap<>();

    /**
     * 是否允许插装
     *
     * @param methodClass
     * @param methodName
     * @param paramClasses
     * @param args
     * @return
     */
    public static boolean isEnable(String methodClass, String methodName, String[] paramClasses, String[] group, Object... args) {
        if (!Command.instance().isGroupPermit(group) && !Command.instance().contains(methodClass + "." + methodName, methodClass)) {
            methodMap.remove(methodClass + "." + methodName);
            return false;
        }

        Boolean result = methodMap.get(methodClass + "." + methodName);
        return result == null || result.booleanValue();
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
    public static Object invoke(Object o, String methodClass, String methodName, String[] paramClassNames, String[] groups, String tag, Object... args) {
        List<String> propCmd = Command.instance().getCommandInfo(methodClass);
        List<String> methodCmd = Command.instance().getCommandInfo(methodClass + "." + methodName);

        //获取堆栈信息
        long startTime = System.currentTimeMillis();

        TrackerNode trackerNode = Trackers.instance().getTrackerNode(o, methodClass, methodName, paramClassNames, args);
        List<String> scripts = Trackers.instance().getScript(trackerNode, methodClass, methodName);
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
