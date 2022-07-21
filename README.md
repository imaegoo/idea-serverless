# IntelliJ IDEA on web, but serverless

[![Package type](https://editor.devsapp.cn/icon?package=idea-webide&type=packageType)](https://www.serverless-devs.com)
[![Package version](https://editor.devsapp.cn/icon?package=idea-webide&type=packageVersion)](https://www.devsapp.cn/details.html?name=idea-webide)
[![Package download](https://editor.devsapp.cn/icon?package=idea-webide&type=packageDownload)](https://www.devsapp.cn/details.html?name=idea-webide)

## 应用背景

在云的时代，我们已经能够轻松地在阿里云函数计算上，部署一个在线版 VSCode。然而 VSCode 的后端开发体验仍然不如 IntelliJ IDE。虽然市面上已经出现了 [Eclipse Che](https://www.eclipse.org/che/)、[云效云端开发平台 DevStudio](https://www.aliyun.com/product/yunxiao/devstudio) 等支持 IntelliJ 的在线开发平台，但是部署复杂、不够轻量。由此引发思考：能不能把 IntelliJ IDE 也搬上 Serverless？享受 Serverless 即搭即用、按量付费的特性呢？

## 技术架构实现、原理以及亮点

我是如何使用阿里云的一个或者多个 Serverless 服务实现 Web IDE 服务？

1. 通过 [projector-server](https://github.com/JetBrains/projector-server) 将 IDE 封装为远程服务。
1. IDE 解压后有近 2 GB 的大小，可将 IDE 的依赖（`jbr`、`lib`、`plugins`）拆到各自的函数层（`layer`）中，以降低单次部署需要上传的包大小。
1. 编写 Java 程序实现配置同步、代码同步，同步源可选阿里云 OSS。

相比官方给的默认示例，我有什么亮点？

1. 不重复造轮子！截至 2022 年 7 月 14 日，[Google 搜索](https://www.google.com/search?q=deploy+intellij+to+serverless) 中还未出现成功将 Intellij IDE 部署到 Serverless 函数计算的实践案例。
1. 默认示例使用的 VSCode 难以胜任 Java 等语言开发场景。
1. 虚拟机级别的多租安全隔离。
1. 配置同步、代码同步。
1. 集成 CLI 等常用 FC 开发工具，支持快速开发和测试 FC Java runtime 函数。

## 使用说明

您可以体验 [demo 站点](http://idea-fc.idea-service.1064348262863466.cn-hangzhou.fc.devsapp.net/)，也可以一键部署到自己的阿里云账号。

### 一键部署

[![Deploy with Severless Devs](https://img.alicdn.com/imgextra/i1/O1CN01w5RFbX1v45s8TIXPz_!!6000000006118-55-tps-95-28.svg)](https://fcnext.console.aliyun.com/applications/create?template=idea-webide)

### 通过 [Cli](https://www.serverless-devs.com/serverless-devs/install) 部署

**_TODO: 此节待完善，请不要尝试通过下面的命令部署_**

需 Linux 环境，需安装 Node.js。

```sh
cd src/
./build.sh
npm i -g @serverless-devs/s
s config add
s deploy
```

**_TODO: 为了防止因无法使用影响评测，下面我提供了视频演示。_**

## 实现说明

1. 该应用部署在阿里云帐号 1064348262863466 下，杭州区域。
1. 该应用主要使用了阿里云如下服务：
    * 函数计算服务，服务名包括 `idea-service`。
    * OSS服务，bucket名称是 `imaegoo`。
1. 该应用的实现代码在当前项目下。

## 参考资料

### 小白学习路径

1. [函数计算官网](https://help.aliyun.com/document_detail/52895.html) 
1. [使用 Serverless-Devs 工具快速开发 FC cookbook](https://docs.serverless-devs.com/fc-faq/s_fc_cookbook/readme)

### 官方参考 DEMO

[https://github.com/devsapp/start-serverless-webide](https://github.com/devsapp/start-serverless-webide)
