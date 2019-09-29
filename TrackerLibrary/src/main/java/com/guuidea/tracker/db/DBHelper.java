package com.guuidea.tracker.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.guuidea.tracker.db.column.TraceLogColumns;

public class DBHelper extends SQLiteOpenHelper {
    static final int DB_VERSION = 1;
    static final String DB_NAME = "trace_log";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TraceLogColumns.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int v = oldVersion; v <= newVersion; v++) {
            switch (v) {
                case 2:
                    break;
            }
        }
    }
}
