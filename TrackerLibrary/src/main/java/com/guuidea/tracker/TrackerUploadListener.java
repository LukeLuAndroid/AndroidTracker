package com.guuidea.tracker;

import com.guuidea.tracker.log.TraceLog;

import java.util.List;

public interface TrackerUploadListener {
    /**
     * 上传日志信息
     *
     * @param list
     */
    void uploadLogInfo(List<TraceLog> list, TrackerResultListener listener);
}
