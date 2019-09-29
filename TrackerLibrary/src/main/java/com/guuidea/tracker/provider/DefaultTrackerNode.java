package com.guuidea.tracker.provider;

import com.guuidea.tracker.core.CommandConstants;

import java.util.Map;

public class DefaultTrackerNode extends TrackerNode {
    protected String[] paramClassNames;

    public DefaultTrackerNode(Object current, String cls, String methodName, String[] paramCls, String[] group, String tag, Object... args) {
        super(current, cls, methodName, group, tag, args);
        this.paramClassNames = paramCls;
    }

    @Override
    public Object getParameter(String name, String def) {
        return null;
    }

    @Override
    public TrackerInvoker invoker(Object input) {
        return null;
    }

    @Override
    public void execute(String script, String type, Object result, Object... args) {
        String[] commands = script.split("#");
        if (commands.length >= 2) {
            if (type != null && type.equals(commands[0])) {
                String action = commands[1];
                if (action != null) {
                    if (action.startsWith(CommandConstants.GET_LOG)) {
                        if (getAddListener() != null && getAddListener().addTracker(className, methodName, args)) {
                            return;
                        }
                        if (mHandler != null) {
                            mHandler.getLog(className, methodName, paramClassNames, labels, script);
                        }
                    } else if (action.startsWith(CommandConstants.GET_ARGS)) {
                        if (mHandler != null) {
                            Object value = mHandler.getParameters(commands[2], args);
                        }
                    } else if (action.startsWith(CommandConstants.GET_RESULT)) {
                        if (mHandler != null) {
                            Object value = mHandler.getResult(result, commands[2]);
                        }
                    }
                }
            }
        }
    }
}
