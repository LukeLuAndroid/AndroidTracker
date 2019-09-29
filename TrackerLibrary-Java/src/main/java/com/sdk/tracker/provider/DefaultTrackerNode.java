package com.sdk.tracker.provider;

import com.sdk.tracker.Trackers;
import com.sdk.tracker.core.Command;
import com.sdk.tracker.core.CommandConstants;

public class DefaultTrackerNode extends TrackerNode {
    protected String[] paramNames;
    protected String[] paramClassNames;

    public DefaultTrackerNode(Object current, String cls, String methodName, String[] paramCls, String[] paramNames, String group, String tag, Object... args) {
        super(current, cls, methodName, group, tag, args);
        this.paramNames = paramNames;
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
                        if (getAddListener() != null && getAddListener().addTracker(className, methodName, group, labels, args)) {
                            removeMethodCommand(script);
                            return;
                        }
                        if (mHandler != null) {
                            mHandler.getLog(className, methodName, paramNames, group, labels, script, args);
                        }
                    } else if (action.startsWith(CommandConstants.GET_ARGS)) {
                        try {
                            if (mHandler != null && commands.length > 2) {
                                int position = Integer.valueOf(commands[2]);
                                if (args.length > position && commands.length > 3) {
                                    Object value = mHandler.getParameters(position, commands[3], args);
                                    removeMethodCommand(script);
                                    mHandler.uploadValue(className, methodName, paramNames, script, group, value, args);
                                    return;
                                }
                            } else {
                                removeMethodCommand(script);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            removeMethodCommand(script);
                        }
                        Command.instance().restoreCommand();
                    } else if (action.startsWith(CommandConstants.GET_RESULT)) {
                        if (mHandler != null && commands.length > 2) {
                            Object value = mHandler.getResult(result, commands[2]);
                            removeMethodCommand(script);
                            mHandler.uploadValue(className, methodName, paramNames, script, group, value, args);
                            return;
                        } else {
                            removeMethodCommand(script);
                        }
                        Command.instance().restoreCommand();
                    } else if (action.startsWith(CommandConstants.GET_PROP)) {
                        if (mHandler != null && commands.length > 2) {
                            Object value = mHandler.getProperties(className, mCurrent, commands[2]);
                            removeMethodCommand(script);
                            mHandler.uploadValue(className, methodName, paramNames, script, group, value, args);
                            return;
                        } else {
                            removeMethodCommand(script);
                        }
                        Command.instance().restoreCommand();
                    }
                }
            }
        }
    }

    //remove method command
    protected void removeMethodCommand(String script) {
        Command.instance().removeCommand(Trackers.getMethodInfo(className, methodName, paramNames), script);
    }
}
