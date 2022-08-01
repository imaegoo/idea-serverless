package com.serverless.middle.manager;

import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.http.OKHttpUtil;
import com.serverless.middle.oss.OSSManager;
import com.serverless.middle.remote.ProjectorServer;
import com.serverless.middle.thread.NamedThreadFactory;
import io.netty.channel.Channel;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.serverless.middle.config.ServerConfig.IDEA_CONFIG_PATCH;
import static com.serverless.middle.config.ServerConfig.IDEA_PROJECT_PATCH;

public class ConnectionManager {

    private static Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private static final String BASE_URL = "localhost:8887";

    private static final String HTTP_PROTOCOL = "http://";

    private static final String WS_PROTOCOL = "ws://";

    private static final String HTTP_URL = HTTP_PROTOCOL + BASE_URL;

    private static final String WS_URL = WS_PROTOCOL + BASE_URL;

    private static final String CONNECTION_CHECK_THREAD_PREFIX = "connectionCheck";

    private static volatile ConnectionManager instance;

    private static final Map<String, WebSocket> CHANNEL_SOCKET_MAP = new ConcurrentHashMap<>(1);

    private static final Map<String, OSSManager> WORKSPACE_OSS_MAP = new ConcurrentHashMap<>(1);

    private static final DelayQueue<DelayTask> queue = new DelayQueue<>();

    private final ScheduledExecutorService connectionCheck = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory(CONNECTION_CHECK_THREAD_PREFIX, 1));

    private final ScheduledExecutorService uploadIdea = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory(CONNECTION_CHECK_THREAD_PREFIX, 1));

    private final ServerConfig serverConfig;

    private ProjectorServer projectorServer;

    public static ConnectionManager getInstance(ServerConfig serverConfig, ProjectorServer projectorServer) {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager(serverConfig, projectorServer);
                }
            }
        }
        return instance;
    }

    public ConnectionManager(ServerConfig serverConfig, ProjectorServer projectorServer) {
        this.serverConfig = serverConfig;
        this.projectorServer = projectorServer;
        connectionCheck.scheduleAtFixedRate(new ConnectionRunnable(this.projectorServer),
                60, 1, TimeUnit.SECONDS);
        uploadIdea.scheduleAtFixedRate(new UploadRunnable(), 60, 60, TimeUnit.SECONDS);
    }

    public WebSocket getWebSocket(Channel channel) throws Exception {
        projectorServer.start();
        return CHANNEL_SOCKET_MAP.computeIfAbsent(channel.id().asLongText(),
                k -> OKHttpUtil.getInstance().doConnectionSocket(WS_URL, channel));
    }

    public Response doGet(String url) throws Exception {
        projectorServer.start();
        return OKHttpUtil.getInstance().doGetDownLoad(HTTP_URL + url);
    }

    public void setUnConnectioned(Channel channel) {
        if (CHANNEL_SOCKET_MAP.containsKey(channel.id().asLongText())) {
            queue.put(new DelayTask(serverConfig.getConnectionTimeout(), channel));
        }
    }

    public void initIdea(String workspace, String accessKeyId, String secretKey, String securityToken, String endPoint) {
        OSSManager ossManager = WORKSPACE_OSS_MAP.computeIfAbsent(workspace,
                (k) -> new OSSManager(endPoint, accessKeyId, secretKey, workspace, securityToken));
        ossManager.batchDownload(IDEA_PROJECT_PATCH, IDEA_PROJECT_PATCH);
        ossManager.batchDownload(IDEA_CONFIG_PATCH, IDEA_CONFIG_PATCH);
    }

    public void shutdown() {
        connectionCheck.shutdown();
        uploadIdea.shutdown();
        WORKSPACE_OSS_MAP.entrySet().stream().forEach(entry -> {
            OSSManager ossManager = entry.getValue();
            ossManager.batchUploadFile(new File(IDEA_PROJECT_PATCH));
            ossManager.batchUploadFile(new File(IDEA_PROJECT_PATCH));
        });
    }

    private static class DelayTask implements Delayed {

        private long expireTime;

        private Channel channel;

        private long start = System.currentTimeMillis();

        public DelayTask(Long expireTime, Channel channel) {
            this.expireTime = expireTime;
            this.channel = channel;
        }

        public Channel getChannel() {
            return channel;
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

        private ProjectorServer projectorServer;

        public ConnectionRunnable(ProjectorServer projectorServer) {
            this.projectorServer = projectorServer;
        }

        @Override
        public void run() {
            DelayTask delayTask = queue.poll();
            if (delayTask == null) {
                return;
            }
            WebSocket socket = CHANNEL_SOCKET_MAP.remove(delayTask.getChannel().id());
            if (socket != null) {
                socket.close(1000, "timeout close");
            }
            if (CHANNEL_SOCKET_MAP.size() == 0) {
                LOGGER.info("all channel closed,projector will be shutdown");
                this.projectorServer.destroyShell();
            }
        }
    }

    public static class UploadRunnable implements Runnable {

        @Override
        public void run() {
            WORKSPACE_OSS_MAP.entrySet().parallelStream().forEach(entry -> {
                OSSManager ossManager = entry.getValue();
                ossManager.batchUploadFile(new File(IDEA_PROJECT_PATCH));
                ossManager.batchUploadFile(new File(IDEA_PROJECT_PATCH));
            });
        }
    }
}
