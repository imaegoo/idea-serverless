package com.serverless.middle.remote;

import com.serverless.middle.config.ServerConfig;
import com.serverless.middle.thread.NamedThreadFactory;
import com.serverless.middle.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ProjectorServer extends AbstractRemotingBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectorServer.class);

    private final ExecutorService shellLog = new ThreadPoolExecutor(1, 1, 500L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(50), new NamedThreadFactory("shellog", 1),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private Process process;

    public ProjectorServer(ServerConfig serverConfig) {
        super(serverConfig);
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public void destroyShell() {
        if (isAlive()) {
            synchronized (ProjectorServer.class) {
                if (isAlive()) {
                    process.destroy();
                }
            }
        }
    }

    @Override
    public void start() throws Exception {
        if (!isAlive()) {
            synchronized (ProjectorServer.class) {
                if (!isAlive()) {
                    try {
                        LOGGER.info("Projector will start");
                        long start = System.currentTimeMillis();
                        String startPatch = serverConfig.getProjectorStartShellPath();
                        process = ShellUtils.exceShell(startPatch);
                        shellLog.submit(() -> {
                            try (BufferedInputStream in = new BufferedInputStream(process.getInputStream());
                                 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                                String line;
                                while (isAlive()) {
                                    while ((line = br.readLine()) != null) {
                                        LOGGER.info("shell output:" + line);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        LOGGER.info("projector started successful,cost:{}", System.currentTimeMillis() - start);
                    } catch (Exception e) {
                        if (isAlive()) {
                            process.destroy();
                        }
                        LOGGER.error("projector started failed,exception:", e);
                        throw e;
                    }
                }
            }
        } else {
            LOGGER.info("Projector is already started");
        }
    }

    @Override
    public void shutdown() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        shellLog.shutdown();
    }
}
