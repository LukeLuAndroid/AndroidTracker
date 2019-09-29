package com.sdk.tracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.sdk.tracker.Trackers;
import com.sdk.tracker.db.column.TraceLogColumns;
import com.sdk.tracker.log.TraceLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TraceLogDB {
    public static final String TAG = "TraceLogDB";
    private Lock lock = new ReentrantLock(true);

    private static class Singleton {
        static TraceLogDB _instance = new TraceLogDB();
    }

    public static TraceLogDB getInstance() {
        return Singleton._instance;
    }

    private DBHelper mdbHelper;

    private void checkDB(Context context) {
        if (mdbHelper == null && context != null) {
            mdbHelper = new DBHelper(context);
        }
    }

    public void addTraceLog(Context context, TraceLog log) {
        checkDB(context);
        if (mdbHelper == null) {
            return;
        }
        SQLiteDatabase db = null;
        try {
            lock.lock();
            db = mdbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(TraceLogColumns.THREAD_NAME, log.getThreadName());
            values.put(TraceLogColumns.LOG_MESSAGE, log.getLogMessage());
            values.put(TraceLogColumns.METHOD_INFO, log.getMethodInfo());
            values.put(TraceLogColumns.LOG_TYPE, log.getLogType());
            values.put(TraceLogColumns.LOG_TIME, log.getLogTime());
            values.put(TraceLogColumns.LOG_GROUP, log.getGroup());
            values.put(TraceLogColumns.LOG_TAG, log.getTag());

            db.insert(TraceLogColumns.TABLE, null, values);
        } finally {
            if (db != null) {
                db.close();
            }
            lock.unlock();
        }
        if (Trackers.instance().isDebug()) {
            Log.e(TAG, "insert message into db");
        }
    }

    /**
     * 查询所有日志
     *
     * @param context
     * @return
     */
    public List<TraceLog> queryLogs(Context context) {
        checkDB(context);
        List<TraceLog> list = new ArrayList<>();

        if (mdbHelper == null) {
            return list;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            lock.lock();
            db = mdbHelper.getReadableDatabase();
            cursor = db.query(TraceLogColumns.TABLE, TraceLogColumns.COLUMNS, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String thread_name = cursor.getString(cursor.getColumnIndex(TraceLogColumns.THREAD_NAME));
                    String method_info = cursor.getString(cursor.getColumnIndex(TraceLogColumns.METHOD_INFO));
                    String log_message = cursor.getString(cursor.getColumnIndex(TraceLogColumns.LOG_MESSAGE));
                    String log_time = cursor.getString(cursor.getColumnIndex(TraceLogColumns.LOG_TIME));
                    String log_group = cursor.getString(cursor.getColumnIndex(TraceLogColumns.LOG_GROUP));
                    String log_tag = cursor.getString(cursor.getColumnIndex(TraceLogColumns.LOG_TAG));
                    String log_type = cursor.getString(cursor.getColumnIndex(TraceLogColumns.LOG_TYPE));
                    int id = cursor.getInt(cursor.getColumnIndex(TraceLogColumns._ID));
                    TraceLog log = TraceLog.obtain();
                    log.setLogMessage(log_message);
                    log.setThreadName(thread_name);
                    log.setMethodInfo(method_info);
                    log.setLogType(log_type);
                    log.setGroup(log_group);
                    log.setTag(log_tag);
                    log.setLogTime(log_time);
                    log.setId(id);
                    list.add(log);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
            lock.unlock();
        }
        return list;
    }

    /**
     * query count by method info
     *
     * @param context
     * @param log
     * @return
     */
    public int queryCountByMethodInfo(Context context, TraceLog log) {
        checkDB(context);

        if (mdbHelper == null || log == null) {
            return 0;
        }
        String sql = " SELECT COUNT ( *)FROM " + TraceLogColumns.TABLE + " where " + TraceLogColumns.METHOD_INFO + "=? and " + TraceLogColumns.LOG_TYPE + "=? and " + TraceLogColumns.LOG_MESSAGE + "=?";
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            lock.lock();
            db = mdbHelper.getReadableDatabase();
            cursor = db.rawQuery(sql, new String[]{log.getMethodInfo(), "0", log.getLogMessage()});
            cursor.moveToFirst();
            return cursor.getInt(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
            lock.unlock();
        }
        return 0;
    }

    //删除指定的日志
    public void removeLogs(Context context, List<TraceLog> logs) {
        checkDB(context);
        if (mdbHelper == null) {
            return;
        }

        SQLiteDatabase db = null;
        try {
            lock.lock();

            db = mdbHelper.getWritableDatabase();
            for (TraceLog info : logs) {
                db.delete(TraceLogColumns.TABLE, TraceLogColumns._ID + "=? and " + TraceLogColumns.METHOD_INFO + "= ?", new String[]{info.getId() + "", info.getMethodInfo()});
            }
        } finally {
            if (db != null) {
                db.close();
            }
            lock.unlock();
        }
    }


}
