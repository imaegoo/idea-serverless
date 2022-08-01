package com.serverless.middle.handler;

import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.controller.RetryController;
import com.serverless.middle.manager.ConnectionManager;
import com.serverless.middle.remote.ProjectorServer;
import com.serverless.middle.utils.HttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.StringUtil;
import kotlin.Pair;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class NettyHttpHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpHandler.class);

    public final ServerConfig serverConfig;

    private ProjectorServer projectorServer;

    private final ConnectionManager connectionManager;

    private static final String WORKSPACE_KEY = "workspace";


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
                connectionManager.getWebSocket(ctx.channel());
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
        final WebSocket[] socket = {connectionManager.getWebSocket(channel)};
        int count = serverConfig.getRetryCount();
        RetryController.doRetry(count, 1000L,
                () -> socket[0].send(webSocketFrame.text()),
                () -> {
                    if (socket[0] != null && socket[0].send(webSocketFrame.text())) {
                        return true;
                    }
                    try {
                        socket[0] = connectionManager.getWebSocket(channel);
                    } catch (Exception e) {
                        LOGGER.error("build socket failed");
                    } finally {
                        return false;
                    }
                });
    }


    private void handleHttpRequest(FullHttpRequest request, Channel channel) throws Exception {
        LOGGER.info("uri:{}", request.uri());
        if ("/".equals(HttpUtils.getUrlWithoutParams(request.uri()))) {
            String workspace = HttpUtils.parseRequestParams(request.uri()).get(WORKSPACE_KEY);
            String accessKeyId = request.headers().get("X-Fc-Access-Key-Id");
            String keySecret = request.headers().get("X-Fc-Access-Key-Secret");
            String securityToken = request.headers().get("X-Fc-Security-Token");
            String endpoint = HttpUtils.buildOSSEndpoint(request.headers().get("X-Fc-Region"));
            if (StringUtil.isNullOrEmpty(workspace)) {
                workspace = UUID.randomUUID().toString();
                connectionManager.initIdea(workspace, accessKeyId, keySecret, securityToken, endpoint);
                doRedirect(workspace, channel);
            } else {
                connectionManager.initIdea(workspace, accessKeyId, keySecret, securityToken, endpoint);
                transportRequest(request, channel);
            }
        } else {
            transportRequest(request, channel);
        }
    }

    private void transportRequest(FullHttpRequest request, Channel channel) throws Exception {
        RetryController.doRetry(serverConfig.getRetryCount(), 1000L, () -> {
            Response response = null;
            try {
                request.headers().forEach(entry -> {
                    LOGGER.info("request-header:{}----------->{}", entry.getKey(), entry.getValue());
                });
                response = connectionManager.doGet(request.uri());
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
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        });
    }

    private void doRedirect(String workspace, Channel channel) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND); //设置重定向响应码 （临时重定向、永久重定向）
        HttpHeaders headers = response.headers();
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with,content-type");
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "POST,GET");
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        headers.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        String url = "?workspace=" + workspace;
        headers.set(HttpHeaderNames.LOCATION, url); //重定向URL设置
        channel.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);//解决写入完成后，客户端断开会报异常的问题
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionManager.setUnConnectioned(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
