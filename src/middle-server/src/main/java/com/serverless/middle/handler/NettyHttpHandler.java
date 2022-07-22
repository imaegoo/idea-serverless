package com.serverless.middle.handler;

import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.controller.RetryController;
import com.serverless.middle.manager.ConnectionManager;
import com.serverless.middle.remote.ProjectorServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import kotlin.Pair;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyHttpHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpHandler.class);

    private WebSocket socket;

    public final ServerConfig serverConfig;

    private ProjectorServer projectorServer;

    private final ConnectionManager connectionManager;


    public NettyHttpHandler(ServerConfig serverConfig, ProjectorServer server) {
        this.serverConfig = serverConfig;
        this.projectorServer = server;
        connectionManager = ConnectionManager.getInstance(serverConfig, projectorServer);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
            if ("websocket".equals(fullHttpRequest.headers().get("Upgrade"))) {
                connectionManager.setConnectioned(ctx.channel());
                ctx.fireChannelRead(msg);
            }
            handleHttpRequest(fullHttpRequest, ctx.channel());
        }
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame webSocketFrame = (TextWebSocketFrame) msg;
            handleWebSocketFrameRequest(webSocketFrame, ctx.channel());
        }
    }

    private void handleWebSocketFrameRequest(TextWebSocketFrame webSocketFrame, Channel channel) throws Exception {
        if (socket == null) {
            socket = connectionManager.getWebSocket(channel);
        }
        int count = serverConfig.getRetryCount();
        RetryController.doRetry(count, 1000L,
                () -> socket.send(webSocketFrame.text()),
                () -> {
                    if (socket != null && socket.send(webSocketFrame.text())) {
                        return true;
                    }
                    try {
                        socket = connectionManager.getWebSocket(channel);
                        connectionManager.setConnectioned(channel);
                    } finally {
                        return false;
                    }
                });
    }


    private void handleHttpRequest(FullHttpRequest request, Channel channel) throws Exception {
        LOGGER.info("uri:{}", request.uri());
        RetryController.doRetry(serverConfig.getRetryCount(), 1000L, () -> {
            try {
                Response response = connectionManager.doGet(request.uri());
                byte[] bytes = response.body().bytes();
                ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.valueOf(response.code()), byteBuf);
                for (Pair<? extends String, ? extends String> header : response.headers()) {
                    fullHttpResponse.headers().set(header.getFirst(), header.getSecond());
                }
                if (channel.isActive()) {
                    channel.writeAndFlush(fullHttpResponse);
                }
                return true;
            } catch (Exception e) {
                LOGGER.error("go get error:", e);
                return false;
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().equals(connectionManager.getSocketChannel())) {
            connectionManager.setUnConnectioned();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
