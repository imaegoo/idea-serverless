package com.serverless.middle.remote;

import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectorServer extends AbstractRemotingBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectorServer.class);

    public ProjectorServer(ServerConfig serverConfig) {
        super(serverConfig);
    }

    @Override
    public void start() {
        try {
            LOGGER.info("Projector will start");
            long start = System.currentTimeMillis();
            String startPatch = serverConfig.getProjectorStartShellPath();
            ShellUtils.exceShell(startPatch);
            LOGGER.info("projector started successful,cost:{}", System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOGGER.info("projector started failed,exception:{}", e);
        }
    }

    @Override
    public void shutdown() {

    }
}
