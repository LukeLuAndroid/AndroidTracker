package com.sdk.tracker.listener;

import com.sdk.tracker.TrackerStoreListener;
import com.sdk.tracker.db.TrackerStoreHelper;
import com.sdk.tracker.log.TraceLog;

import java.util.List;
import java.util.Map;

public class DefaultTrackerStoreListener implements TrackerStoreListener {

    @Override
    public Map<String, List<String>> getAllCommand() {
        return TrackerStoreHelper.instance().getAllCommand();
    }

    @Override
    public void restoreGroup(List<String> permitGroups) {
        TrackerStoreHelper.instance().restoreGroup(permitGroups);
    }

    @Override
    public List<String> getAllPermitGroup() {
        return TrackerStoreHelper.instance().getAllPermitGroup();
    }

    @Override
    public void restoreCommand(Map<String, List<String>> commands) {
        TrackerStoreHelper.instance().restoreCommand(commands);
    }

    @Override
    public void addTraceLog(TraceLog bean) {
        TrackerStoreHelper.instance().addTraceLog(bean);
    }

    @Override
    public List<TraceLog> queryLogs() {
        return TrackerStoreHelper.instance().queryLogs();
    }

    @Override
    public int queryCountByLog(TraceLog log) {
        return TrackerStoreHelper.instance().queryCountByLog(log);
    }

    @Override
    public void removeLogs(List<TraceLog> list) {
        TrackerStoreHelper.instance().removeLogs(list);
    }
}
