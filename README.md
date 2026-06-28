# Aijia Video Application

A modern Kotlin video playback application built with the latest Android technology stack.

## 🚀 Tech Stack

- **Language**: Kotlin 100%
- **Architecture**: MVVM + Clean Architecture
- **UI Framework**: Jetpack Compose + Material Design 3
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp + Coroutines
- **Database**: Room + SQLite
- **Image Loading**: Coil
- **Video Playback**: AndroidX Media3 (ExoPlayer)
- **Navigation**: Navigation Compose

## 📱 Features

### Core Features

- ✅ Video playback and controls
- ✅ Video search and category browsing
- ✅ User system (login/register)
- ✅ Play history
- ✅ Video favorites
- ✅ Comments and bullet chat system
- ✅ Live streaming
- ✅ Video download (offline viewing)

### Technical Features

- ✅ Modern UI design
- ✅ Responsive layout
- ✅ Dark theme support
- ✅ Network state caching
- ✅ Local data persistence
- ✅ Dependency injection architecture
- ✅ Coroutine async processing

## 🏗️ Project Structure

```text
app/src/main/java/com/aijia/video/
├── data/                          # Data Layer
│   ├── local/                     # Local Data Source
│   │   ├── dao/                   # Data Access Objects
│   │   ├── converters/            # Type Converters
│   │   └── AppDatabase.kt         # Database Config
│   ├── model/                     # Data Models
│   ├── remote/                    # Remote Data Source
│   │   └── ApiService.kt          # API Interface
│   └── repository/                # Data Repository
├── di/                            # Dependency Injection Modules
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── ui/                            # UI Layer
│   ├── components/                # Reusable Components
│   ├── navigation/                # Navigation Config
│   ├── screens/                   # Screens
│   │   ├── home/                  # Home
│   │   ├── player/                # Player
│   │   ├── search/                # Search
│   │   └── profile/               # Profile
│   └── theme/                     # Theme Styles
├── VideoApplication.kt            # Application Class
└── MainActivity.kt                # Main Activity
```

## 🛠️ Development Environment

- **Android Studio**: Giraffe (2022.3.1) or higher
- **JDK**: 11 or higher
- **Gradle**: 8.5
- **Kotlin**: 2.0.21
- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 34)

## 🚀 Quick Start

1. **Clone the project**

```bash
git clone <repository-url>
cd aijia
```

2. **Configure environment**

- Ensure Android Studio and JDK 11+ are installed
- Configure Android SDK

3. **Build the project**

```bash
./gradlew build
```

4. **Run the app**

```bash
./gradlew installDebug
```

## 📦 Dependency Versions

Key dependency versions (see `gradle/libs.versions.toml`):

- **Compose**: 2024.09.00
- **Navigation**: 2.8.0
- **Hilt**: 2.48
- **Retrofit**: 2.9.0
- **OkHttp**: 4.12.0
- **Room**: 2.6.1
- **Media3**: 1.2.0
- **Coil**: 2.6.0

## 🔧 Configuration

### Network Configuration

- **Base URL**: `http://192.168.31.999:9090`
- HTTP plaintext supported (for development testing)
- HTTPS recommended for production

### Database

- **Database Name**: `aijia_video_database`
- **Version**: 1
- Uses Room Database

### Permissions

- Network access
- Storage access
- Wake lock permission

## 🎨 UI Design

- Follows Material Design 3 guidelines
- Dynamic color theme support
- Responsive layout for different screens
- Dark theme support

## 📈 Performance Optimization

- Compose for rendering performance
- Coroutine async processing to avoid blocking
- Room database query optimization
- Coil image caching mechanism
- Network request caching strategy

## 🔒 Security Considerations

- HTTPS communication (production)
- Encrypted storage for sensitive data
- Code obfuscation protection
- Principle of least privilege for permissions

## 🧪 Testing

Complete test structure includes:

- Unit tests (Repository, ViewModel)
- UI tests (Compose components)
- Integration tests (API interfaces)

## 📝 TODO

- [√] Integrated Media3 video player
- [√] Implemented bullet chat system
- [√] Added video download feature
- [√] Completed user system
- [√] Implemented screen casting
- [√] Added performance monitoring
- [√] Improved error handling
- [√] Added unit tests

