package com.sdk.tracker.provider;

import com.sdk.tracker.core.Command;
import com.sdk.tracker.core.CommandConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectTrackerNode extends DefaultTrackerNode {
    private Method method;

    public ReflectTrackerNode(Object current, String cls, String methodName, String[] paramCls, String[] paramNames, String group, String tag, Method method, Object... args) {
        super(current, cls, methodName, paramCls, paramNames, group, tag, args);
        this.method = method;
    }

    @Override
    public Object getParameter(String name, String def) {
        return null;
    }

    @Override
    public TrackerInvoker invoker(Object input) {
        return new TrackerInvokerImpl(input, method, args);
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

    private static class TrackerInvokerImpl implements TrackerInvoker {
        private Method mMethod;
        private Object mInput;
        private Object[] args;

        public TrackerInvokerImpl(Object input, Method method, Object[] args) {
            this.mMethod = method;
            this.args = args;
            this.mInput = input;
        }

        @Override
        public Object invoke() {
            try {
                if (mMethod != null && mInput != null) {
                    mMethod.setAccessible(true);
                    return mMethod.invoke(mInput, args);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
