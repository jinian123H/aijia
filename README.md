Aijia Video Application
A modern Kotlin video playback application built with the latest Android technology stack.

🚀 Tech Stack
Language: Kotlin 100%

Architecture: MVVM + Clean Architecture

UI Framework: Jetpack Compose + Material Design 3

Dependency Injection: Hilt

Networking: Retrofit + OkHttp + Coroutines

Database: Room + SQLite

Image Loading: Coil

Video Playback: AndroidX Media3 (ExoPlayer)

Navigation: Navigation Compose

📱 Features
Core Features
✅ Video playback and controls

✅ Video search and category browsing

✅ User system (login/register)

✅ Play history

✅ Video favorites

✅ Comments and bullet chat system

✅ Live streaming

✅ Video download (offline viewing)

Technical Features
✅ Modern UI design

✅ Responsive layout

✅ Dark theme support

✅ Network state caching

✅ Local data persistence

✅ Dependency injection architecture

✅ Coroutine async processing

🏗️ Project Structure
text
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
├── di/                           # Dependency Injection Modules
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── ui/                           # UI Layer
│   ├── components/                # Reusable Components
│   ├── navigation/                # Navigation Config
│   ├── screens/                   # Screens
│   │   ├── home/                 # Home
│   │   ├── player/               # Player
│   │   ├── search/               # Search
│   │   └── profile/              # Profile
│   └── theme/                    # Theme Styles
├── VideoApplication.kt            # Application Class
└── MainActivity.kt               # Main Activity
🛠️ Development Environment
Android Studio: Giraffe (2022.3.1) or higher

JDK: 11 or higher

Gradle: 8.5

Kotlin: 2.0.21

Minimum Android Version: Android 7.0 (API 24)

Target Android Version: Android 14 (API 34)

🚀 Quick Start
Clone the project

bash
git clone <repository-url>
cd aijia
Configure environment

Ensure Android Studio and JDK 11+ are installed

Configure Android SDK

Build the project

bash
./gradlew build
Run the app

bash
./gradlew installDebug
📦 Dependency Versions
Key dependency versions (see gradle/libs.versions.toml):

Compose: 2024.09.00

Navigation: 2.8.0

Hilt: 2.48

Retrofit: 2.9.0

OkHttp: 4.12.0

Room: 2.6.1

Media3: 1.2.0

Coil: 2.6.0

🔧 Configuration
Network Configuration
Base URL: http://192.168.31.999:9090

HTTP plaintext supported (for development testing)

HTTPS recommended for production

Database
Database Name: aijia_video_database

Version: 1

Uses Room Database

Permissions
Network access

Storage access

Wake lock permission

🎨 UI Design
Follows Material Design 3 guidelines

Dynamic color theme support

Responsive layout for different screens

Dark theme support

📈 Performance Optimization
Compose for rendering performance

Coroutine async processing to avoid blocking

Room database query optimization

Coil image caching mechanism

Network request caching strategy

🔒 Security Considerations
HTTPS communication (production)

Encrypted storage for sensitive data

Code obfuscation protection

Principle of least privilege for permissions

🧪 Testing
Complete test structure includes:

Unit tests (Repository, ViewModel)

UI tests (Compose components)

Integration tests (API interfaces)

📝 TODO
[√] Integrated Media3 video player

[√] Implemented bullet chat system

[√] Added video download feature

[√] Completed user system

[√] Implemented screen casting

[√] Added performance monitoring

[√] Improved error handling

[√] Added unit tests

Demo Screenshots
https://wmimg.com/i/1502/2026/05/6a1aa5a44192d.png
https://wmimg.com/i/1502/2026/05/6a1aa5a044b88.png
https://wmimg.com/i/1502/2026/05/6a1aa5a057047.png
https://wmimg.com/i/1502/2026/05/6a1aa5a3948a7.png
https://wmimg.com/i/1502/2026/05/6a1aa59fd3bb3.png
https://wmimg.com/i/1502/2026/05/6a1aa5a03fea0.png
https://wmimg.com/i/1502/2026/05/6a1ae033e1afd.png

🤝 Contributing
Issues and Pull Requests are welcome to improve the project.

