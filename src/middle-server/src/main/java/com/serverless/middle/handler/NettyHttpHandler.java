package com.serverless.middle.handler;

import com.serverless.middle.config.ServerConfig;
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NettyHttpHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpHandler.class);

    private WebSocket socket;

    public final ServerConfig serverConfig;

    private ProjectorServer projectorServer;


    public NettyHttpHandler(ServerConfig serverConfig, ProjectorServer server) {
        this.serverConfig = serverConfig;
        this.projectorServer = server;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
            if ("websocket".equals(fullHttpRequest.headers().get("Upgrade"))) {
                ctx.fireChannelRead(msg);
            }
            handleHttpRequest(fullHttpRequest, ctx.channel());
        }
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame webSocketFrame = (TextWebSocketFrame) msg;
            handleWebSocketFrameRequest(webSocketFrame, ctx.channel());
        }
    }

    private void handleWebSocketFrameRequest(TextWebSocketFrame webSocketFrame, Channel channel) throws InterruptedException {
        if (socket == null) {
            socket = ConnectionManager.getInstance(serverConfig).getWebSocket(channel);
        }
        int count = serverConfig.getRetryCount();
        while (!socket.send(webSocketFrame.text())) {
            if (count == 0) {
                throw new RuntimeException("send msg failed after retry");
            }
            LOGGER.error("send msg failed,will retry after 1000ms");
            TimeUnit.SECONDS.sleep(1000);
            if (socket != null && socket.send(webSocketFrame.text())) {
                break;
            }
            socket.close(1000, "send msg failed");
            socket = ConnectionManager.getInstance(serverConfig).getWebSocket(channel);
            ConnectionManager.getInstance(serverConfig).setConnectioned();
            count--;
        }
    }


    private void handleHttpRequest(FullHttpRequest request, Channel channel) throws IOException {
        LOGGER.info("uri:{}", request.uri());
        Response response = ConnectionManager.getInstance(serverConfig).doGet(request.uri());
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
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (socket == null) {
            socket = ConnectionManager.getInstance(serverConfig).getWebSocket(ctx.channel());
            ConnectionManager.getInstance(serverConfig).setConnectioned();
            LOGGER.info("build ws to projector");
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        ConnectionManager.getInstance(serverConfig).setUnConnectioned();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
