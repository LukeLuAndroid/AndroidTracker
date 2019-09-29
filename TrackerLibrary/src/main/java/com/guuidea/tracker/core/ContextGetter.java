package com.guuidea.tracker.core;

import android.content.Context;

import java.lang.reflect.Method;

public class ContextGetter {

    private static Context mContext;

    public static Context getContext() {
        if (mContext == null) {
            try {
                Class cls = Class.forName("android.app.ActivityThread");
                Method method = cls.getDeclaredMethod("currentActivityThread");
                Object currentThread = method.invoke(null);
                Method appMethod = cls.getDeclaredMethod("getApplication");
                mContext = (Context) appMethod.invoke(currentThread);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mContext;
    }
}
