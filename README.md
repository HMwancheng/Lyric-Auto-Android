# Lyric Auto - Android悬浮窗歌词APP

一款Android端高版本可用的悬浮窗歌词APP，能读取系统正在播放的歌曲，并自动从网上下载、本地缓存获取歌词并按当前播放进度显示歌词。

## 功能特性

- 🎵 自动监听系统音乐播放
- 📝 自动从网易云音乐下载歌词
- 💾 本地歌词缓存
- 🎨 可自定义悬浮窗样式、动画、颜色、位置
- 📍 支持虚拟按键微调位置
- 📂 支持本地音乐扫描和歌词下载

## 修复内容 (v1.1.0)

### COLOROS16 / Android 14+ 闪退问题修复

1. **AndroidManifest.xml 权限更新**
   - 添加了 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 权限
   - 添加了 `FOREGROUND_SERVICE_DATA_SYNC` 权限
   - 修复了存储权限的SDK版本限制

2. **前台服务适配 Android 12+**
   - 所有服务现在正确声明 `foregroundServiceType`
   - 服务启动时使用 `ContextCompat.startForegroundService()`
   - 歌词下载服务添加了前台通知

3. **BroadcastReceiver 适配 Android 13+**
   - 添加了 `RECEIVER_NOT_EXPORTED` 标志
   - 修复了Receiver注册的安全问题

4. **代码优化**
   - 修复了歌词更新协程的内存泄漏问题
   - 优化了悬浮窗触摸事件处理
   - 添加了更多的异常处理
   - 更新了Java版本到17

## 构建要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

## 安装说明

1. 下载最新版本的APK
2. 安装前请确保已开启「允许安装未知来源应用」
3. 安装后需要授予以下权限：
   - 悬浮窗权限（必须）
   - 通知权限（Android 13+）
   - 存储权限（用于扫描本地音乐）
   - 通知监听权限（用于获取音乐信息）

## 使用方法

1. 打开应用，授予必要权限
2. 点击「开启悬浮窗」按钮
3. 播放音乐，歌词会自动显示在悬浮窗中
4. 点击悬浮窗可以显示/隐藏控制按钮
5. 使用控制按钮可以微调悬浮窗位置

## 技术栈

- Kotlin
- Android SDK 34
- MVVM架构
- Kotlin Coroutines
- OkHttp3
- Gson
- Material Design Components

## 开源协议

MIT License

## 致谢

- 歌词数据来源：网易云音乐
