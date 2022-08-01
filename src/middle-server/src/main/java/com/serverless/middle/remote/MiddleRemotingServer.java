package com.serverless.middle.remote;

import com.serverless.middle.Disposable;
import com.serverless.middle.ShutdownHook;
import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.manager.ConnectionManager;

public class MiddleRemotingServer implements RemotingServer, Disposable {

    private SeverRemotingBootstrap severRemotingBootstrap;

    private ProjectorServer projectorServer;

    private ConnectionManager connectionManager;

    public MiddleRemotingServer() {
        ServerConfig serverConfig = new ServerConfig();
        projectorServer = new ProjectorServer(serverConfig);
        severRemotingBootstrap = new SeverRemotingBootstrap(serverConfig, projectorServer);
        connectionManager = ConnectionManager.getInstance(serverConfig, projectorServer);

    }

    @Override
    public void init() throws Exception {
        projectorServer.start();
        severRemotingBootstrap.start();
        ShutdownHook.getInstance().addDisposable(this);
    }

    @Override
    public void destroy() {
        severRemotingBootstrap.shutdown();
        projectorServer.shutdown();
        connectionManager.shutdown();
    }
}
