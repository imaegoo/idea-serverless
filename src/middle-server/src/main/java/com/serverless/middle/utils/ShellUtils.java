package com.serverless.middle.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ShellUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellUtils.class);

    /**
     * @param pathOrCommand 脚本路径或者命令
     * @return
     */
    public static List<String> exceShell(String pathOrCommand) throws Exception {
        List<String> result = new ArrayList<>();
        BufferedInputStream in = null;
        BufferedReader br = null;
        try {
            // 执行脚本
            Process ps = Runtime.getRuntime().exec(pathOrCommand);
            int exitValue = ps.waitFor();
            if (0 != exitValue) {
                throw new RuntimeException("call shell failed. error code is :" + exitValue);
            }
            // 只能接收脚本echo打印的数据，并且是echo打印的最后一次数据
            in = new BufferedInputStream(ps.getInputStream());
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                LOGGER.info("shell output:" + line);
                result.add(line);
            }
        } finally {
            if (null != in) {
                in.close();
            }
            if (null != br) {
                br.close();
            }
        }
        return result;
    }
}
