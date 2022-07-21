package com.serverless.middle.remote;

import com.serverless.middle.config.ServerConfig;

public abstract class AbstractRemotingBootstrap implements RemotingBootstrap {

    protected final ServerConfig serverConfig;

    public AbstractRemotingBootstrap(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }
}