📄 License
This project is open-source under the MIT License.

🤝 Open Source Repository
https://github.com/jinian123H/aijia

Project Status: ✅ Basic architecture complete, ready to run

Installation Guide
Backend
Open port 9090
.env

env
# Encryption Configuration
AES_PASSPHRASE=AijiaAES2026SecurePassphrase!@#        -----App encryption key
ENABLE_ENCRYPTION=true
PLAY_SIGN_SECRET=kumiao_play_sign_2026
PLAY_SIGN_EXPIRE=600
https://wmimg.com/i/1502/2026/05/6a1ae02af0de4.png

Frontend
aijia/app/src/main/assets/api_config.json

json
{
  "url": "http://112.124.36.198:9090",                  -----Backend URL
  "aes_passphrase": "AijiaAES2026SecurePassphrase!@#",  -----App encryption key
  "enable_encryption": true
}
中文版 (Chinese Version)
Aijia 视频应用
现代化Kotlin视频播放应用，采用最新Android技术栈。

🚀 技术栈
开发语言: Kotlin 100%

架构模式: MVVM + Clean Architecture

UI框架: Jetpack Compose + Material Design 3

依赖注入: Hilt

网络请求: Retrofit + OkHttp + Coroutines

数据库: Room + SQLite

图片加载: Coil

视频播放: AndroidX Media3 (ExoPlayer)

导航: Navigation Compose

📱 功能特性
核心功能
✅ 视频播放与控制

✅ 视频搜索与分类浏览

✅ 用户系统（登录/注册）

✅ 播放历史记录

✅ 视频收藏功能

✅ 评论与弹幕系统

✅ 直播功能

✅ 视频下载（离线观看）

技术特性
✅ 现代化UI设计

✅ 响应式布局

✅ 暗色主题支持

✅ 网络状态缓存

✅ 本地数据持久化

✅ 依赖注入架构

✅ 协程异步处理

🏗️ 项目结构
text
app/src/main/java/com/aijia/video/
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── dao/                   # 数据访问对象
│   │   ├── converters/            # 类型转换器
│   │   └── AppDatabase.kt         # 数据库配置
│   ├── model/                     # 数据模型
│   ├── remote/                    # 远程数据源
│   │   └── ApiService.kt          # API接口
│   └── repository/                # 数据仓库
├── di/                           # 依赖注入模块
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── ui/                           # UI层
│   ├── components/                # 可复用组件
│   ├── navigation/                # 导航配置
│   ├── screens/                   # 页面Screen
│   │   ├── home/                 # 首页
│   │   ├── player/               # 播放页
│   │   ├── search/               # 搜索页
│   │   └── profile/              # 个人中心
│   └── theme/                    # 主题样式
├── VideoApplication.kt            # Application类
└── MainActivity.kt               # 主Activity
🛠️ 开发环境
Android Studio: Giraffe (2022.3.1) 或更高版本

JDK: 11 或更高版本

Gradle: 8.5

Kotlin: 2.0.21

最低Android版本: Android 7.0 (API 24)

目标Android版本: Android 14 (API 34)

🚀 快速开始
克隆项目

bash
git clone <repository-url>
cd aijia
配置环境

确保已安装Android Studio和JDK 11+

配置Android SDK

构建项目

bash
./gradlew build
运行应用

bash
./gradlew installDebug
📦 依赖库版本
主要依赖库版本（详见gradle/libs.versions.toml）：

Compose: 2024.09.00

Navigation: 2.8.0

Hilt: 2.48

Retrofit: 2.9.0

OkHttp: 4.12.0

Room: 2.6.1

Media3: 1.2.0

Coil: 2.6.0

🔧 配置说明
网络配置
基础URL: http://192.168.31.999:9090

支持HTTP明文传输（用于开发测试）

生产环境建议使用HTTPS

数据库
数据库名称: aijia_video_database

版本: 1

使用Room数据库

权限配置
网络访问权限

存储访问权限

设备唤醒锁权限

🎨 UI设计
遵循Material Design 3设计规范

支持动态颜色主题

响应式布局适配不同屏幕

暗色主题支持

📈 性能优化
使用Compose提升渲染性能

