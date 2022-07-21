#!/bin/sh
java -jar -Xmx256m -Xmx256m -Xmn128m -Xss512k -Dio.netty.leakDetectionLevel=advanced middle-server/target/middle-server.jar
