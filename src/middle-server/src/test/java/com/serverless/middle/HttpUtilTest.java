package com.serverless.middle;

import com.serverless.middle.utils.HttpUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class HttpUtilTest {

    @Test
    public void testParser() {
        String url = "/?workspace=abcdef&user=sssss";
        Map<String, String> map = HttpUtils.parseRequestParams(url);
        Assertions.assertEquals(map.get("workspace"), "abcdef");
        Assertions.assertEquals(map.get("user"), "sssss");

        url = "/";
        map = HttpUtils.parseRequestParams(url);
        Assertions.assertNull(map.get("workspace"));
    }

    @Test
    public void testGetUrlWithoutParams() {
        String url = "/?workspace=abcdef&user=sssss";
        Assertions.assertEquals("/", HttpUtils.getUrlWithoutParams(url));
        url = "/";
        Assertions.assertEquals("/", HttpUtils.getUrlWithoutParams(url));
    }

    @Test
    public void testBuildOSSEndpoint() {
        String region = "cn-hangzhou";
        Assertions.assertEquals("https://oss-cn-hangzhou.aliyuncs.com", HttpUtils.buildOSSEndpoint(region));
    }
}
