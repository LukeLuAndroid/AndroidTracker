package com.guuidea.tracker.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.guuidea.tracker.TrackerAddListener;
import com.guuidea.tracker.Trackers;
import com.guuidea.tracker.db.TraceLogDB;
import com.guuidea.tracker.log.LogManager;
import com.guuidea.tracker.log.TraceLog;
import com.guuidea.tracker.provider.TrackerNode.PropInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
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
        return getProperties(input, command);
    }

    public Object getProperties(Object input, String command) {
        Object result = null;
        if (input != null) {
            if (input instanceof String || input instanceof Integer || input instanceof Long || input instanceof Byte ||
                    input instanceof Character || input instanceof Double || input instanceof Float || input instanceof Boolean) {
                result = String.valueOf(input);
            } else {
                List<PropInfo> list = new ArrayList<>();
                try {
                    JSONArray array = new JSONArray(command);
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject object = array.getJSONObject(i);
                            PropInfo prop = new PropInfo();
                            prop.name = object.optString("name");
                            prop.key = object.opt("key");
                            list.add(prop);
                        }
                    }
                    return getProperties(input, list);
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
    private Object getProperties(Object input, List<PropInfo> props) {
        Object result;
        if (input != null && props != null) {
            try {
                result = input;
                Class cls = input.getClass();
                for (int i = 0; i < props.size(); i++) {
                    if (result == null) {
                        return "";
                    }
                    Field field = null;
                    PropInfo prop = props.get(i);
                    if (prop.name != null && !"".equals(prop.name)) {
                        field = cls.getDeclaredField(prop.name);
                    }
                    Class filedCls = cls;
                    if (field != null) {
                        field.setAccessible(true);
                        result = field.get(result);
                        if (prop.key != null && result != null) {
                            filedCls = result.getClass();
                        }
                    }

                    if (result instanceof List) {
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
    public Object getParameters(String command, Object... args) {
        return null;
    }

    /**
     * 获取日志
     *
     * @param methodClass
     * @param methodName
     * @param paramClassNames
     */
    public void getLog(final String methodClass, final String methodName, final String[] paramClassNames, final String tag, String script) {
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

        String methodInfo = getMethodInfo(methodClass, methodName, paramClassNames);

        final TraceLog bean = TraceLog.obtain();
        bean.markInUse();
        bean.setLogMessage(log);
        bean.setLogTime(getFormatTime());
        bean.setThreadName(threadName);
        bean.setMethodInfo(methodInfo);
        bean.setParams(tag);

        //移除指令
        Command.instance().removeCommand(methodClass + "." + methodName, script);
        ThreadPool.submitSingleTask(new Runnable() {
            @Override
            public void run() {
                if (isDebug()) {
                    TraceLogDB.getInstance().addTraceLog(ContextGetter.getContext(), bean);
                    LogManager.instance().addLogs(bean);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            uploader.uploadDebugLogInfo(bean);
                        }
                    });
                } else {
                    if (LogManager.instance().getCurrentLogs().isEmpty()) {
                        LogManager.instance().setCurrentLogs(TraceLogDB.getInstance().queryLogs(ContextGetter.getContext()));
                    }

                    int count = TraceLogDB.getInstance().queryCountByMethodInfo(ContextGetter.getContext(), bean);
                    if (count < CommandConstants.MAX_METHOD_RECORD) {
                        //最多存3次
                        TraceLogDB.getInstance().addTraceLog(ContextGetter.getContext(), bean);
                        LogManager.instance().addLogs(bean);
                    }
                    //指令空了
                    if (Command.instance().isCommandActionEmpty(CommandConstants.GET_LOG)) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                uploader.uploadLogInfo();
                            }
                        });
                    }
                }
                Command.instance().restoreCommand();
            }
        });

        if (isDebug()) {
            Log.e(TAG, "getLog cost time=" + (System.currentTimeMillis() - startTime) + ",methodName=" + methodName);
        }
    }

    /**
     * 获取方法唯一标志信息
     *
     * @param methodClass
     * @param methodName
     * @param paramClassNames
     * @return
     */
    private static String getMethodInfo(String methodClass, String methodName, String[] paramClassNames) {
        StringBuilder buffer = new StringBuilder(methodClass);
        buffer.append(".").append(methodName).append("(");
        for (int i = 0; i < paramClassNames.length; i++) {
            if (i != 0) {
                buffer.append(", ");
            }
            buffer.append(paramClassNames[i]);
        }
        buffer.append(")");
        return buffer.toString();
    }

    /**
     * 上传已经准备好的日志信息
     * 每次启动上报
     */
    private void uploadLastReadyLogInfo() {
        ThreadPool.submitSingleTask(new Runnable() {
            @Override
            public void run() {
                List<TraceLog> logs = TraceLogDB.getInstance().queryLogs(ContextGetter.getContext());
                if (logs.size() > 0) {
                    LogManager.instance().setCurrentLogs(logs);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            uploader.uploadLogInfo();
                        }
                    });
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
