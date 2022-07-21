package com.serverless.middle.server;

import com.serverless.middle.ShutdownHook;
import com.serverless.middle.remote.MiddleRemotingServer;

public class Server {

    public static void start(String[] args) {
        MiddleRemotingServer server = new MiddleRemotingServer();
        server.init();
    }
}
