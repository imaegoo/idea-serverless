package com.serverless.middle.oss;

public interface OSSManagerFactory {

    String OSS_BUCKET_KEY = "OSS_BUCKET_NAME";

    OSSManager createOSSManager();

}
