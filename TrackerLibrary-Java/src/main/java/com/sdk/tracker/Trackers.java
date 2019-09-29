package com.sdk.tracker;

import com.sdk.annotation.Tracker;
import com.sdk.tracker.core.Command;
import com.sdk.tracker.core.CommandHandler;
import com.sdk.tracker.core.Strategy;
import com.sdk.tracker.provider.DefaultTrackerNodeProvider;
import com.sdk.tracker.provider.TrackerNode;
import com.sdk.tracker.provider.TrackerNodeProvider;
import com.sdk.tracker.util.TrackerUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trackers {

    static final String TAG = "Trackers";

    private static CommandHandler handler = new CommandHandler();
    private static boolean isDebug = false;
    private TrackerAddListener trackerAddListener;
    private TrackerUploadListener trackerUploadListener;
    private TrackerStoreListener trackerStoreListener;
    private static TrackerNodeProvider DEFAULT_PROVIDER = new DefaultTrackerNodeProvider();
    private TrackerNodeProvider mTrackerNodeProvider;
    private Strategy DEFAULT_STRATEGY = new Strategy();
    private Strategy strategy;

    private static Trackers _instance;

    public static Trackers instance() {
        if (_instance == null) {
            synchronized (Trackers.class) {
                if (_instance == null) {
                    _instance = new Trackers();
                }
            }
        }
        return _instance;
    }

    public Trackers setTrackerAddListener(TrackerAddListener listener) {
        trackerAddListener = listener;
        return this;
    }

    public Trackers setTrackerUploadListener(TrackerUploadListener listener) {
        trackerUploadListener = listener;
        return this;
    }

    public Trackers setTrackerStoreListener(TrackerStoreListener listener) {
        trackerStoreListener = listener;
        return this;
    }

    private Strategy getDefaultStrategy() {
        return DEFAULT_STRATEGY;
    }

    public Strategy getStrategy() {
        if (strategy == null) {
            return getDefaultStrategy();
        }
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
    }

    public Trackers setDebug(boolean debug) {
        isDebug = debug;
        return this;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public TrackerStoreListener getTrackerStoreListener() {
        if (trackerStoreListener != null) {
            return trackerStoreListener;
        }
        return TrackerListenerFactory.getDefaultStoreListener();
    }

    public TrackerAddListener getTrackerAddListener() {
        return trackerAddListener;
    }

    public TrackerUploadListener getTrackerUploadListener() {
        if (trackerUploadListener != null) {
            return trackerUploadListener;
        }
        return TrackerListenerFactory.getDefaultUploadListener();
    }

    public void init() {
        //upload last info to server
        handler.init();

    }

    public Trackers setTrackerNodeProvider(TrackerNodeProvider provider) {
        mTrackerNodeProvider = provider;
        return this;
    }

    public TrackerNodeProvider getTrackerNodeProvider() {
        if (mTrackerNodeProvider == null) {
            return DEFAULT_PROVIDER;
        }
        return mTrackerNodeProvider;
    }

    public List<String> getScript(TrackerNode node, String methodClass, String methodName, String[] paramNames) {
        List<String> commands = new ArrayList<>();
        List<String> methodCmd = Command.instance().getCommandInfo(getMethodInfo(methodClass, methodName, paramNames));
        if (methodCmd != null) {
            commands.addAll(methodCmd);
        }
        if (node != null) {
            node.setCommands(commands);
        }
        return commands;
    }

    private static Map<String, Class> clsMap = new HashMap<>();

    public TrackerNode getTrackerNode(Object current, String methodClass, String methodName, String[] paramClassNames, String[] paramNames, String group, Object... args) {
        try {
            Class cls = Class.forName(methodClass);
            Class[] paramTypes = new Class[paramClassNames.length];
            for (int i = 0; i < paramClassNames.length; i++) {
                paramTypes[i] = clsMap.get(paramClassNames[i]);
                if (paramTypes[i] == null) {
                    paramTypes[i] = Class.forName(paramClassNames[i]);
                    clsMap.put(paramClassNames[i], paramTypes[i]);
                }
            }
            Method method = cls.getDeclaredMethod(methodName, paramTypes);

            Tracker tracker = null;
            if (method != null) {
                tracker = method.getAnnotation(Tracker.class);
                if (tracker == null) {
                    tracker = (Tracker) cls.getAnnotation(Tracker.class);
                }
            }
            if (tracker != null && method != null) {
                return getTrackerNodeProvider().newInstance(tracker, current, methodClass, methodName, paramClassNames, paramNames, group, method, args);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TrackerNode getTrackerNode(Object current, String cls, String methodName, String[] paramCls, String[] paramNames, String group, String tag, Object... args) {
        return getTrackerNodeProvider().newInstance(current, cls, methodName, paramCls, paramNames, group, tag, args);
    }

    public static String getMethodInfo(String methodClass, String methodName, String[] paramNames) {
        return TrackerUtils.getUniqueMethodId(methodClass, methodName, paramNames);
    }
}
