# 星之棋 · Star Chess

單機星象主題棋類策略遊戲。

## 網頁版

- `src/`（`index.html`、`css/style.css`、`js/01-game.js`）：多檔網頁原始碼
- `index.html`：打包後單檔成品（GitHub Pages 線上版）
- `starchess-v6.22.0-standalone.html`：單檔離線成品，瀏覽器打開即玩

## Android 版

`android/` 目錄為原生 Android 專案（Jetpack Compose、AGP 9.1.1、minSdk 24 / targetSdk 36）。以 Android Studio 開啟 `android/` 目錄同步後建置；`android/app/src/main/assets/starchess.html` 為離線回退副本，隨網頁成品一併更新。
