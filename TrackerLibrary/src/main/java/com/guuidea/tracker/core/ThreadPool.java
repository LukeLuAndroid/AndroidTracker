package com.guuidea.tracker.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    //参数初始化
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    //核心线程数量大小
    private static final int corePoolSize = Math.max(2, Math.min(CPU_COUNT - 1, 4));

    //线程的创建工厂
    static ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static ExecutorService service = Executors.newFixedThreadPool(corePoolSize, threadFactory);
    private static ExecutorService single = Executors.newSingleThreadExecutor(threadFactory);

    public static void submitTask(Runnable runnable) {
        service.execute(runnable);
    }

    public static void submitSingleTask(Runnable runnable) {
        single.execute(runnable);
    }

}
