package com.serverless.middle.remote;

public interface RemotingBootstrap {

    /**
     * start server
     */
    void start();

    /**
     * shutdown server
     */
    void shutdown();
}
