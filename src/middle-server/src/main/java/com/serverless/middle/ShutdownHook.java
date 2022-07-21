package com.serverless.middle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownHook extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);

    /**
     * default 10. Lower values have higher priority
     */
    private static final int DEFAULT_PRIORITY = 10;

    private static final ShutdownHook SHUTDOWN_HOOK = new ShutdownHook("ShutdownHook");

    private final PriorityQueue<DisposablePriorityWrapper> disposables = new PriorityQueue<>();

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    static {
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);
    }

    private ShutdownHook(String name) {
        destroyAll();
    }

    public static ShutdownHook getInstance() {
        return SHUTDOWN_HOOK;
    }

    private void destroyAll() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        if (disposables.isEmpty()) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("destroyAll starting");
        }

        while (!disposables.isEmpty()) {
            Disposable disposable = disposables.poll();
            disposable.destroy();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("destroyAll finish");
        }
    }


    public void addDisposable(Disposable disposable) {
        addDisposable(disposable, DEFAULT_PRIORITY);
    }

    public void addDisposable(Disposable disposable, int priority) {
        disposables.add(new DisposablePriorityWrapper(disposable, priority));
    }

    @Override
    public void run() {
        super.run();
    }


    private static class DisposablePriorityWrapper implements Comparable<DisposablePriorityWrapper>, Disposable {

        private final Disposable disposable;

        private final int priority;

        public DisposablePriorityWrapper(Disposable disposable, int priority) {
            this.disposable = disposable;
            this.priority = priority;
        }

        @Override
        public int compareTo(DisposablePriorityWrapper challenger) {
            return priority - challenger.priority;
        }

        @Override
        public void destroy() {
            disposable.destroy();
        }
    }
}
