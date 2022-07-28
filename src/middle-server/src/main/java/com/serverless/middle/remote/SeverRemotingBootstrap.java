package com.serverless.middle.remote;

import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.handler.NettyHttpHandler;
import com.serverless.middle.thread.NamedThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeverRemotingBootstrap extends AbstractRemotingBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeverRemotingBootstrap.class);

    private final ServerBootstrap serverBootstrap = new ServerBootstrap();

    private final EventLoopGroup eventLoopGroupBoss;

    private final EventLoopGroup eventLoopGroupWorker;

    private final ProjectorServer projectorServer;


    public SeverRemotingBootstrap(ServerConfig serverConfig, ProjectorServer projectorServer) {
        super(serverConfig);
        this.projectorServer = projectorServer;
//        if (Epoll.isAvailable()) {
//            eventLoopGroupBoss = new EpollEventLoopGroup(serverConfig.getBossThreadSize(),
//                    new NamedThreadFactory(serverConfig.getBossThreadPrefix(), serverConfig.getBossThreadSize()));
//            eventLoopGroupWorker = new EpollEventLoopGroup(serverConfig.getWorkerThreadSize(),
//                    new NamedThreadFactory(serverConfig.getWorkerThreadPrefix(), serverConfig.getWorkerThreadSize()));
//        } else {
            eventLoopGroupBoss = new NioEventLoopGroup(serverConfig.getBossThreadSize(),
                    new NamedThreadFactory(serverConfig.getBossThreadPrefix(), serverConfig.getBossThreadSize()));
            eventLoopGroupWorker = new NioEventLoopGroup(serverConfig.getWorkerThreadSize(),
                    new NamedThreadFactory(serverConfig.getWorkerThreadPrefix(), serverConfig.getWorkerThreadSize()));
//        }
    }

    @Override
    public void start() {
        this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupBoss)
                .channel(ServerConfig.SERVER_CHANNEL_CLAZZ)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .localAddress(ServerConfig.DEFAULT_SERVER_LOCAL_HOST, ServerConfig.DEFAULT_SERVER_LOCAL_PORT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        // 将http消息的多个部分组合成一条完整的HTTP消息
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                        // 向客户端发送HTML5文件。主要用于支持浏览器和服务端进行WebSocket通信
                        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                        pipeline.addLast(new NettyHttpHandler(serverConfig, projectorServer));
                        WebSocketServerProtocolHandler webSocketServerProtocolHandler =
                                new WebSocketServerProtocolHandler("/", "WebSocket",
                                        true, 65536 * 10);
                        pipeline.addLast(webSocketServerProtocolHandler);
                    }
                });
        try {
            ChannelFuture future = this.serverBootstrap.bind(ServerConfig.DEFAULT_SERVER_LOCAL_HOST,
                    ServerConfig.DEFAULT_SERVER_LOCAL_PORT).sync();
            if (future.isSuccess()) {
                LOGGER.info("server start success ip:{}", future.channel().localAddress().toString());
            } else {
                System.out.println("server start failed");
                future.cause().printStackTrace();
                eventLoopGroupBoss.shutdownGracefully();
                eventLoopGroupBoss.shutdownGracefully();
            }
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("server start failed,cause:{}", e.getCause());
        }
    }

    @Override
    public void shutdown() {
        if (this.eventLoopGroupBoss != null) {
            this.eventLoopGroupBoss.shutdownGracefully();
        }
        if (this.eventLoopGroupWorker != null) {
            this.eventLoopGroupWorker.shutdownGracefully();
        }
    }
}
