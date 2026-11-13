# DeepSeek KaTeX（Android 版）

Windows 版 DeepSeekKaTeX.exe 的 Android 移植：用 WebView 打开 chat.deepseek.com，
并注入 KaTeX + renderer.js，让对话里的 LaTeX 公式和 CSS 代码块正确渲染。
`app/src/main/assets/` 里的三个文件和 exe 里的完全一致，一行没改。

## 方法一：Android Studio（本机构建）
1. 安装 Android Studio，File → Open 选择本文件夹，等 Gradle 同步完成
2. 菜单 Build → Build Bundle(s) / APK(s) → Build APK(s)
3. 弹出提示后点 locate，apk 在 `app/build/outputs/apk/debug/app-debug.apk`

## 方法二：GitHub Actions（本机什么都不用装）
1. 新建一个 GitHub 仓库，把本文件夹里的所有内容（包括隐藏的 `.github`）推上去
2. 打开仓库的 Actions 页面，等 "Build APK" 跑完（约 3~5 分钟），也可以手动点 Run workflow
3. 在这次运行页面底部 Artifacts 下载 `DeepSeekKaTeX-apk`，解压得到 `app-debug.apk`

## 方法三：命令行
已安装 JDK 17、Android SDK 和 Gradle 8.7 的话：`gradle assembleDebug`

## 安装
把 apk 传到手机，允许"安装未知来源应用"后点开安装即可。这是 debug 签名，自己用没问题。

## 结构
- `MainActivity.kt`：全部逻辑（对应 main.py：加载页面、延迟注入、5 秒 watchdog、Cookie 持久化、文件上传、返回键）
- `assets/`：katex.min.js、katex.selfcontained.css、renderer.js
