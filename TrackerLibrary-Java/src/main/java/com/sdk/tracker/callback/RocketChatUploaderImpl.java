package com.sdk.tracker.callback;

import com.sdk.tracker.TrackerResultListener;
import com.sdk.tracker.TrackerUploadListener;
import com.sdk.tracker.log.TraceLog;

import java.util.List;

/**
 * 默认实现上传到rocket
 */
public class RocketChatUploaderImpl extends RocketChatCallBackWithOutToken implements TrackerUploadListener {

    @Override
    public void uploadLogInfo(List<TraceLog> list, TrackerResultListener listener) {
        if (listener != null) {
            for (int i = 0; i < list.size(); i++) {
                postMessage(list.get(i).toString());
            }
        }
        if (listener != null) {
            listener.onSuccess();
        }
    }
}
