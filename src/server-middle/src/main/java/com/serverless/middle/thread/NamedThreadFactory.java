package com.serverless.middle.thread;

import io.netty.util.concurrent.FastThreadLocalThread;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final static Map<String, AtomicInteger> PREFIX_COUNTER = new ConcurrentHashMap<>();
    private final ThreadGroup group;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final String prefix;
    private final int totalSize;
    private final boolean makeDaemons;

    /**
     * Instantiates a new Named thread factory.
     *
     * @param prefix      the prefix
     * @param totalSize   the total size
     * @param makeDaemons the make daemons
     */
    public NamedThreadFactory(String prefix, int totalSize, boolean makeDaemons) {
        int prefixCounter = PREFIX_COUNTER.computeIfAbsent(prefix, k -> new AtomicInteger(0)).incrementAndGet();
        SecurityManager securityManager = System.getSecurityManager();
        group = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().
                getThreadGroup();
        this.prefix = prefix + "_" + prefixCounter;
        this.makeDaemons = makeDaemons;
        this.totalSize = totalSize;
    }

    /**
     * Instantiates a new Named thread factory.
     *
     * @param prefix      the prefix
     * @param makeDaemons the make daemons
     */
    public NamedThreadFactory(String prefix, boolean makeDaemons) {
        this(prefix, 0, makeDaemons);
    }

    /**
     * Instantiates a new Named thread factory.
     *
     * @param prefix    the prefix
     * @param totalSize the total size
     */
    public NamedThreadFactory(String prefix, int totalSize) {
        this(prefix, totalSize, true);
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = prefix + "_" + counter.incrementAndGet();
        if (totalSize > 1) {
            name += "_" + totalSize;
        }
        Thread thread = new FastThreadLocalThread(group, r, name);

        thread.setDaemon(makeDaemons);
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}