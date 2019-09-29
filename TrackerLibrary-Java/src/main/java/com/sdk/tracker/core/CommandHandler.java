package com.sdk.tracker.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.sdk.tracker.TrackerAddListener;
import com.sdk.tracker.TrackerStoreListener;
import com.sdk.tracker.Trackers;
import com.sdk.tracker.log.LogManager;
import com.sdk.tracker.log.TraceLog;
import com.sdk.tracker.provider.TrackerNode.PropInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CommandHandler {
    private static final String TAG = "CommandHandler";
    private Handler mHandler = new HandlerImpl(this);
    private UploadHandler uploader = new UploadHandler();

    public CommandHandler() {
    }

    public static TrackerAddListener getAddListener() {
        return Trackers.instance().getTrackerAddListener();
    }

    public static TrackerStoreListener getStoreListener() {
        return Trackers.instance().getTrackerStoreListener();
    }

    public static boolean isDebug() {
        return Trackers.instance().isDebug();
    }

    public void init() {
        uploadLastReadyLogInfo();
    }


    /**
     * 获取方法结果
     *
     * @param input
     */
    public Object getResult(Object input, String command) {
        return getProperties(null, input, command);
    }

    /**
     * 获取属性的值
     *
     * @param methodClass
     * @param input
     * @param jsonCommand
     * @return
     */
    public Object getProperties(String methodClass, Object input, String jsonCommand) {
        Object result = null;
        if (input != null || methodClass != null) {
            if (input != null && (input instanceof String || input instanceof Integer || input instanceof Long || input instanceof Byte ||
                    input instanceof Character || input instanceof Double || input instanceof Float || input instanceof Boolean)) {
                result = String.valueOf(input);
            } else {
                List<PropInfo> list = new ArrayList<>();
                try {
                    JSONArray array = new JSONArray(jsonCommand);
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject object = array.getJSONObject(i);
                            PropInfo prop = new PropInfo();
                            prop.name = object.optString("name");
                            prop.key = object.opt("key");
                            list.add(prop);
                        }
                    }
                    return getProperties(methodClass, input, list);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 获取属性
     *
     * @param input
     * @param props
     * @return
     */
    private Object getProperties(String methodClass, Object input, List<PropInfo> props) {
        Object result;
        if ((input != null || methodClass != null) && props != null) {
            try {
                result = input;
                Class cls;
                if (input != null) {
                    cls = input.getClass();
                } else {
                    cls = Class.forName(methodClass);
                }

                Class filedCls = cls;
                for (int i = 0; i < props.size(); i++) {
                    if (filedCls == null) {
                        break;
                    }
                    Field field = null;
                    PropInfo prop = props.get(i);
                    if (prop.name != null && !"".equals(prop.name)) {
                        field = filedCls.getDeclaredField(prop.name);
                    }

                    if (field != null) {
                        field.setAccessible(true);
                        result = field.get(result);
                        if (result != null) {
                            filedCls = result.getClass();
                        } else {
                            filedCls = null;
                        }
                    }

                    if (prop.key != null && !"".equals(prop.key)) {
                        if (result instanceof List && prop.key instanceof Integer) {
                            Method method = filedCls.getDeclaredMethod("get", int.class);
                            if (method != null) {
                                result = method.invoke(result, prop.key);
                            } else {
                                result = null;
                            }
                        } else if (result instanceof Map) {
                            Method method = filedCls.getDeclaredMethod("get", Object.class);
                            if (method != null) {
                                result = method.invoke(result, prop.key);
                            } else {
                                result = null;
                            }
                        } else if (result instanceof String[] && prop.key instanceof Integer) {
                            String[] source = (String[]) result;
                            if (source.length > (int) prop.key) {
                                result = source[(int) prop.key];
                            }
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获取传入参数
     *
     * @param args
     */
    public Object getParameters(int position, String command, Object... args) {
        try {
            if (args.length > position) {
                return getProperties(null, args[position], command);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取日志
     *
     * @param methodClass
     * @param methodName
     * @param paramNames
     */
    public void getLog(final String methodClass, final String methodName, final String[] paramNames, String group, final String tag, String script, Object... args) {
        long startTime = System.currentTimeMillis();
        Thread thread = Thread.currentThread();
        final String threadName = thread.getName();

        String log;
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        if (traces.length > 5) {
            log = String.valueOf(traces[5]);
        } else {
            log = "get log error";
        }

        String methodInfo = Trackers.getMethodInfo(methodClass, methodName, paramNames);

        final TraceLog bean = TraceLog.obtain();
        bean.markInUse();
        bean.setLogMessage(log);
        bean.setLogTime(getFormatTime());
        bean.setThreadName(threadName);
        bean.setMethodInfo(methodInfo);
        bean.setGroup(group);
        bean.setTag(tag);
        bean.setLogType(TraceLog.LOG_TYPE);
        bean.setClassName(methodClass);
        bean.setMethodName(methodName);
        bean.setObjectArgs(args);

        //移除指令
        Command.instance().removeCommand(methodInfo, script);
        ThreadPool.submitSingleTask(new Runnable() {
            @Override
            public void run() {
                if (getStoreListener() != null) {
                    if (isDebug()) {
                        getStoreListener().addTraceLog(bean);
                        LogManager.instance().addLogs(bean);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                uploader.uploadDebugLogInfo(bean);
                            }
                        });
                    } else {
                        if (LogManager.instance().getCurrentLogs().isEmpty() && getStoreListener() != null) {
                            LogManager.instance().setCurrentLogs(getStoreListener().queryLogs());
                        }

                        int count = getStoreListener().queryCountByLog(bean);
                        if (count < CommandConstants.MAX_METHOD_RECORD) {
                            //最多存3次
                            if (getStoreListener() != null) {
                                getStoreListener().addTraceLog(bean);
                            }
                            LogManager.instance().addLogs(bean);
                        }

                        //当getLog的指令都空了,则尝试一次上传
//                        if (Command.instance().isCommandActionEmpty(CommandConstants.GET_LOG)) {
//                            mHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    uploader.uploadLogInfo(LogManager.instance().getCurrentLogs());
//                                }
//                            });
//                        }
                    }
                }
                Command.instance().restoreCommand();
            }
        });

        if (isDebug()) {
            Log.e(TAG, "getLog cost time=" + (System.currentTimeMillis() - startTime) + ",methodName=" + methodName);
        }
    }

    public void uploadValue(String className, String methodName, String[] paramNames, String script, String group, final Object value, Object... args) {
        String methodInfo = Trackers.getMethodInfo(className, methodName, paramNames);

        long startTime = System.currentTimeMillis();
        Thread thread = Thread.currentThread();
        final String threadName = thread.getName();

        final TraceLog bean = TraceLog.obtain();
        bean.markInUse();
        bean.setLogMessage(script);
        bean.setLogTime(getFormatTime());
        bean.setThreadName(threadName);
        bean.setMethodInfo(methodInfo);
        bean.setGroup(group);
        bean.setTag(String.valueOf(value));
        bean.setLogType(TraceLog.VALUE_TYPE);
        bean.setClassName(className);
        bean.setMethodName(methodName);
        bean.setObjectArgs(args);

        ThreadPool.submitSingleTask(new Runnable() {
            @Override
            public void run() {
                if (getStoreListener() != null) {
                    if (isDebug()) {
                        getStoreListener().addTraceLog(bean);
                        LogManager.instance().addLogs(bean);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                uploader.uploadDebugLogInfo(bean);
                            }
                        });
                    } else {
                        if (LogManager.instance().getCurrentLogs().isEmpty()) {
                            LogManager.instance().setCurrentLogs(getStoreListener().queryLogs());
                        }

                        getStoreListener().addTraceLog(bean);
                        LogManager.instance().addLogs(bean);

                        ////当指令都空了,则尝试一次上传
//                        if (Command.instance().isCommandEmpty()) {
//                            mHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    uploader.uploadLogInfo(LogManager.instance().getCurrentLogs());
//                                }
//                            });
//                        }
                    }
                    Command.instance().restoreCommand();
                }
            }
        });
        if (isDebug()) {
            Log.e(TAG, "getLog cost time=" + (System.currentTimeMillis() - startTime) + ",methodName=" + methodName);
        }
    }

    /**
     * 上传已经准备好的日志信息
     * 每次启动上报
     */
    private void uploadLastReadyLogInfo() {
        ThreadPool.submitSingleTask(new Runnable() {
            @Override
            public void run() {
                if (getStoreListener() != null) {
                    final List<TraceLog> logs = getStoreListener().queryLogs();
                    if (logs != null && logs.size() > 0) {
                        LogManager.instance().setCurrentLogs(logs);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                uploader.uploadLogInfo(logs);
                            }
                        });
                    }
                }
            }
        });
    }

    //    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    public static String getFormatTime() {
        Date date = new Date();
        return dateFormat.format(date);
    }

    private static class HandlerImpl extends Handler {

        private WeakReference<CommandHandler> mReference;

        public HandlerImpl(CommandHandler handler) {
            super(Looper.getMainLooper());
            mReference = new WeakReference<>(handler);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