协程异步处理避免阻塞

Room数据库优化查询

Coil图片缓存机制

网络请求缓存策略

🔒 安全考虑
HTTPS通信（生产环境）

敏感数据加密存储

代码混淆保护

权限最小化原则

🧪 测试
项目包含完整的测试结构：

单元测试（Repository、ViewModel）

UI测试（Compose组件）

集成测试（API接口）

📝 TODO
[√] 集成Media3视频播放器

[√] 实现弹幕系统

[√] 添加视频下载功能

[√] 完善用户系统

[√] 实现投屏功能

[√] 添加性能监控

[√] 完善错误处理

[√] 添加单元测试

演示图片
https://wmimg.com/i/1502/2026/05/6a1aa5a44192d.png
https://wmimg.com/i/1502/2026/05/6a1aa5a044b88.png
https://wmimg.com/i/1502/2026/05/6a1aa5a057047.png
https://wmimg.com/i/1502/2026/05/6a1aa5a3948a7.png
https://wmimg.com/i/1502/2026/05/6a1aa59fd3bb3.png
https://wmimg.com/i/1502/2026/05/6a1aa5a03fea0.png
https://wmimg.com/i/1502/2026/05/6a1ae033e1afd.png

🤝 贡献
欢迎提交Issue和Pull Request来改进项目。

📄 许可证
本项目基于MIT许可证开源。

🤝 开源地址
https://github.com/jinian123H/aijia

项目状态: ✅ 基础架构完成，可运行

安装教程
后端
开放9090端口
.env

env
# 加密配置
AES_PASSPHRASE=AijiaAES2026SecurePassphrase!@#        -----这是app密钥
ENABLE_ENCRYPTION=true
PLAY_SIGN_SECRET=kumiao_play_sign_2026
PLAY_SIGN_EXPIRE=600
https://wmimg.com/i/1502/2026/05/6a1ae02af0de4.png

前端
aijia/app/src/main/assets/api_config.json

json
{
  "url": "http://112.124.36.198:9090",                  -----后端
  "aes_passphrase": "AijiaAES2026SecurePassphrase!@#",  -----这是app密钥
  "enable_encryption": true
}
下载地址: https://www.suiyuanlu.cn/index.php/archives/40/

本回答由 AI 生成，内容仅供参考，请仔细甄别
下载md文档给我
我无法直接生成可下载的MD文件，但我可以为您提供完整的Markdown内容，您可以直接复制保存为 .md 文件。

复制以下内容，保存为 README.md：

markdown
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
app/src/main/java/com/aijia/video/
├── data/ # Data Layer
│ ├── local/ # Local Data Source
│ │ ├── dao/ # Data Access Objects
│ │ ├── converters/ # Type Converters
│ │ └── AppDatabase.kt # Database Config
│ ├── model/ # Data Models
│ ├── remote/ # Remote Data Source
│ │ └── ApiService.kt # API Interface
│ └── repository/ # Data Repository
├── di/ # Dependency Injection Modules
│ ├── DatabaseModule.kt
│ └── NetworkModule.kt
├── ui/ # UI Layer
│ ├── components/ # Reusable Components
│ ├── navigation/ # Navigation Config
│ ├── screens/ # Screens
│ │ ├── home/ # Home
│ │ ├── player/ # Player
│ │ ├── search/ # Search
│ │ └── profile/ # Profile
│ └── theme/ # Theme Styles
├── VideoApplication.kt # Application Class
└── MainActivity.kt # Main Activity

text

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
Configure environment

Ensure Android Studio and JDK 11+ are installed

Configure Android SDK

Build the project

bash
./gradlew build
Run the app

bash
./gradlew installDebug
📦 Dependency Versions
Key dependency versions (see gradle/libs.versions.toml):

Compose: 2024.09.00

Navigation: 2.8.0

Hilt: 2.48

Retrofit: 2.9.0

OkHttp: 4.12.0

Room: 2.6.1

Media3: 1.2.0

Coil: 2.6.0

🔧 Configuration
Network Configuration
Base URL: http://192.168.31.999:9090

HTTP plaintext supported (for development testing)

HTTPS recommended for production

Database
Database Name: aijia_video_database

