package com.sdk.tracker.core;

import com.sdk.tracker.TrackerResultListener;
import com.sdk.tracker.TrackerUploadListener;
import com.sdk.tracker.Trackers;
import com.sdk.tracker.log.LogManager;
import com.sdk.tracker.log.TraceLog;
import com.sdk.tracker.util.TrackerUtils;

import java.util.ArrayList;
import java.util.List;

public class UploadHandler {

    private TrackerUploadListener getUploadListener() {
        return Trackers.instance().getTrackerUploadListener();
    }

    public void uploadDebugLogInfo(TraceLog bean) {
        List<TraceLog> logs = new ArrayList<>();
        logs.add(bean);
        if (getUploadListener() != null) {
            getUploadListener().uploadLogInfo(logs, new TrackerResultCall(bean));
        } else {
            String json = TrackerUtils.getTraceLogJson(LogManager.instance().getCurrentLogs());
        }
    }

    /**
     * 上传数据
     */
    public void uploadLogInfo(List<TraceLog> logs) {
        if (getUploadListener() != null) {
            getUploadListener().uploadLogInfo(logs, new TrackerResultCall(logs));
        } else {
            String json = TrackerUtils.getTraceLogJson(logs);
        }
    }

    private static class TrackerResultCall implements TrackerResultListener {

        private List<TraceLog> list = new ArrayList<>();

        public TrackerResultCall(List<TraceLog> traceLogs) {
            list.clear();
            list.addAll(traceLogs);
        }

        public TrackerResultCall(TraceLog traceLog) {
            list.clear();
            list.add(traceLog);
        }

        @Override
        public void onSuccess() {
            if (Trackers.instance().getTrackerStoreListener() != null) {
                Trackers.instance().getTrackerStoreListener().removeLogs(list);
                LogManager.instance().removeLogs(list);
                for (TraceLog log : list) {
                    if (log != null) {
                        log.recycleUnchecked();
                    }
                }
                Command.instance().restoreCommand();
            }
        }

        @Override
        public void onFail() {
            Command.instance().restoreCommand();
        }
    }
}
