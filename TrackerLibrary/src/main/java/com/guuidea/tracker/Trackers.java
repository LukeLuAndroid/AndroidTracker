package com.guuidea.tracker;

import android.support.v4.util.ArrayMap;

import com.guuidea.annotation.Tracker;
import com.guuidea.tracker.library.BuildConfig;
import com.guuidea.tracker.core.Command;
import com.guuidea.tracker.core.CommandHandler;
import com.guuidea.tracker.provider.DefaultTrackerNodeProvider;
import com.guuidea.tracker.provider.TrackerNode;
import com.guuidea.tracker.provider.TrackerNodeProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Trackers {

    static final String TAG = "Trackers";

    private static CommandHandler handler = new CommandHandler();
    private static boolean isDebug = false;
    private TrackerAddListener trackerAddListener;
    private TrackerUploadListener trackerUploadListener;
    private static TrackerNodeProvider DEFAULT_PROVIDER = new DefaultTrackerNodeProvider();
    private TrackerNodeProvider mTrackerNodeProvider;

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

    public Trackers setDebug(boolean debug) {
        isDebug = debug && BuildConfig.DEBUG;
        return this;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public TrackerAddListener getTrackerAddListener() {
        return trackerAddListener;
    }

    public TrackerUploadListener getTrackerUploadListener() {
        return trackerUploadListener;
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

    public List<String> getScript(TrackerNode node, String methodClass, String methodName) {
        List<String> commands = new ArrayList<>();
        List<String> propCmd = Command.instance().getCommandInfo(methodClass);
        List<String> methodCmd = Command.instance().getCommandInfo(methodClass + "." + methodName);
        if (propCmd != null) {
            commands.addAll(propCmd);
        }
        if (methodCmd != null) {
            commands.addAll(methodCmd);
        }
        if (node != null) {
            node.setCommands(commands);
        }
        return commands;
    }

    private static ArrayMap<String, Class> clsMap = new ArrayMap<>();

    public TrackerNode getTrackerNode(Object current, String methodClass, String methodName, String[] paramClassNames, Object... args) {
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
                return getTrackerNodeProvider().newInstance(tracker, current, methodClass, methodName, paramClassNames, method, args);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TrackerNode getTrackerNode(Object current, String cls, String methodName, String[] paramCls, String[] groups, String tag, Object... args) {
        return getTrackerNodeProvider().newInstance(current, cls, methodName, paramCls, groups, tag, args);
    }

}
