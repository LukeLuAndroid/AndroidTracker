package com.guuidea.tracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.guuidea.tracker.db.column.TraceLogColumns;
import com.guuidea.tracker.log.TraceLog;

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
            values.put(TraceLogColumns.LOG_TIME, log.getLogTime());
            values.put(TraceLogColumns.LOG_TAG, log.getParams());

            db.insert(TraceLogColumns.TABLE, null, values);
        } finally {
            if (db != null) {
                db.close();
            }
            lock.unlock();
        }
        Log.e(TAG, "insert message into db");
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
                    String log_tag = cursor.getString(cursor.getColumnIndex(TraceLogColumns.LOG_TAG));
                    TraceLog log = TraceLog.obtain();
                    log.setLogMessage(log_message);
                    log.setThreadName(thread_name);
                    log.setMethodInfo(method_info);
                    log.setParams(log_tag);
                    log.setLogTime(log_time);
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

        if (mdbHelper == null) {
            return 0;
        }
        String sql = " SELECT COUNT ( *)FROM " + TraceLogColumns.TABLE + " where " + TraceLogColumns.METHOD_INFO + "=?";
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            lock.lock();
            db = mdbHelper.getReadableDatabase();
            cursor = db.rawQuery(sql, new String[]{log.getMethodInfo()});
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

            List<String> methods = new ArrayList<>();
            for (TraceLog log : logs) {
                if (!methods.contains(log.getMethodInfo())) {
                    methods.add(log.getMethodInfo());
                }
            }

            db = mdbHelper.getWritableDatabase();
            for (String info : methods) {
                db.delete(TraceLogColumns.TABLE, TraceLogColumns.METHOD_INFO + "= ?", new String[]{info});
            }
        } finally {
            if (db != null) {
                db.close();
            }
            lock.unlock();
        }
    }


}
