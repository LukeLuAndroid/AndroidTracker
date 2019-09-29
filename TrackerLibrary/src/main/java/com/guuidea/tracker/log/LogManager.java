package com.guuidea.tracker.log;

import android.support.v4.util.ArrayMap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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


    private Map<String, Map<String, List<String>>> logMapping = new ArrayMap<>();

    private List<TraceLog> currentLogs = new ArrayList<>();

    public List<TraceLog> getCurrentLogs() {
        if (currentLogs == null) {
            currentLogs = new ArrayList<>();
        }
        return currentLogs;
    }

    public void setCurrentLogs(List<TraceLog> currentLogs) {
        this.currentLogs = currentLogs;
    }

    public void addLogs(TraceLog log) {
        currentLogs.add(log);
    }

    public int getLogsCount() {
        return currentLogs.size();
    }

    public void removeLogs() {
        currentLogs.clear();
    }

    public void removeLogs(List<TraceLog> logs) {
        currentLogs.removeAll(logs);
    }

    //上传数据
    private void check() {
        logMapping.clear();
        for (TraceLog log : getCurrentLogs()) {
            Map<String, List<String>> timeMap = logMapping.get(log.getThreadName());
            if (timeMap == null) {
                timeMap = new ArrayMap<>();
                logMapping.put(log.getThreadName(), timeMap);
            }
            List<String> list = timeMap.get(log.getLogTime());
            if (list == null) {
                list = new ArrayList<>();
                timeMap.put(log.getLogTime(), list);
            }
            list.add(log.getLogMessage());
        }
    }

    //获取上传的数据结构
    public String getUploadJsonData() {
        check();
        JSONArray threadArray = new JSONArray();
        try {
            for (String key : logMapping.keySet()) {
                JSONObject threadJson = new JSONObject();
                threadJson.put("threadName", key);
                Map<String, List<String>> times = logMapping.get(key);
                JSONArray timeArray = new JSONArray();
                for (String timeKey : times.keySet()) {
                    JSONObject timeJson = new JSONObject();
                    timeJson.put("logTime", timeKey);
                    List<String> logs = times.get(timeKey);
                    JSONArray logArray = new JSONArray();
                    for (String log : logs) {
                        JSONObject logJson = new JSONObject();
                        logJson.put("logMessage", log);
                        logArray.put(logJson);
                    }
                    timeJson.put("logs", logArray);
                    timeArray.put(timeJson);
                }
                threadJson.put("times", timeArray);
                threadArray.put(threadJson);
            }
            String logJson = threadArray.toString();
            Log.e(TAG, "jsonInfo = " + logJson);
            return logJson;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
}
