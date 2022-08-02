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

    private String securityToken;

    private String workspace;

    private String bucketName = System.getenv("OSS_BUCKET_NAME");

    private static OSSClientBuilder ossClientBuilder = new OSSClientBuilder();

    public OSSManager(String endpoint, String accessKeyId, String accessKeySecret, String workspace, String securityToken) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.workspace = workspace;
    }

    public OSSManager(String endpoint, String accessKeyId, String accessKeySecret, String workspace) {
        this(endpoint, accessKeyId, accessKeySecret, workspace, null);
    }

    public void batchUploadFile(File parentFile) {
        LOGGER.info("will upload path：{}", parentFile.getAbsolutePath());
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret, securityToken);
        try {
            if (parentFile.isDirectory()) {
                File[] files = parentFile.listFiles();
                for (File file : files) {
                    LOGGER.info("will upload {} to {}", file.getAbsolutePath(), workspace + "/" + file.getAbsolutePath());
                    ossClient.putObject(new PutObjectRequest(bucketName, workspace + "/" + file.getAbsolutePath(), file));
                }
            } else {
                ossClient.putObject(new PutObjectRequest(bucketName, workspace + "/" + parentFile.getAbsolutePath(), parentFile));
            }
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public void uploadFile(String path, File file) {
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret, securityToken);
        ossClient.putObject(new PutObjectRequest(bucketName, path, file));
    }

    public void batchDownload(String prefix, String targetPath) {
        File target = new File(targetPath);
        if (target.exists()) {
            return;
        }
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret, securityToken);
        LOGGER.info("endpoint:{}.accessKeyId:{}.accessKeySecret:{},securityToken:{},bucket:{}",
                endpoint,accessKeyId,accessKeySecret,securityToken,bucketName);
        try {
            String userPath = workspace + "/" + prefix;
            ObjectListing objectListing = ossClient.listObjects(bucketName, userPath);
            List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            for (OSSObjectSummary objectSummary : objectSummaries) {
                LOGGER.info("will download:{}", objectSummary.getKey());
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
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret, securityToken);
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

