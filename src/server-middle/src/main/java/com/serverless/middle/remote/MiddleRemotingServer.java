package com.serverless.middle.remote;

import com.serverless.middle.Disposable;
import com.serverless.middle.ShutdownHook;
import com.serverless.middle.config.ServerConfig;

public class MiddleRemotingServer implements RemotingServer, Disposable {

    private SeverRemotingBootstrap severRemotingBootstrap;

    private ProjectorServer projectorServer;

    public MiddleRemotingServer() {
        ServerConfig serverConfig = new ServerConfig();
        projectorServer = new ProjectorServer(serverConfig);
        severRemotingBootstrap = new SeverRemotingBootstrap(serverConfig, projectorServer);

    }

    @Override
    public void init() {
        projectorServer.start();
        severRemotingBootstrap.start();
        ShutdownHook.getInstance().addDisposable(this);
    }

    @Override
    public void destroy() {
        severRemotingBootstrap.shutdown();
        projectorServer.shutdown();
    }
}
