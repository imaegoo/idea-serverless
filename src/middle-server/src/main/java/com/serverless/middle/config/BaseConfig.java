package com.serverless.middle.config;

import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.NettyRuntime;

public class BaseConfig {
    protected static final int DEFAULT_NETTY_BOSS_THREAD_SIZE = 1;

    protected static final String DEFAULT_NETTY_BOSS_THREAD_PREFIX = "NettyBossThread";

    protected static final int DEFAULT_NETTY_WORKER_THREAD_SIZE = NettyRuntime.availableProcessors() * 2;

    protected static final String DEFAULT_NETTY_WORKER_THREAD_PREFIX = "NettyWorkerThread";

    protected static final Class<? extends ServerChannel> SERVER_CHANNEL_CLAZZ = NioServerSocketChannel.class;

}
