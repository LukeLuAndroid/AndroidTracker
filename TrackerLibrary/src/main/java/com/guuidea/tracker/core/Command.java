package com.guuidea.tracker.core;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.ArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Command {

    private static final String SP_NAME = "sp_command";
    /**
     * key表示当前需要执行命令的地方
     * list中的是命令的具体执行
     */
    private ArrayMap<String, List<String>> commands = new ArrayMap<>();
    private List<String> permitGroups = new ArrayList<>();
    private List<String> permitTags = new ArrayList<>();

    private static class Singleton {
        private static Command _instance = new Command();
    }

    public static Command instance() {
        return Singleton._instance;
    }

    public Command() {
        init();
    }

    private void init() {
        Context context = ContextGetter.getContext();
        if (context != null) {
            SharedPreferences sp = ContextGetter.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            if (sp != null) {
                initCommand(sp);
                initPermitGroup(sp);
            }
        }
    }

    private void initCommand(SharedPreferences sp) {
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

    public void restoreCommand() {
        Context context = ContextGetter.getContext();
        if (context != null) {
            SharedPreferences sp = ContextGetter.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);

            Set<String> sets = commands.keySet();
            try {
                JSONObject result = new JSONObject();
                for (String key : sets) {
                    List<String> items = commands.get(key);
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

    private void initPermitGroup(SharedPreferences sp) {
        String data = sp.getString("group", "");
        try {
            if (!"".equals(data)) {
                JSONObject object = new JSONObject(data);
                Iterator<String> iterable = object.keys();
                while (iterable.hasNext()) {
                    String key = iterable.next();
                    permitGroups.add(key);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void restoreGroup() {
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

    public void removeCommand(String keyInfo, String command) {
        List<String> list = commands.get(keyInfo);
        if (list == null || list.isEmpty()) {
            return;
        }
        list.remove(command);
    }

    public void removeCommand(String keyInfo) {
        commands.remove(keyInfo);
    }

    public void addCommand(String keyInfo, String action) {
        List<String> list = commands.get(keyInfo);
        if (list == null) {
            list = new ArrayList<>();
            commands.put(keyInfo, list);
        }
        if (!list.contains(action)) {
            list.add(action);
        }
    }

    public List<String> getCommandInfo(String classInfo) {
        List<String> list = commands.get(classInfo);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }

    public boolean isCommandEmpty() {
        return commands.isEmpty();
    }

    //具体指令为空 比如getLog
    public boolean isCommandActionEmpty(String action) {
        if (action == null || "".equals(action)) {
            return true;
        }
        for (List<String> values : commands.values()) {
            if (values != null && values.contains(action)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否包含某个指令
     *
     * @param args
     * @return
     */
    public boolean contains(String... args) {
        for (String arg : args) {
            if (getCommandInfo(arg) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 某分组是否被允许
     *
     * @param groups
     * @return
     */
    public boolean isGroupPermit(String[] groups) {
        for (String group : groups) {
            if (group != null && permitGroups.contains(group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 某tag是否被允许
     *
     * @param tag
     * @return
     */
    public boolean isTagPermit(String tag) {
        return tag != null && permitTags.contains(tag);
    }

    /**
     * 打开日志组
     * @param allow
     */
    public void openGroup(List<String> allow) {
        if (allow != null) {
            permitGroups.removeAll(allow);
            permitGroups.addAll(allow);
            restoreGroup();
        }
    }

    /**
     * 打开日志组
     * @param group
     */
    public void openGroup(String group) {
        if (group != null) {
            permitGroups.remove(group);
            permitGroups.add(group);
            restoreGroup();
        }
    }

    /**
     * 关闭日志分组权限
     * @param close
     */
    public void closeGroup(List<String> close) {
        if (close != null) {
            permitGroups.removeAll(close);
            restoreGroup();
        }
    }

}
