package com.guuidea.tracker.core;

import com.guuidea.tracker.TrackerResultListener;
import com.guuidea.tracker.TrackerUploadListener;
import com.guuidea.tracker.Trackers;
import com.guuidea.tracker.db.TraceLogDB;
import com.guuidea.tracker.log.LogManager;
import com.guuidea.tracker.log.TraceLog;

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
            String json = LogManager.instance().getUploadJsonData();
        }
    }

    /**
     * 上传数据
     */
    public void uploadLogInfo() {
        if (getUploadListener() != null) {
            getUploadListener().uploadLogInfo(LogManager.instance().getCurrentLogs(), new TrackerResultCall(LogManager.instance().getCurrentLogs()));
        } else {
            String json = LogManager.instance().getUploadJsonData();
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
            TraceLogDB.getInstance().removeLogs(ContextGetter.getContext(), list);
            LogManager.instance().removeLogs(list);
            for (TraceLog log : list) {
                if (log != null) {
                    log.recycleUnchecked();
                }
            }
        }

        @Override
        public void onFail() {

        }
    }
}
