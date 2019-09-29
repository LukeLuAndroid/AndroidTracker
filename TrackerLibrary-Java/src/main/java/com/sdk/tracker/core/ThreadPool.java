package com.sdk.tracker.core;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    //参数初始化
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static ExecutorService ioExecutorService = new ThreadPoolExecutor(2 * CPU_COUNT + 1,
            2 * CPU_COUNT + 1,
            30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128));

    private static Executor UiExecutor = new MainThreadExecutor();

    //线程的创建工厂
    static ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }

    private static ExecutorService single = Executors.newSingleThreadExecutor(threadFactory);

    public static void submitIoTask(Runnable runnable) {
        ioExecutorService.execute(runnable);
    }

    public static void submitSingleTask(Runnable runnable) {
        single.execute(runnable);
    }

    public static void submitUiThead(Runnable runnable) {
        UiExecutor.execute(runnable);
    }

}
