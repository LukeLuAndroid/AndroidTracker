package com.sdk.tracker.log;

import android.os.Parcel;
import android.os.Parcelable;

public class TempleteLog implements Parcelable {
    /**
     * 方法名
     */
    protected String methodName;
    /**
     * 类名
     */
    protected String className;
    /**
     * 参数
     */
    protected Object[] objectArgs;

    /**
     * 分组信息
     */
    protected String group;

    /**
     * tag参数
     */
    protected String tag;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String name) {
        this.methodName = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String name) {
        this.className = name;
    }

    public Object[] getObjectArgs() {
        return objectArgs;
    }

    public void setObjectArgs(Object[] args) {
        this.objectArgs = args;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public TempleteLog() {

    }

    public TempleteLog(String className, String methodName, String group, String tag, Object[] args) {
        this.methodName = methodName;
        this.className = className;
        this.objectArgs = args;
        this.group = group;
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "{\"" + "className" + '\"' + ":" + '\"' + className + '\"' +
                ", \"" + "methodName" + '\"' + ":" + '\"' + methodName + '\"' +
                ", \"" + "group" + '\"' + ":" + '\"' + group + '\"' +
                ", \"" + "tag" + '\"' + ":" + '\"' + tag + '\"' + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.methodName);
        dest.writeString(this.className);
        dest.writeString(this.group);
        dest.writeString(this.tag);
    }

    protected TempleteLog(Parcel in) {
        this.methodName = in.readString();
        this.className = in.readString();
        this.group = in.readString();
        this.tag = in.readString();
    }

}
