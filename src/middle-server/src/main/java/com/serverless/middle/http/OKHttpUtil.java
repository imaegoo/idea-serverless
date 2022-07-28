package com.serverless.middle.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class OKHttpUtil {
    private static volatile OKHttpUtil okHttpFactory = null;

    private static OkHttpClient okHttpClient = null;

    public OKHttpUtil() {

    }

    public static OKHttpUtil getInstance() {
        if (okHttpFactory == null) {
            synchronized (OKHttpUtil.class) {
                if (okHttpFactory == null) {
                    okHttpFactory = new OKHttpUtil();
                }
            }
        }
        return okHttpFactory;
    }

    private static synchronized OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            ConnectionPool connectionPool = new ConnectionPool(50, 60, TimeUnit.SECONDS);
            okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .connectionPool(connectionPool)
                    .build();
        }
        return okHttpClient;
    }

    public Response doGetDownLoad(String url, HttpHeaders headers, Consumer<HttpHeaders> consumer) throws IOException {
        Request.Builder builder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            consumer.accept(headers);
        }
        Request request = builder.get().url(url).build();
        return getOkHttpClient().newCall(request).execute();
    }

    public Response doGetDownLoad(String url) throws IOException {
        return doGetDownLoad(url, null, null);
    }

    public WebSocket doConnectionSocket(String url, Channel channel) {
        return this.doConnectionSocket(url, channel, null, null);
    }

    public WebSocket doConnectionSocket(String url, Channel channel,
                                        HttpHeaders headers, Consumer<HttpHeaders> consumer) {
        Request.Builder builder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            consumer.accept(headers);
        }
        Request request = builder.get().url(url).build();
        return getOkHttpClient().newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                if (channel.isActive()) {
                    channel.writeAndFlush(new TextWebSocketFrame(text));
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                if (channel.isActive()) {
                    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes.toByteArray());
                    channel.writeAndFlush(new BinaryWebSocketFrame(byteBuf));
                }
            }
        });
    }

    public void doPost(String url, Map<String, String> params, Callback callback) {
        // 请求体
        FormBody.Builder builder = new FormBody.Builder();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            String value = params.get(key);
            builder.add(key, value);
        }
        FormBody formBody = builder.build();

        Request reQuest = new Request.Builder()
                .post(formBody)
                .url(url)
                .build();

        getOkHttpClient().newCall(reQuest).enqueue(callback);
    }


    public void doPostJson(String url, String json, Callback callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), json);
        Request reQuest = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();

        getOkHttpClient().newCall(reQuest).enqueue(callback);
    }
}