Version: 1

Uses Room Database

Permissions
Network access

Storage access

Wake lock permission

🎨 UI Design
Follows Material Design 3 guidelines

Dynamic color theme support

Responsive layout for different screens

Dark theme support

📈 Performance Optimization
Compose for rendering performance

Coroutine async processing to avoid blocking

Room database query optimization

Coil image caching mechanism

Network request caching strategy

🔒 Security Considerations
HTTPS communication (production)

Encrypted storage for sensitive data

Code obfuscation protection

Principle of least privilege for permissions

🧪 Testing
Complete test structure includes:

Unit tests (Repository, ViewModel)

UI tests (Compose components)

Integration tests (API interfaces)

📝 TODO
[√] Integrated Media3 video player

[√] Implemented bullet chat system

[√] Added video download feature

[√] Completed user system

[√] Implemented screen casting

[√] Added performance monitoring

[√] Improved error handling

[√] Added unit tests

Demo Screenshots
https://wmimg.com/i/1502/2026/05/6a1aa5a44192d.png
https://wmimg.com/i/1502/2026/05/6a1aa5a044b88.png
https://wmimg.com/i/1502/2026/05/6a1aa5a057047.png
https://wmimg.com/i/1502/2026/05/6a1aa5a3948a7.png
https://wmimg.com/i/1502/2026/05/6a1aa59fd3bb3.png
https://wmimg.com/i/1502/2026/05/6a1aa5a03fea0.png
https://wmimg.com/i/1502/2026/05/6a1ae033e1afd.png

🤝 Contributing
Issues and Pull Requests are welcome to improve the project.

📄 License
This project is open-source under the MIT License.

🤝 Open Source Repository
https://github.com/jinian123H/aijia

Project Status: ✅ Basic architecture complete, ready to run

Installation Guide
Backend
Open port 9090
.env

env
# Encryption Configuration
AES_PASSPHRASE=AijiaAES2026SecurePassphrase!@#        -----App encryption key
ENABLE_ENCRYPTION=true
PLAY_SIGN_SECRET=kumiao_play_sign_2026
PLAY_SIGN_EXPIRE=600
https://wmimg.com/i/1502/2026/05/6a1ae02af0de4.png

Frontend
aijia/app/src/main/assets/api_config.json

json
{
  "url": "http://112.124.36.198:9090",                  -----Backend URL
  "aes_passphrase": "AijiaAES2026SecurePassphrase!@#",  -----App encryption key
  "enable_encryption": true
}
中文版 (Chinese Version)
Aijia 视频应用
现代化Kotlin视频播放应用，采用最新Android技术栈。

🚀 技术栈
开发语言: Kotlin 100%

架构模式: MVVM + Clean Architecture

UI框架: Jetpack Compose + Material Design 3

依赖注入: Hilt

网络请求: Retrofit + OkHttp + Coroutines

数据库: Room + SQLite

图片加载: Coil

视频播放: AndroidX Media3 (ExoPlayer)

导航: Navigation Compose

📱 功能特性
核心功能
✅ 视频播放与控制

✅ 视频搜索与分类浏览

✅ 用户系统（登录/注册）

✅ 播放历史记录

✅ 视频收藏功能

✅ 评论与弹幕系统

✅ 直播功能

✅ 视频下载（离线观看）

技术特性
✅ 现代化UI设计

✅ 响应式布局

✅ 暗色主题支持

✅ 网络状态缓存

✅ 本地数据持久化

✅ 依赖注入架构

✅ 协程异步处理

🏗️ 项目结构
text
app/src/main/java/com/aijia/video/
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── dao/                   # 数据访问对象
│   │   ├── converters/            # 类型转换器
│   │   └── AppDatabase.kt         # 数据库配置
│   ├── model/                     # 数据模型
│   ├── remote/                    # 远程数据源
│   │   └── ApiService.kt          # API接口
│   └── repository/                # 数据仓库
├── di/                           # 依赖注入模块
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── ui/                           # UI层
│   ├── components/                # 可复用组件
│   ├── navigation/                # 导航配置
│   ├── screens/                   # 页面Screen
│   │   ├── home/                 # 首页
│   │   ├── player/               # 播放页
│   │   ├── search/               # 搜索页
│   │   └── profile/              # 个人中心
│   └── theme/                    # 主题样式
├── VideoApplication.kt            # Application类
└── MainActivity.kt               # 主Activity
🛠️ 开发环境
Android Studio: Giraffe (2022.3.1) 或更高版本

