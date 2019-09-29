package com.sdk.tracker.util;

import android.util.Log;

import com.sdk.tracker.Trackers;
import com.sdk.tracker.core.UploadHandler;
import com.sdk.tracker.log.TempleteLog;
import com.sdk.tracker.log.TraceLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerUtils {
    public static final String TAG = "TrackerUtils";

    public static void addTraceList(List<TempleteLog> list) {
        if (list != null && Trackers.instance().getTrackerAddListener() != null) {
            for (TempleteLog log : list) {
                if (log != null) {
                    Trackers.instance().getTrackerAddListener().addTracker(log.getClassName(), log.getMethodName(), log.getGroup(), log.getTag(), log.getObjectArgs());
                }
            }
        }
    }

    /**
     * 上传数据
     *
     * @param list
     */
    public static void uploadTraceList(List<TraceLog> list) {
        if (list != null && Trackers.instance().getTrackerUploadListener() != null) {
            UploadHandler uploader = new UploadHandler();
            uploader.uploadLogInfo(list);
        }
    }

    /**
     * 转换trace日志
     * <p>
     * [{
     * threadName:"线程名",
     * times:[{
     * *         logTime:"时间戳"，
     * *         logs:[{
     * *             logMessage:"TraceLog.toString()"
     * *         }]
     * }]
     * }]
     *
     * @param logList
     * @return
     */
    public static String getTraceLogJson(List<TraceLog> logList) {
        if (logList == null || logList.isEmpty()) {
            return "{}";
        }
        Map<String, Map<String, List<String>>> logMapping = new HashMap<>();
        for (TraceLog log : logList) {
            Map<String, List<String>> timeMap = logMapping.get(log.getThreadName());
            if (timeMap == null) {
                timeMap = new HashMap<>();
                logMapping.put(log.getThreadName(), timeMap);
            }
            List<String> list = timeMap.get(log.getLogTime());
            if (list == null) {
                list = new ArrayList<>();
                timeMap.put(log.getLogTime(), list);
            }
            list.add(log.toString());
        }

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

    /**
     * 获取方法唯一标志信息
     *
     * @param methodClass 类名
     * @param methodName  方法名
     * @param paramNames  参数名
     * @return
     */
    public static String getUniqueMethodId(String methodClass, String methodName, String[] paramNames) {
        StringBuilder buffer = new StringBuilder(methodClass);
        buffer.append(".").append(methodName).append("(");
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (i != 0) {
                    buffer.append(",");
                }
                buffer.append(paramNames[i]);
            }
        }
        buffer.append(")");
        return buffer.toString();
    }
}
