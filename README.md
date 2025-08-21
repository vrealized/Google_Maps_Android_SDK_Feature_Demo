# 谷歌地图 Android SDK 全功能演示 (Google Maps Android SDK All-in-One Demo)

这是一个基于 Google 官方 `android-samples` 项目深度修改和扩展而来的 Android 应用。它不再是一个简单的示例，而是一个功能丰富的演示平台，集中展示了 Google Maps SDK for Android 的多项核心及高级功能，并创新性地集成了前沿的 AI 技术。

本项目是您学习和实践现代 Android 地图应用开发的绝佳起点。

## ✨ 核心功能 (Core Features)

本项目精心实现了 Google 地图开发中的六大核心功能模块：

---

### 🚗 **出行路线规划 (Directions & Route Planning)**
- **功能**: 用户可以输入起点和终点，应用将调用 Directions API 规划出最佳驾车路线。
- **实现**: 在地图上精确绘制出路线的 Polyline，并在界面上显示预计的行程时间和距离。

---

### 🔥 **热力图 (Heatmap)**
- **功能**: 实现数据点的密度可视化，直观展示地理区域内事件的集中程度。
- **实现**: 在地图上渲染出一个平滑的颜色渐变图层，用颜色深浅（如从绿到红）来表示数据点的密集程度。

---

### 📍 **标记点聚合 (Marker Clustering)**
- **功能**: 高效展示海量（100+）的标记点而不会造成界面混乱。
- **实现**: 使用 `android-maps-utils` 库。当地图缩小时，密集的标记点会自动聚合成带有计数的聚合簇；放大时则自动散开，始终保持界面清晰。

---

### 🌍 **逆地理编码 (Reverse Geocoding)**
- **功能**: 强大的交互式地址查询功能。
- **实现**: 用户点击地图上的任意位置，应用会立即将该点的地理坐标（经纬度）通过 `Geocoder` API 转换为人类可读的详细街道地址，并显示在标记点的信息窗口中。

---

### 🚶‍♂️ **街景集成 (Street View Integration)**
- **功能**: 提供沉浸式的 360 度实景体验。
- **实现**: 用户点击地图任意位置即可启动一个专门的全屏街景 (`StreetViewActivity`) 界面。同时，主地图上也已启用可拖拽的街景“小黄人”(Pegman)图标。

---

### ⚙️ **地图 UI 控件 (Map UI Controls)**
- **功能**: 全面启用标准的地图内置控件以提升用户体验。
- **实现**:
    - **静态启用**: 在 XML 布局文件中直接启用了**缩放按钮 (+/-)**。
    - **动态启用**: 在代码中启用了**指南针**、**地图工具栏**和**街景“小黄人”图标**。
    - **自定义交互**: 通过悬浮按钮（FloatingActionButton）和 Material Design 对话框，实现了现代化的**图层切换**功能（普通、卫星、混合等）。

---

### 🤖 **AI 事实核查器 (AI Fact-Checker with Gemini)**
- **功能**: 一项前沿的 AI 集成演示，用于判断信息的真实性。
- **实现**: 集成 Google 最新的 Gemini API。用户可以输入一个问题或一段网络流传的言论（例如：“在野外把竹子砍开真的能喝到水吗？”），应用会调用 Gemini 大语言模型进行分析和判断，返回事实核查结果。


## 截图 (Screenshots)
<img width="594" height="1358" alt="image" src="https://github.com/user-attachments/assets/4ce60edb-43e7-4012-bdaf-e9e17a52c476" />
<img width="588" height="1349" alt="image" src="https://github.com/user-attachments/assets/0fdddf65-c75f-4e32-9758-6cf2d71b4351" />
<img width="602" height="1347" alt="image" src="https://github.com/user-attachments/assets/07664722-1115-4517-a78a-f634b5f5630e" />
<img width="594" height="1358" alt="image" src="https://github.com/user-attachments/assets/fe300dac-4e53-4159-9016-e7c79ae91dcd" />
<img width="603" height="1355" alt="image" src="https://github.com/user-attachments/assets/fd02ea34-5629-4587-88dc-2bc340cdd3dc" />
<img width="591" height="1353" alt="image" src="https://github.com/user-attachments/assets/c82de818-3d26-4a07-a3f7-1c4268dd409a" />
<img width="612" height="1368" alt="image" src="https://github.com/user-attachments/assets/255014e2-954a-41d1-8baf-ac23ea63dd14" />


## 🚀 如何构建与运行 (How to Build & Run)

### 1. 获取并配置 API 密钥 (重要！)

本项目需要有效的 Google API 密钥才能正常工作。

- **获取密钥**: 请访问 [Google Cloud Console](https://console.cloud.google.com/google/maps-apis/overview) 创建项目，并确保启用了以下两个 API：
    1.  **Maps SDK for Android** (用于地图和街景)
    2.  **Generative Language API** (用于 Gemini)
- **配置项目**:
    1. 在您项目的根目录下，找到（或创建）一个名为 `local.properties` 的文件。
    2. 在 `local.properties` 文件中，添加以下内容，并将 `YOUR_..._KEY` 替换为您自己的密钥：
       ```properties
       MAPS_API_KEY=YOUR_MAPS_API_KEY
       GEMINI_API_KEY=YOUR_GEMINI_API_KEY
       ```
    3. **注意**: `local.properties` 文件已被添加到 `.gitignore` 中，以防止您的密钥被意外上传到 GitHub。

### 2. 构建项目
1.  使用 Git 克隆本仓库。
2.  在 Android Studio 中选择 "Open an existing project"。
3.  等待 Gradle 完成项目的同步和构建。
4.  连接您的 Android 设备或启动一个模拟器。
5.  点击 "Run" (▶) 按钮。

## 许可证 (License)

本项目基于 Apache 2.0 许可证。详情请参阅 `LICENSE` 文件。

```
Copyright 2025 Your Name or Company

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
