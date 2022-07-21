package com.serverless.middle.config;

import io.netty.channel.ServerChannel;
import io.netty.util.internal.StringUtil;

public class ServerConfig extends BaseConfig {

    private static final String NETTY_BOSS_THREAD_SIZE_KEY = "netty.boss.thread.size";

    private static final String NETTY_BOSS_THREAD_PREFIX_KEY = "netty.boss.thread.prefix";

    private static final String NETTY_WORKER_THREAD_SIZE_KEY = "netty.worker.thread.size";

    private static final String NETTY_WORKER_THREAD_PREFIX_KEY = "netty.worker.thread.prefix";

    private static final String WEB_SOCKET_SEND_RETRY_COUNT_KEY = "websocket.retry.count";

    private static final String PROJECTOR_START_SHELL_PATCH_KEY = "projector.start.shell";

    private static final String PROJECTOR_CONNECTION_TIMEOUT_KEY = "projector.timeout";

    private static final int DEFAULT_WEB_SOCKET_SEND_RETRY_COUNT = 5;

    public static final Class<? extends ServerChannel> SERVER_CHANNEL_CLAZZ = BaseConfig.SERVER_CHANNEL_CLAZZ;

    public static final String DEFAULT_SERVER_LOCAL_HOST = "0.0.0.0";

    public static final int DEFAULT_SERVER_LOCAL_PORT = 7887;

    private static final String DEFAULT_PROJECTOR_START_SHELL_PATCH = "/code/start.sh";

    private static final long DEFAULT_PROJECTOR_CONNECTION_TIMEOUT = 1000 * 20;


    /**
     * get netty boss thread size
     *
     * @return
     */
    public int getBossThreadSize() {
        String threadSizeStr = System.getProperty(NETTY_BOSS_THREAD_SIZE_KEY);
        if (StringUtil.isNullOrEmpty(threadSizeStr)) {
            return DEFAULT_NETTY_BOSS_THREAD_SIZE;
        }
        return Integer.valueOf(threadSizeStr);
    }

    /**
     * get netty boss thread prefix
     *
     * @return
     */
    public String getBossThreadPrefix() {
        return System.getProperty(NETTY_BOSS_THREAD_PREFIX_KEY, DEFAULT_NETTY_BOSS_THREAD_PREFIX);
    }

    /**
     * get netty worker thread size
     *
     * @return
     */
    public int getWorkerThreadSize() {
        String threadSizeStr = System.getProperty(NETTY_WORKER_THREAD_SIZE_KEY);
        if (StringUtil.isNullOrEmpty(threadSizeStr)) {
            return DEFAULT_NETTY_WORKER_THREAD_SIZE;
        }
        return Integer.valueOf(threadSizeStr);
    }

    /**
     * get netty worker thread prefix
     *
     * @return
     */
    public String getWorkerThreadPrefix() {
        return System.getProperty(NETTY_WORKER_THREAD_PREFIX_KEY, DEFAULT_NETTY_WORKER_THREAD_PREFIX);
    }

    /**
     * get websocket send retry count
     *
     * @return
     */
    public int getRetryCount() {
        String threadSizeStr = System.getProperty(WEB_SOCKET_SEND_RETRY_COUNT_KEY);
        if (StringUtil.isNullOrEmpty(threadSizeStr)) {
            return DEFAULT_WEB_SOCKET_SEND_RETRY_COUNT;
        }
        return Integer.valueOf(threadSizeStr);
    }

    /**
     * get project shell patch
     *
     * @return
     */
    public String getProjectorStartShellPath() {
        return System.getProperty(PROJECTOR_START_SHELL_PATCH_KEY, DEFAULT_PROJECTOR_START_SHELL_PATCH);
    }

    public Long getConnectionTimeout() {
        String threadSizeStr = System.getProperty(PROJECTOR_CONNECTION_TIMEOUT_KEY);
        if (StringUtil.isNullOrEmpty(threadSizeStr)) {
            return DEFAULT_PROJECTOR_CONNECTION_TIMEOUT;
        }
        return Long.valueOf(threadSizeStr);
    }
}
