# 看什么 (KanShenMe)

> 一个围绕 [kanshenme.uk](https://kanshenme.uk) 构建的影视浏览应用，支持 Web 预览和原生 Android APK 两种形式。

---

## 项目结构

```
├── src/              # Web 端 React 演示应用（手机外框模拟）
├── android-app/      # 原生 Android Kotlin 源码（WebView 封装）
├── .github/          # GitHub Actions 自动构建 APK
└── public/           # Web 应用公共资源
```

## 下载 APK

每次推送到 `main` 分支，GitHub Actions 会自动构建 APK：

1. 进入本仓库的 [Actions](../../actions) 页面
2. 点击最新的 **Build Android APK** 构建任务
3. 在 **Artifacts** 区域下载 `Kanshenme-APK.zip`
4. 解压后安装 `.apk` 文件（需开启「允许未知来源」）

## 本地运行 Web 预览

**前置条件：** Node.js

```bash
npm install
npm run dev
```

## 本地构建 Android APK

**前置条件：** JDK 17、Android SDK

```bash
cd android-app
gradle assembleDebug
```

## 技术栈

| 端 | 技术 |
|---|---|
| Web | React 19 + Vite 6 + TypeScript + Tailwind CSS 4 |
| Android | Kotlin + AndroidX WebKit + ViewBinding |
| CI/CD | GitHub Actions |
