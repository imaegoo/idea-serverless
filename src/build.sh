#!/bin/sh
echo "building middle server"
cd middle-server
mvn package
cd ..
echo "installing serverless cli"
cd js
npm install
cd ..
echo "installing maven"
mkdir -p maven
cd maven
curl -LO https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz
tar -xzf apache-maven-3.8.6-bin.tar.gz
rm -f apache-maven-3.8.6-bin.tar.gz
cd ..
echo "creating temp folder"
rm -rf ./.temp/
mkdir -p .temp
cd .temp
echo "downloading projector-server"
curl -LO https://github.com/JetBrains/projector-server/releases/download/v1.8.1/projector-server-v1.8.1.zip
echo "downloading idea"
curl -LO https://download.jetbrains.com/idea/ideaIC-2021.3.2.tar.gz
echo "unpacking projector-server"
unzip projector-server-v1.8.1.zip
echo "unpacking idea"
tar -xzf ideaIC-2021.3.2.tar.gz
# Android 插件体积太大，并且绝大多数开发场景用不到，移除之
rm -rf ./idea-IC-213.6777.52/plugins/android/
echo "removing existing files"
rm -rf ../ide-jbr/
rm -rf ../ide-lib/
rm -rf ../ide-plugins/
rm -rf ../ide-server/
echo "moving files"
mv ./idea-IC-213.6777.52 ../ide-server
mv ./projector-server-1.8.1 ../ide-server/projector-server
cp ../idea.sh ../ide-server/bin/
mkdir -p ../ide-jbr
mkdir -p ../ide-lib
mkdir -p ../ide-plugins
mv ../ide-server/jbr ../ide-jbr/jbr
mv ../ide-server/lib ../ide-lib/lib
mv ../ide-server/plugins ../ide-plugins
echo "deleting temp folder"
cd ..
rm -rf .temp
