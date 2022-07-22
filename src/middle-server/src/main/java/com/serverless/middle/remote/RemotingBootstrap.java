package com.serverless.middle.remote;

public interface RemotingBootstrap {

    /**
     * start server
     */
    void start() throws Exception;

    /**
     * shutdown server
     */
    void shutdown();
}
