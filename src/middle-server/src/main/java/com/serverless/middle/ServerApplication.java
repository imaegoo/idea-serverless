package com.serverless.middle;

import com.serverless.middle.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) throws Exception {
        long beginTime = System.currentTimeMillis();
        Server.start(args);
        LOGGER.info("server start cost:{}ms", System.currentTimeMillis() - beginTime);
    }
}
