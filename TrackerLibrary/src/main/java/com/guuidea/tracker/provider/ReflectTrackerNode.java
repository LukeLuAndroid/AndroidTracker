package com.guuidea.tracker.provider;

import com.guuidea.tracker.core.CommandConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectTrackerNode extends DefaultTrackerNode {
    private Method method;

    public ReflectTrackerNode(Object current, String cls, String methodName, String[] paramCls, String[] group, String tag, Method method, Object... args) {
        super(current, cls, methodName, paramCls, group, tag, args);
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
        super.execute(script,type,result,args);
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