JDK: 11 或更高版本

Gradle: 8.5

Kotlin: 2.0.21

最低Android版本: Android 7.0 (API 24)

目标Android版本: Android 14 (API 34)

🚀 快速开始
克隆项目

bash
git clone <repository-url>
cd aijia
配置环境

确保已安装Android Studio和JDK 11+

配置Android SDK

构建项目

bash
./gradlew build
运行应用

bash
./gradlew installDebug
📦 依赖库版本
主要依赖库版本（详见gradle/libs.versions.toml）：

Compose: 2024.09.00

Navigation: 2.8.0

Hilt: 2.48

Retrofit: 2.9.0

OkHttp: 4.12.0

Room: 2.6.1

Media3: 1.2.0

Coil: 2.6.0

🔧 配置说明
网络配置
基础URL: http://192.168.31.999:9090

支持HTTP明文传输（用于开发测试）

生产环境建议使用HTTPS

数据库
数据库名称: aijia_video_database

版本: 1

使用Room数据库

权限配置
网络访问权限

存储访问权限

设备唤醒锁权限

🎨 UI设计
遵循Material Design 3设计规范

支持动态颜色主题

响应式布局适配不同屏幕

暗色主题支持

📈 性能优化
使用Compose提升渲染性能

协程异步处理避免阻塞

Room数据库优化查询

Coil图片缓存机制

网络请求缓存策略

🔒 安全考虑
HTTPS通信（生产环境）

敏感数据加密存储

代码混淆保护

权限最小化原则

🧪 测试
项目包含完整的测试结构：

单元测试（Repository、ViewModel）

UI测试（Compose组件）

集成测试（API接口）

📝 TODO
[√] 集成Media3视频播放器

[√] 实现弹幕系统

[√] 添加视频下载功能

[√] 完善用户系统

[√] 实现投屏功能

[√] 添加性能监控

[√] 完善错误处理

[√] 添加单元测试

演示图片
https://wmimg.com/i/1502/2026/05/6a1aa5a44192d.png
https://wmimg.com/i/1502/2026/05/6a1aa5a044b88.png
https://wmimg.com/i/1502/2026/05/6a1aa5a057047.png
https://wmimg.com/i/1502/2026/05/6a1aa5a3948a7.png
https://wmimg.com/i/1502/2026/05/6a1aa59fd3bb3.png
https://wmimg.com/i/1502/2026/05/6a1aa5a03fea0.png
https://wmimg.com/i/1502/2026/05/6a1ae033e1afd.png

🤝 贡献
欢迎提交Issue和Pull Request来改进项目。

📄 许可证
本项目基于MIT许可证开源。

🤝 开源地址
https://github.com/jinian123H/aijia

项目状态: ✅ 基础架构完成，可运行

安装教程
后端
开放9090端口
.env

env
# 加密配置
AES_PASSPHRASE=AijiaAES2026SecurePassphrase!@#        -----这是app密钥
ENABLE_ENCRYPTION=true
PLAY_SIGN_SECRET=kumiao_play_sign_2026
PLAY_SIGN_EXPIRE=600
https://wmimg.com/i/1502/2026/05/6a1ae02af0de4.png

前端
aijia/app/src/main/assets/api_config.json

json
{
  "url": "http://112.124.36.198:9090",                  -----后端
  "aes_passphrase": "AijiaAES2026SecurePassphrase!@#",  -----这是app密钥
  "enable_encryption": true
}
下载地址: https://www.suiyuanlu.cn/index.php/archives/40/

text

---

**操作步骤：**

1. 全选上面的内容，复制
2. 打开记事本（或其他文本编辑器）
3. 粘贴内容
4. 点击 `文件` → `另存为`
5. 文件名输入 `README.md`
6. 保存类型选择 `所有文件 (*.*)`
7. 点击保存即可
