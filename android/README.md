# 网络安全日报速递 — Android 小组件

基于 Android 原生 AppWidget + RemoteViews 的桌面小组件，自动从 GitHub Pages 抓取最新日报内容，
在桌面展示当日安全要闻。

## 功能

- 桌面小组件显示最新一期日报的**关键词**和**头条摘要**
- 中午12点每隔15分钟自动刷新直到检测到最新的推文更新为止
- 点击小组件直接跳转浏览器阅读完整日报
- 支持横向/纵向拉伸，自适应宽窄布局

## 技术栈

| 层 | 技术 |
|---|---|
| Widget UI | Android AppWidget + RemoteViews |
| 后台刷新 | WorkManager 2.9.0 |
| HTML 解析 | Jsoup 1.17.2 |
| 网络请求 | OkHttp 4.12.0 |
| UI (App) | Jetpack Compose + Material3 |
| 最低 SDK | Android 8.0 (API 26) |

## 构建

### 前提条件

- Android Studio Hedgehog (2023.1) 或更新版本
- JDK 17
- Android SDK 34

### 步骤

```bash
# 1. 进入 android 目录
cd android

# 2. (首次) 生成 Gradle Wrapper
gradle wrapper --gradle-version 8.5

# 3. 构建 APK
./gradlew assembleRelease

# 或直接在 Android Studio 中打开 android/ 目录
```

构建完成后，APK 位于 `app/build/outputs/apk/release/`。

## 安装与使用

1. 将 APK 安装到 Android 手机
2. 长按桌面空白区域 → 小组件
3. 搜索「网络安全日报」
4. 拖拽到桌面
5. 首次添加后会自动拉取最新数据
6. 点击小组件即可在浏览器中阅读完整日报

## 项目结构

```
android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/cybersecdaily/widget/
│       │   ├── CyberSecDailyWidgetReceiver.kt  # AppWidgetProvider + RemoteViews
│       │   ├── DailyReport.kt                  # 数据模型
│       │   ├── ReportFetcher.kt                # HTML 抓取 & 解析
│       │   ├── WidgetUpdateWorker.kt           # WorkManager Worker
│       │   └── MainActivity.kt                 # App 主界面
│       ├── res/
│       │   ├── drawable/    (图标、Widget 背景)
│       │   ├── layout/      (Widget 初始布局)
│       │   ├── mipmap-*/    (启动器图标)
│       │   ├── values/      (字符串、颜色、主题)
│       │   └── xml/         (Widget 配置)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## Widget 数据流

```
┌─────────────┐   HTTP GET    ┌─────────────────────────┐
│  Android     │ ────────────→ │ GitHub Pages             │
│  Widget      │ ←──────────── │ /index.html (找最新日期)  │
│  Worker      │               │ /daily/YYYY-MM-DD.html   │
└─────────────┘               └─────────────────────────┘
       │
       ▼
┌─────────────────┐
│ Jsoup 解析 HTML  │
│ → keywords      │
│ → headlines     │
│ → quick news    │
└─────────────────┘
       │
       ▼
┌─────────────────┐
│ RemoteViews      │
│ 暗色报纸风格 UI  │
└─────────────────┘
```
