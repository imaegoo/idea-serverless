#!/bin/sh
set -x
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
tar -xzvf ideaIC-2021.3.2.tar.gz
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
echo "publishing layers"
s cli fc layer publish --layer-name ide-jbr --code ./ide-jbr --region cn-hangzhou
s cli fc layer publish --layer-name ide-lib --code ./ide-lib --region cn-hangzhou
s cli fc layer publish --layer-name ide-plugins --code ./ide-plugins --region cn-hangzhou
