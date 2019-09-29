package com.sdk.tracker.core;


import com.sdk.tracker.TrackerStoreListener;
import com.sdk.tracker.Trackers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Command {

    /**
     * key表示当前需要执行命令的地方
     * list中的是命令的具体执行
     */
    private Map<String, List<String>> commands = new HashMap<>();
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
        initCommand();
        initPermitGroup();
    }

    private TrackerStoreListener getListener() {
        return Trackers.instance().getTrackerStoreListener();
    }

    private void initCommand() {
        if (getListener() != null) {
            Map<String, List<String>> result = getListener().getAllCommand();
            if (result != null) {
                commands.putAll(result);
            }
        }
    }

    public void restoreCommand() {
        synchronized (commands) {
            if (getListener() != null) {
                getListener().restoreCommand(commands);
            }
        }
    }

    private void initPermitGroup() {
        if (getListener() != null) {
            List<String> groups = getListener().getAllPermitGroup();
            if (groups != null) {
                permitGroups.addAll(groups);
            }
        }
    }

    public void restoreGroup() {
        synchronized (permitGroups) {
            if (getListener() != null) {
                getListener().restoreGroup(permitGroups);
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
     * 新增获取日志指令
     *
     * @param keyInfo
     */
    public void addGetLogCommand(String keyInfo) {
        addCommand(keyInfo, CommandConstants.GET_LOG_BEFORE_COMMAND);
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
    public String getGroupPermit(String[] groups) {
        for (String group : groups) {
            if (group != null && permitGroups.contains(group)) {
                return group;
            }
        }
        return "";
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
     *
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
     *
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
     *
     * @param close
     */
    public void closeGroup(List<String> close) {
        if (close != null) {
            permitGroups.removeAll(close);
            restoreGroup();
        }
    }

}
