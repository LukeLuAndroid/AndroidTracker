package com.sdk.tracker.log;

import android.os.Parcel;
import android.os.Parcelable;


public class TraceLog extends TempleteLog implements Parcelable {

    /**
     * the log type
     */
    public static final String LOG_TYPE = "0";
    /**
     * the value type
     */
    public static final String VALUE_TYPE = "1";

    /**
     * 唯一id
     */
    private int mLogId;
    /**
     * 线程
     */
    private String threadName;
    /**
     * 日志时间
     */
    private String logTime;
    /**
     * 日志信息
     */
    private String logMessage;
    /**
     * 方法信息
     */
    private String methodInfo;

    /**
     * 日志类型
     */
    private String logType;

    public int getId() {
        return mLogId;
    }

    public void setId(int id) {
        this.mLogId = id;
    }

    public String getMethodInfo() {
        return methodInfo;
    }

    public void setMethodInfo(String methodInfo) {
        this.methodInfo = methodInfo;
    }

    public String getLogTime() {
        return logTime;
    }

    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    @Override
    public int hashCode() {
        int hashCode = mLogId * 2 ^ 4;
        if (logTime != null) {
            hashCode += logTime.hashCode() * 2 ^ 4;
        }
        if (logMessage != null) {
            hashCode += logMessage.hashCode() * 2 ^ 4;
        }
        if (threadName != null) {
            hashCode += threadName.hashCode() * 2 ^ 4;
        }
        if (methodInfo != null) {
            hashCode += methodInfo.hashCode() * 2 ^ 4;
        }
        if (logType != null) {
            hashCode += logType.hashCode() * 2 ^ 4;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof TraceLog) {
            TraceLog other = (TraceLog) obj;
            if (mLogId == ((TraceLog) obj).getId()) {
                if (logTime != null && logTime.equals(other.getLogTime())) {
                    if (threadName != null && threadName.equals(other.getThreadName())) {
                        if (methodInfo != null && methodInfo.equals(other.getMethodInfo())) {
                            if (logMessage != null && logMessage.equals(other.getLogMessage())) {
                                if (logType != null && logType.equals(other.getLogType())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "{\"" + "id" + '\"' + ":" + '\"' + mLogId + '\"' +
                ", \"" + "threadname" + '\"' + ":" + '\"' + threadName + '\"' +
                ", \"" + "methodInfo" + '\"' + ":" + '\"' + methodInfo + '\"' +
                ", \"" + "logMessage" + '\"' + ":" + '\"' + logMessage + '\"' +
                ", \"" + "logGroup" + '\"' + ":" + '\"' + getGroup() + '\"' +
                ", \"" + "logTag" + '\"' + ":" + '\"' + getTag() + '\"' +
                ", \"" + "logType" + '\"' + ":" + '\"' + logType + '\"' +
                ", \'" + "logTime" + '\"' + ":" + '\"' + logTime + '\"' + "}";
    }

    public TraceLog() {
    }

    int flags;

    TraceLog next;

    static final int FLAG_IN_USE = 1 << 0;

    /**
     * @hide
     */
    public static final Object sPoolSync = new Object();
    private static TraceLog sPool;
    private static int sPoolSize = 0;

    private static final int MAX_POOL_SIZE = 50;

    public static TraceLog obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                TraceLog l = sPool;
                sPool = l.next;
                l.next = null;
                l.flags = 0; // clear in-use flag
                sPoolSize--;
                return l;
            }
        }
        return new TraceLog();
    }

    boolean isInUse() {
        return ((flags & FLAG_IN_USE) == FLAG_IN_USE);
    }

    public void recycleToCache() {
        if (isInUse()) {
            return;
        }
        recycleUnchecked();
    }

    public void markInUse() {
        flags |= FLAG_IN_USE;
    }

    public void recycleUnchecked() {
        flags = FLAG_IN_USE;

        logMessage = null;
        logTime = "";
        threadName = "";
        methodInfo = null;
        logType = "";
        tag = "";
        className = "";
        methodName = "";
        objectArgs = null;

        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mLogId);
        dest.writeString(this.threadName);
        dest.writeString(this.logTime);
        dest.writeString(this.logMessage);
        dest.writeString(this.methodInfo);
        dest.writeString(this.logType);
    }

    protected TraceLog(Parcel in) {
        super(in);
        this.mLogId = in.readInt();
        this.threadName = in.readString();
        this.logTime = in.readString();
        this.logMessage = in.readString();
        this.methodInfo = in.readString();
        this.logType = in.readString();
    }

    public static final Creator<TraceLog> CREATOR = new Creator<TraceLog>() {
        @Override
        public TraceLog createFromParcel(Parcel source) {
            return new TraceLog(source);
        }

        @Override
        public TraceLog[] newArray(int size) {
            return new TraceLog[size];
        }
    };
}
