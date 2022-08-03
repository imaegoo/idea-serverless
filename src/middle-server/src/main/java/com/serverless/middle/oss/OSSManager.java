package com.serverless.middle.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectRequest;
import com.serverless.middle.utils.TarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

    private static final String TEMP_PATH = "/root/IdeaTemp/";

    private static final String IDEA_PATH = "/root/";

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

    public void batchUploadFile(String parent) {
        LOGGER.info("will upload path：{}", parent);
        File parentFile = new File(parent);
        String tarName = parentFile.getName() + ".tar.gz";
        String tarPath = TEMP_PATH + tarName;
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret, securityToken);
        try {
            TarUtils.compressTar(parent, tarPath);
            LOGGER.info("will upload {} to {}", tarPath, workspace + "/" + tarName);
            ossClient.putObject(new PutObjectRequest(bucketName, workspace + "/" + tarName,
                    new File(tarPath)));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
            File tarFile = new File(tarPath);
            tarFile.delete();
        }
    }

    public void uploadFile(String path, File file) {
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret, securityToken);
        ossClient.putObject(new PutObjectRequest(bucketName, path, file));
    }

    public void batchDownload() {
        OSS ossClient = ossClientBuilder.build(endpoint, accessKeyId, accessKeySecret, securityToken);
        LOGGER.info("endpoint:{}.accessKeyId:{}.accessKeySecret:{},securityToken:{},bucket:{}",
                endpoint, accessKeyId, accessKeySecret, securityToken, bucketName);
        try {
            ObjectListing objectListing = ossClient.listObjects(bucketName, workspace);
            List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            for (OSSObjectSummary objectSummary : objectSummaries) {
                LOGGER.info("will download:{}", objectSummary.getKey());
                String key = objectSummary.getKey();
                String simpleFileName = getSimpleFileName(workspace, key);
                File tempTarFile = new File(TEMP_PATH + simpleFileName);
                ossClient.getObject(new GetObjectRequest(bucketName, key), tempTarFile);
                TarUtils.unCompressTar(tempTarFile.getAbsolutePath(), IDEA_PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
            File temp = new File(TEMP_PATH);
            for (File file : temp.listFiles()) {
                file.delete();
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

