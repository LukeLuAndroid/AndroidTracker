package com.sdk.tracker.db.column;

public class TraceLogColumns {
    public static final String _ID = "_id";
    public static final String THREAD_NAME = "thread_name";
    public static final String LOG_TIME = "log_time";
    public static final String LOG_GROUP = "log_group";
    /**
     * 0 表示日志
     * 1 表示变量值
     */
    public static final String LOG_TYPE = "log_type";
    public static final String LOG_TAG = "log_tag";
    public static final String LOG_MESSAGE = "log_message";
    public static final String METHOD_INFO = "method_info";

    public static final String TABLE = "logs";

    public static final String[] COLUMNS = new String[]{_ID, THREAD_NAME, METHOD_INFO, LOG_MESSAGE, LOG_TYPE, LOG_GROUP, LOG_TAG, LOG_TIME};

    public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE + "(" +
            _ID + " integer primary key autoincrement," +
            THREAD_NAME + " text, " +
            LOG_MESSAGE + " text, " +
            METHOD_INFO + " text, " +
            LOG_TYPE + " text, " +
            LOG_GROUP + " text, " +
            LOG_TAG + " text, " +
            LOG_TIME + " text);";
}
