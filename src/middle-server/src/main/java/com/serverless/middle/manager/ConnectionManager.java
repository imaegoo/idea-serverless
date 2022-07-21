package com.serverless.middle.manager;

import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.http.OKHttpUtil;
import com.serverless.middle.thread.NamedThreadFactory;
import io.netty.channel.Channel;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionManager {

    private static final String BASE_URL = "localhost:8887";

    private static final String HTTP_PROTOCOL = "http://";

    private static final String WS_PROTOCOL = "ws://";

    private static final String HTTP_URL = HTTP_PROTOCOL + BASE_URL;

    private static final String WS_URL = WS_PROTOCOL + BASE_URL;

    private static final String CONNECTION_CHECK_THREAD_PREFIX = "connectionCheck";

    private static volatile ConnectionManager instance;

    private static volatile AtomicBoolean isConnectioned = new AtomicBoolean(false);

    private static final DelayQueue<DelayTask> queue = new DelayQueue<>();

    private static final ScheduledExecutorService executeService = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory(CONNECTION_CHECK_THREAD_PREFIX, 1));

    private final ServerConfig serverConfig;

    public static ConnectionManager getInstance(ServerConfig serverConfig) {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager(serverConfig);
                }
            }
        }
        return instance;
    }

    public ConnectionManager(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        executeService.scheduleAtFixedRate(new ConnectionRunnable(), 60, 1, TimeUnit.SECONDS);
    }

    public WebSocket getWebSocket(Channel channel) {
        return OKHttpUtil.getInstance().doConnectionSocket(WS_URL, channel);
    }

    public Response doGet(String url) throws IOException {
        return OKHttpUtil.getInstance().doGetDownLoad(HTTP_URL + url);
    }

    public void setConnectioned() {
        this.isConnectioned.compareAndSet(false, true);
    }

    public void setUnConnectioned() {
        this.isConnectioned.compareAndSet(true, false);
        queue.put(new DelayTask(serverConfig.getConnectionTimeout()));
    }


    private static class DelayTask implements Delayed {
        private long expireTime;

        private long start = System.currentTimeMillis();

        public DelayTask(Long expireTime) {
            this.expireTime = expireTime;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return start + this.expireTime - System.currentTimeMillis();
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            return (int) (getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    private static class ConnectionRunnable implements Runnable {
        private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionRunnable.class);

        @Override
        public void run() {
            if (queue.poll() == null || isConnectioned.get()) {
                return;
            }
            LOGGER.info("projector will be shutdown");
        }
    }

}
