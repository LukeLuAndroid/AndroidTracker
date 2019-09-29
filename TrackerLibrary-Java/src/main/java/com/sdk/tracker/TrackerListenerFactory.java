package com.sdk.tracker;

public class TrackerListenerFactory {
    private static TrackerStoreListener DEFAULT_SAVE;
    private static TrackerUploadListener DEFAULT_UPLOAD;

    static {
        try {
            Class save = Class.forName("com.sdk.tracker.listener.DefaultTrackerStoreListener");
            if (save != null) {
                DEFAULT_SAVE = (TrackerStoreListener) save.newInstance();
            }

            Class upload = Class.forName("com.sdk.tracker.listener.DefaultTrackerUploadListener");
            if (upload != null) {
                DEFAULT_UPLOAD = (TrackerUploadListener) upload.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TrackerStoreListener getDefaultStoreListener() {
        return DEFAULT_SAVE;
    }

    public static TrackerUploadListener getDefaultUploadListener() {
        return DEFAULT_UPLOAD;
    }
}
