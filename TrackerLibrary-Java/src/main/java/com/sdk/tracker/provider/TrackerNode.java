package com.sdk.tracker.provider;

import com.sdk.tracker.TrackerAddListener;
import com.sdk.tracker.Trackers;
import com.sdk.tracker.core.CommandHandler;

import java.util.List;

public abstract class TrackerNode {
    protected String className;
    protected String methodName;
    protected String group;
    protected Object[] args;
    protected Object mCurrent;
    protected String labels;

    protected CommandHandler mHandler;
    protected List<String> mCommands;

    public TrackerNode(Object current, String cls, String methodName, String group, String tag, Object... args) {
        this.mCurrent = current;
        this.className = cls;
        this.methodName = methodName;
        this.group = group;
        this.labels = tag;
        this.args = args;
        mHandler = new CommandHandler();
    }

    public TrackerAddListener getAddListener() {
        return Trackers.instance().getTrackerAddListener();
    }

    public List<String> getCommands() {
        return mCommands;
    }

    public void setCommands(List<String> commands) {
        this.mCommands = commands;
    }

    public abstract Object getParameter(String name, String def);

    public abstract TrackerInvoker invoker(Object input);

    public abstract void execute(String script, String type, Object result, Object... args);

    public interface TrackerInvoker {
        Object invoke();
    }

    public static class PropInfo {
        public String name;
        public Object key;

        public PropInfo() {
        }

        public PropInfo(String name, Object key) {
            this.name = name;
            this.key = key;
        }
    }
}