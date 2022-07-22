package com.serverless.middle.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellUtils.class);

    /**
     * @param pathOrCommand 脚本路径或者命令
     * @return
     */
    public static Process exceShell(String pathOrCommand) throws Exception {
        // 执行脚本
        LOGGER.info("begin exec the shell");
        return Runtime.getRuntime().exec(pathOrCommand);
    }
}