## 📷 Demo Screenshots

![Screenshot 1](https://wmimg.com/i/1502/2026/05/6a1aa5a44192d.png)
![Screenshot 2](https://wmimg.com/i/1502/2026/05/6a1aa5a044b88.png)
![Screenshot 3](https://wmimg.com/i/1502/2026/05/6a1aa5a057047.png)
![Screenshot 4](https://wmimg.com/i/1502/2026/05/6a1aa5a3948a7.png)
![Screenshot 5](https://wmimg.com/i/1502/2026/05/6a1aa59fd3bb3.png)
![Screenshot 6](https://wmimg.com/i/1502/2026/05/6a1aa5a03fea0.png)
![Screenshot 7](https://wmimg.com/i/1502/2026/05/6a1ae033e1afd.png)

## 🤝 Contributing

Issues and Pull Requests are welcome to improve the project.

## 📄 License

This project is open-source under the MIT License.

## 🤝 Open Source Repository

https://github.com/jinian123H/aijia

**Project Status**: ✅ Basic architecture complete, ready to run

---

## 📖 Installation Guide

### Backend

1. Open port 9090
2. Create `.env` file with the following configuration:

```env
# Encryption Configuration
AES_PASSPHRASE=AijiaAES2026SecurePassphrase!@#
ENABLE_ENCRYPTION=true
PLAY_SIGN_SECRET=kumiao_play_sign_2026
PLAY_SIGN_EXPIRE=600
```

![Backend Config](https://wmimg.com/i/1502/2026/05/6a1ae02af0de4.png)

### Frontend

Configure `aijia/app/src/main/assets/api_config.json`:

```json
{
  "url": "http://112.124.36.198:9090",
  "aes_passphrase": "AijiaAES2026SecurePassphrase!@#",
  "enable_encryption": true
}
```

## � Download

Download: https://www.suiyuanlu.cn/index.php/archives/40/

---

## 中文版 (Chinese Version)

# Aijia 视频应用

现代化 Kotlin 视频播放应用，采用最新 Android 技术栈。

## 🚀 技术栈

- **开发语言**: Kotlin 100%
- **架构模式**: MVVM + Clean Architecture
- **UI 框架**: Jetpack Compose + Material Design 3
- **依赖注入**: Hilt
- **网络请求**: Retrofit + OkHttp + Coroutines
- **数据库**: Room + SQLite
- **图片加载**: Coil
- **视频播放**: AndroidX Media3 (ExoPlayer)
- **导航**: Navigation Compose

## 📱 功能特性

### 核心功能

- ✅ 视频播放与控制
- ✅ 视频搜索与分类浏览
- ✅ 用户系统（登录/注册）
- ✅ 播放历史记录
- ✅ 视频收藏功能
- ✅ 评论与弹幕系统
- ✅ 直播功能
- ✅ 视频下载（离线观看）

### 技术特性

- ✅ 现代化 UI 设计
- ✅ 响应式布局
- ✅ 暗色主题支持
- ✅ 网络状态缓存
- ✅ 本地数据持久化
- ✅ 依赖注入架构
- ✅ 协程异步处理

## 🏗️ 项目结构

```text
app/src/main/java/com/aijia/video/
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── dao/                   # 数据访问对象
│   │   ├── converters/            # 类型转换器
│   │   └── AppDatabase.kt         # 数据库配置
│   ├── model/                     # 数据模型
│   ├── remote/                    # 远程数据源
│   │   └── ApiService.kt          # API 接口
│   └── repository/                # 数据仓库
├── di/                            # 依赖注入模块
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── ui/                            # UI 层
│   ├── components/                # 可复用组件
│   ├── navigation/                # 导航配置
│   ├── screens/                   # 页面 Screen
│   │   ├── home/                  # 首页
│   │   ├── player/                # 播放页
│   │   ├── search/                # 搜索页
│   │   └── profile/               # 个人中心
│   └── theme/                     # 主题样式
├── VideoApplication.kt            # Application 类
└── MainActivity.kt                # 主 Activity
```

## 🛠️ 开发环境

- **Android Studio**: Giraffe (2022.3.1) 或更高版本
- **JDK**: 11 或更高版本
- **Gradle**: 8.5
- **Kotlin**: 2.0.21
- **最低 Android 版本**: Android 7.0 (API 24)
- **目标 Android 版本**: Android 14 (API 34)

## 🚀 快速开始

1. **克隆项目**

```bash
git clone <repository-url>
cd aijia
```

2. **配置环境**

- 确保已安装 Android Studio 和 JDK 11+
- 配置 Android SDK

3. **构建项目**

```bash
./gradlew build
```

4. **运行应用**

```bash
./gradlew installDebug
```

## 📦 依赖库版本

主要依赖库版本（详见 `gradle/libs.versions.toml`）：

- **Compose**: 2024.09.00
- **Navigation**: 2.8.0
- **Hilt**: 2.48
- **Retrofit**: 2.9.0
- **OkHttp**: 4.12.0
- **Room**: 2.6.1
- **Media3**: 1.2.0
- **Coil**: 2.6.0

## 🔧 配置说明

### 网络配置

- **基础 URL**: `http://192.168.31.999:9090`
- 支持 HTTP 明文传输（用于开发测试）
- 生产环境建议使用 HTTPS

### 数据库

- **数据库名称**: `aijia_video_database`
- **版本**: 1
- 使用 Room 数据库

### 权限配置

- 网络访问权限
- 存储访问权限
- 设备唤醒锁权限

## 🎨 UI 设计

- 遵循 Material Design 3 设计规范
- 支持动态颜色主题
- 响应式布局适配不同屏幕
- 暗色主题支持

## 📈 性能优化

- 使用 Compose 提升渲染性能
- 协程异步处理避免阻塞
- Room 数据库优化查询
- Coil 图片缓存机制
- 网络请求缓存策略

## 🔒 安全考虑

- HTTPS 通信（生产环境）
- 敏感数据加密存储
- 代码混淆保护
- 权限最小化原则

## 🧪 测试

项目包含完整的测试结构：

- 单元测试（Repository、ViewModel）
- UI 测试（Compose 组件）
- 集成测试（API 接口）

## 📝 TODO

- [√] 集成 Media3 视频播放器
- [√] 实现弹幕系统
- [√] 添加视频下载功能
- [√] 完善用户系统
- [√] 实现投屏功能
- [√] 添加性能监控
- [√] 完善错误处理
- [√] 添加单元测试

## 📷 演示图片

![截图 1](https://wmimg.com/i/1502/2026/05/6a1aa5a44192d.png)
![截图 2](https://wmimg.com/i/1502/2026/05/6a1aa5a044b88.png)
![截图 3](https://wmimg.com/i/1502/2026/05/6a1aa5a057047.png)
![截图 4](https://wmimg.com/i/1502/2026/05/6a1aa5a3948a7.png)
![截图 5](https://wmimg.com/i/1502/2026/05/6a1aa59fd3bb3.png)
![截图 6](https://wmimg.com/i/1502/2026/05/6a1aa5a03fea0.png)
![截图 7](https://wmimg.com/i/1502/2026/05/6a1ae033e1afd.png)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来改进项目。

## 📄 许可证

本项目基于 MIT 许可证开源。

## 🤝 开源地址

https://github.com/jinian123H/aijia

**项目状态**: ✅ 基础架构完成，可运行

---

## 📖 安装教程

### 后端

1. 开放 9090 端口
2. 创建 `.env` 文件，配置如下：

```env
# 加密配置
AES_PASSPHRASE=AijiaAES2026SecurePassphrase!@#
ENABLE_ENCRYPTION=true
PLAY_SIGN_SECRET=kumiao_play_sign_2026
PLAY_SIGN_EXPIRE=600
```

![后端配置](https://wmimg.com/i/1502/2026/05/6a1ae02af0de4.png)

### 前端

配置 `aijia/app/src/main/assets/api_config.json`：

```json
{
  "url": "http://112.124.36.198:9090",
  "aes_passphrase": "AijiaAES2026SecurePassphrase!@#",
  "enable_encryption": true
}
```

## 📥 下载地址

下载: https://www.suiyuanlu.cn/index.php/archives/40/
