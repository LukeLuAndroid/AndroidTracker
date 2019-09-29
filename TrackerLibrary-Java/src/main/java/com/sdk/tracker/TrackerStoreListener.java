package com.sdk.tracker;

import com.sdk.tracker.log.TraceLog;

import java.util.List;
import java.util.Map;

public interface TrackerStoreListener {

    Map<String, List<String>> getAllCommand();

    void restoreGroup(List<String> permitGroups);

    List<String> getAllPermitGroup();

    void restoreCommand(Map<String, List<String>> commands);

    void addTraceLog(TraceLog bean);

    List<TraceLog> queryLogs();

    int queryCountByLog(TraceLog log);

    void removeLogs(List<TraceLog> list);
}
