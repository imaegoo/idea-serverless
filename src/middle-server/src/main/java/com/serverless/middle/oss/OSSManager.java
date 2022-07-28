package com.serverless.middle.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectRequest;
import com.serverless.middle.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class OSSManager implements Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSSManager.class);

    private static String endpoint = "oss-cn-hangzhou.aliyuncs.com";
    private static String accessKeyId = "LTAI5t6Z4W1tq74CjiwvB42e";
    private static String accessKeySecret = "8hd9h785eyfrgPVWetyw3rTW6ixtkQ";

    private static String bucketName = System.getenv(OSSManagerFactory.OSS_BUCKET_KEY);

    private static volatile OSS ossInstance;

    private OSSManager() {
        ossInstance = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    public void uploadFile(String path, File file) {
        ossInstance.putObject(new PutObjectRequest(bucketName, path, file));
    }

    public void downloadList(String prefix, String targetPath) {
        ObjectListing objectListing = ossInstance.listObjects(bucketName, prefix);
        List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        for (OSSObjectSummary objectSummary : objectSummaries) {
            LOGGER.debug("will download:{}", objectSummary.getKey());
            String key = objectSummary.getKey();
            String simpleFileName = getSimpleFileName(prefix, key);
            File file = new File(targetPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            downloadFile(key, file + "/" + simpleFileName);
        }
    }

    public void downloadFile(String key, String targetPath) {
        ossInstance.getObject(new GetObjectRequest(bucketName, key), new File(targetPath));
    }

    private String getSimpleFileName(String prefix, String key) {
        return key.substring(prefix.length());
    }

    @Override
    public void destroy() {
        if (ossInstance != null) {
            ossInstance.shutdown();
        }
    }

    public static class Singleton {
        private static final OSSManager INSTANCE = new OSSManager();

        public OSSManager getInstance() {
            return Singleton.INSTANCE;
        }
    }
}

