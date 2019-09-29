package com.sdk.tracker.listener;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.sdk.tracker.TrackerResultListener;
import com.sdk.tracker.TrackerUploadListener;
import com.sdk.tracker.Trackers;
import com.sdk.tracker.db.ContextGetter;
import com.sdk.tracker.db.TrackerStoreHelper;
import com.sdk.tracker.log.TraceLog;

import java.util.List;
import java.util.logging.Logger;

public class DefaultTrackerUploadListener implements TrackerUploadListener {

    @Override
    public void uploadLogInfo(List<TraceLog> list, TrackerResultListener listener) {
        if (checkLimit() && list != null) {
            list.clear();
        }
    }

    //是否超过限制了
    protected boolean checkLimit() {
        if (isNetAvailable(ContextGetter.getContext())) {
            if (TrackerStoreHelper.instance().getDailyUploadCount() > Trackers.instance().getStrategy().getDailyReportLimit()) {
                Log.e("Trackers", "upload exceed limit");
                return true;
            }
            TrackerStoreHelper.instance().addDailyUploadCount();
        }
        return false;
    }

    /**
     * 判断网络状态是否可用
     *
     * @param context
     * @return
     */
    private boolean isNetAvailable(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info == null) {
                return false;
            }
            return info.isAvailable();
        }
        return false;
    }
}
