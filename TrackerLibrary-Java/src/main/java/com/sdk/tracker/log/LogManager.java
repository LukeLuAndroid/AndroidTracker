package com.sdk.tracker.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogManager {
    public static final String TAG = "LogManager";

    private static class Singleton {
        static LogManager _instance = new LogManager();
    }

    public static LogManager instance() {
        return LogManager.Singleton._instance;
    }

    private List<TraceLog> currentLogs = new ArrayList<>();

    public List<TraceLog> getCurrentLogs() {
        if (currentLogs == null) {
            currentLogs = new ArrayList<>();
        }
        return currentLogs;
    }

    public void setCurrentLogs(List<TraceLog> currentLogs) {
        getCurrentLogs().clear();
        getCurrentLogs().addAll(currentLogs);
    }

    public void addLogs(TraceLog log) {
        getCurrentLogs().add(log);
    }

    public int getLogsCount() {
        return getCurrentLogs().size();
    }

    public void removeLogs() {
        getCurrentLogs().clear();
    }

    public void removeLogs(List<TraceLog> logs) {
        getCurrentLogs().removeAll(logs);
    }
}
