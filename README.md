# Cloudflared Helper Android

一个使用 **纯 Java + Material You (Material 3)** 编写的 Android 客户端，通过
`cloudflared access tcp` 将 Cloudflare Tunnel 暴露的远端 TCP 服务映射到本机端口。

## 功能

- **多隧道支持**：添加任意数量的隧道（远端主机名 → `127.0.0.1:本地端口`），逐个启停、查看日志。主机名必须是域名（cloudflared 不支持 IP 地址）。
- **cloudflared 版本管理**
  - 首次启动自动下载最新版 cloudflared 到应用私有目录（显示进度 / 速度）。
  - 每次启动静默检查 GitHub Releases，有新版本时弹窗提示（可稍后 / 忽略此版本）。
  - 版本管理页可下载任意历史版本、切换当前使用版本、删除本地版本。
- **前台服务保活**：隧道进程由前台服务托管，常驻通知 + 部分唤醒锁，支持一键全部停止。
- **Material You**：Android 12+ 自动跟随系统动态取色，支持深色模式。
- **自适应矢量图标**（含 Android 13 单色主题图标）。

## 重要说明：targetSdkVersion = 28

Android 10 及以上对 `targetSdk >= 29` 的应用施加了 W^X 限制，禁止执行应用数据目录中的
可执行文件（`cloudflared` 二进制）。因此本项目 **必须保持 `targetSdk 28`**，请勿升级。

## 构建

```bash
gradle assembleDebug      # 或使用 Android Studio 打开项目
```

GitHub Actions（`.github/workflows/build.yml`）会在 push 到 `main` 或手动触发时构建 Release APK，
上传为 Artifact，并发布（覆盖）名为 `nightly` 的预发布版本。

如需使用自己的密钥签名，在仓库 Secrets 中配置：
`KEYSTORE_BASE64`（keystore 文件 base64）、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。
未配置时 Release APK 会使用 debug 密钥签名，以保证可以直接安装。
CI 会缓存该 debug 密钥以保持各次 nightly 签名一致（缓存被清理后签名会变化，需先卸载旧版）。

## 目录结构

```
app/src/main/java/org/lyi/cha/
  App.java                 Application，启用动态取色
  MainActivity.java        隧道列表 / 添加编辑 / 首次下载 / 更新检查
  VersionActivity.java     cloudflared 版本管理
  TunnelService.java       前台服务，托管 cloudflared 子进程
  CloudflaredManager.java  下载、版本存储、GitHub API
  DownloadDialog.java      带进度和速度的下载对话框
  Tunnel.java / TunnelStore.java   隧道模型与持久化
  TunnelAdapter.java / VersionAdapter.java
```

## 🗂️ License

This program is released under the GNU General Public License v3.0 (GPLv3).

Copyright (C) 2026 lingyicute.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses.
