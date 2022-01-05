package com.sdk.tracker.db;

import android.content.Context;
import android.content.SharedPreferences;

import com.sdk.tracker.log.TraceLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrackerStoreHelper {
    private static final String SP_NAME = "sp_command";
    private static final String TRACKER_DATA_NAME = "sp_tracker_data";

    public List<TraceLog> queryLogs() {
        return TraceLogDB.getInstance().queryLogs(ContextGetter.getContext());
    }

    public int queryCountByLog(TraceLog log) {
        return TraceLogDB.getInstance().queryCountByMethodInfo(ContextGetter.getContext(), log);
    }

    public void removeLogs(List<TraceLog> list) {
        TraceLogDB.getInstance().removeLogs(ContextGetter.getContext(), list);
    }

    private static class Singleton {
        private static TrackerStoreHelper _instance = new TrackerStoreHelper();
    }

    public static TrackerStoreHelper instance() {
        return Singleton._instance;
    }

    public Map<String, List<String>> getAllCommand() {
        Map<String, List<String>> commands = new HashMap<>();
        Context context = ContextGetter.getContext();

        if (context != null) {
            SharedPreferences sp = ContextGetter.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            if (sp != null) {
                String data = sp.getString("command", "");
                try {
                    if (!"".equals(data)) {
                        JSONObject object = new JSONObject(data);
                        Iterator<String> iterable = object.keys();
                        while (iterable.hasNext()) {
                            String key = iterable.next();
                            String value = object.optString(key, "[]");
                            List<String> actions = new ArrayList<>();
                            JSONArray array = new JSONArray(value);
                            JSONObject dataObj;
                            for (int i = 0; i < array.length(); i++) {
                                dataObj = array.getJSONObject(i);
                                String info = dataObj.getString("data");
                                actions.add(info);
                            }
                            commands.put(key, actions);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return commands;
    }

    public List<String> getAllPermitGroup() {
        Context context = ContextGetter.getContext();
        List<String> groups = new ArrayList<>();

        if (context != null) {
            SharedPreferences sp = ContextGetter.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            if (sp != null) {
                String data = sp.getString("group", "");
                try {
                    if (!"".equals(data)) {
                        JSONObject object = new JSONObject(data);
                        Iterator<String> iterable = object.keys();
                        while (iterable.hasNext()) {
                            String key = iterable.next();
                            groups.add(key);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return groups;
    }

    public void restoreCommand(Map<String, List<String>> commands) {
        synchronized (commands) {
            Context context = ContextGetter.getContext();
            if (context != null) {
                SharedPreferences sp = ContextGetter.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);

                Set<Map.Entry<String,List<String>>> sets = commands.entrySet();
                try {
                    JSONObject result = new JSONObject();
                    for (Map.Entry<String,List<String>> entry : sets) {
                        String key = entry.getKey();
                        List<String> items = entry.getValue();
                        JSONArray array = new JSONArray();
                        if (items != null && !items.isEmpty()) {
                            JSONObject jsonObject;
                            for (int i = 0; i < items.size(); i++) {
                                String info = items.get(i);
                                jsonObject = new JSONObject();
                                jsonObject.put("data", info);
                                array.put(jsonObject);
                            }
                        }
                        result.put(key, array.toString());
                    }
                    if (result != null) {
                        sp.edit().putString("command", result.toString()).commit();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void restoreGroup(List<String> permitGroups) {
        Context context = ContextGetter.getContext();
        if (context != null) {
            SharedPreferences sp = ContextGetter.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);

            Iterator<String> iterator = permitGroups.iterator();
            try {
                JSONObject result = new JSONObject();
                while (iterator.hasNext()) {
                    String value = iterator.next();
                    result.put(value, "");
                }
                if (result != null) {
                    sp.edit().putString("group", result.toString()).commit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addTraceLog(TraceLog bean) {
        TraceLogDB.getInstance().addTraceLog(ContextGetter.getContext(), bean);
    }

    private SharedPreferences dataPreferences;

    public SharedPreferences getDataPreferences() {
        Context context = ContextGetter.getContext();
        if (dataPreferences == null && context != null) {
            dataPreferences = context.getSharedPreferences(TRACKER_DATA_NAME, Context.MODE_PRIVATE);
        }
        return dataPreferences;
    }

    public static long getZeroTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public void addDailyUploadCount() {
        SharedPreferences sp = getDataPreferences();
        if (sp != null) {
            long currentZeroTime = getZeroTime(System.currentTimeMillis());
            long lastUpload = sp.getLong("last_upload", 0);
            SharedPreferences.Editor editor = sp.edit();
            if (lastUpload == 0 || lastUpload != currentZeroTime) {
                editor.putInt("upload_count", 1).putLong("last_upload", currentZeroTime).commit();
            } else {
                int count = getDailyUploadCount();
                editor.putInt("upload_count", count + 1).commit();
            }
        }
    }

    public int getDailyUploadCount() {
        SharedPreferences sp = getDataPreferences();
        if (sp != null) {
            long currentZeroTime = getZeroTime(System.currentTimeMillis());
            long lastUpload = sp.getLong("last_upload", 0);
            if (currentZeroTime != lastUpload) {
                return 0;
            }
            return sp.getInt("upload_count", 0);
        }
        return 0;
    }

}
