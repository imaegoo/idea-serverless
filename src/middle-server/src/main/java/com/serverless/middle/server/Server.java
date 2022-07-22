package com.serverless.middle.server;

import com.serverless.middle.remote.MiddleRemotingServer;

public class Server {

    public static void start(String[] args) throws Exception {
        MiddleRemotingServer server = new MiddleRemotingServer();
        server.init();
    }
}
