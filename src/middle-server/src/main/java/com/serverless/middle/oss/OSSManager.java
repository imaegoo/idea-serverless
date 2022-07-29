package com.serverless.middle.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class OSSManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSSManager.class);

    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String workspace;

    private String bucketName = "twl-serverless";

    private static OSSClientBuilder ossClientBuilder = new OSSClientBuilder();

    public OSSManager(String endpoint, String accessKeyId, String accessKeySecret, String workspace) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.workspace = workspace;
    }

    public void batchUploadFile(File parentFile) {
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret);
        try {
            if (parentFile.isDirectory()) {
                File[] files = parentFile.listFiles();
                for (File file : files) {
                    ossClient.putObject(new PutObjectRequest(bucketName, workspace + "/" + file.getPath(), file));
                }
            } else {
                ossClient.putObject(new PutObjectRequest(bucketName, workspace + "/" + parentFile.getPath(), parentFile));
            }
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public void uploadFile(String path, File file) {
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret);
        ossClient.putObject(new PutObjectRequest(bucketName, path, file));
    }

    public void batchDownload(String prefix, String targetPath) {
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret);
        try {
            String userPath = workspace + "/" + prefix;
            ObjectListing objectListing = ossClient.listObjects(bucketName, userPath);
            List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            for (OSSObjectSummary objectSummary : objectSummaries) {
                LOGGER.debug("will download:{}", objectSummary.getKey());
                String key = objectSummary.getKey();
                String simpleFileName = getSimpleFileName(userPath, key);
                File file = new File(targetPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                ossClient.getObject(new GetObjectRequest(bucketName, key),
                        new File(file.getAbsolutePath() + "/" + simpleFileName));
            }
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public void downloadFile(String key, String targetPath) {
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.getObject(new GetObjectRequest(bucketName, key), new File(targetPath));
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String getSimpleFileName(String prefix, String key) {
        return key.substring(prefix.length());
    }

}

