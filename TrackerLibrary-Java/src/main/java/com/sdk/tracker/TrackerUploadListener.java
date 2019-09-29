package com.sdk.tracker;

import com.sdk.tracker.log.TraceLog;

import java.util.List;

public interface TrackerUploadListener {
    /**
     * 上传日志信息
     * @param list TraceLog 中logType为1 表示获取的值，为0表示打点日志
     * @param listener 返回值
     */
    void uploadLogInfo(List<TraceLog> list, TrackerResultListener listener);
}
