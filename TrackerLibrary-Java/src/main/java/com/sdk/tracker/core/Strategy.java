package com.sdk.tracker.core;

import android.content.Context;

import java.io.File;

public class Strategy {

    /**
     * 每天上报限制
     */
    long dailyReportLimit = 20;


    public static class Builder {
        long dailyReportLimit = 20;

        public Builder(Context context) {
        }


        public Builder setDailyReportLimit(long limit) {
            this.dailyReportLimit = limit;
            return this;
        }

        public Strategy build() {
            Strategy strategy = new Strategy();
            strategy.dailyReportLimit = dailyReportLimit;
            return strategy;
        }
    }

    public long getDailyReportLimit() {
        return dailyReportLimit;
    }
}
