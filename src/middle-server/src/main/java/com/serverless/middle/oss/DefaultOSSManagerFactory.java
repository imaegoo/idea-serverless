package com.serverless.middle.oss;

public class DefaultOSSManagerFactory implements OSSManagerFactory {

    @Override
    public OSSManager createOSSManager() {
        return new OSSManager.Singleton().getInstance();
    }
}
