package com.sdk.tracker;

public interface TrackerResultListener {
    /**
     * 成功
     */
    void onSuccess();

    /**
     * 失败
     */
    void onFail();
}
