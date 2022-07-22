#!/bin/sh
java -Xmx256m -Xmx256m -Xmn128m -Xss512k -Dio.netty.leakDetectionLevel=advanced -Dprojector.start.shell=/home/mae/workspace/idea-serverless/src/projector-local.sh -jar middle-server/target/middle-server.jar
