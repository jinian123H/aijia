# Aijia 视频应用

现代化Kotlin视频播放应用，采用最新Android技术栈。

## 🚀 技术栈

- **开发语言**: Kotlin 100%
- **架构模式**: MVVM + Clean Architecture
- **UI框架**: Jetpack Compose + Material Design 3
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
- ✅ 现代化UI设计
- ✅ 响应式布局
- ✅ 暗色主题支持
- ✅ 网络状态缓存
- ✅ 本地数据持久化
- ✅ 依赖注入架构
- ✅ 协程异步处理

## 🏗️ 项目结构

```
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
```



## 🛠️ 开发环境

- **Android Studio**: Giraffe (2022.3.1) 或更高版本
- **JDK**: 11 或更高版本
- **Gradle**: 8.5
- **Kotlin**: 2.0.21
- **最低Android版本**: Android 7.0 (API 24)
- **目标Android版本**: Android 14 (API 34)

## 🚀 快速开始

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd aijia
   ```

2. **配置环境**
   - 确保已安装Android Studio和JDK 11+
   - 配置Android SDK

3. **构建项目**
   ```bash
   ./gradlew build
   ```

4. **运行应用**
   ```bash
   ./gradlew installDebug
   ```

## 📦 依赖库版本

主要依赖库版本（详见`gradle/libs.versions.toml`）：

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
- 基础URL: `http://1.94.212.201:8090`
- 支持HTTP明文传输（用于开发测试）
- 生产环境建议使用HTTPS

### 数据库
- 数据库名称: `aijia_video_database`
- 版本: 1
- 使用Room数据库

### 权限配置
- 网络访问权限
- 存储访问权限
- 设备唤醒锁权限

## 🎨 UI设计

- 遵循Material Design 3设计规范
- 支持动态颜色主题
- 响应式布局适配不同屏幕
- 暗色主题支持

## 📈 性能优化

- 使用Compose提升渲染性能
- 协程异步处理避免阻塞
- Room数据库优化查询
- Coil图片缓存机制
- 网络请求缓存策略

## 🔒 安全考虑

- HTTPS通信（生产环境）
- 敏感数据加密存储
- 代码混淆保护
- 权限最小化原则

## 🧪 测试

项目包含完整的测试结构：
- 单元测试（Repository、ViewModel）
- UI测试（Compose组件）
- 集成测试（API接口）

## 📝 TODO

- [ ] 集成Media3视频播放器
- [ ] 实现弹幕系统
- [ ] 添加视频下载功能
- [ ] 完善用户系统
- [ ] 实现投屏功能
- [ ] 添加性能监控
- [ ] 完善错误处理
- [ ] 添加单元测试

## 🤝 贡献

欢迎提交Issue和Pull Request来改进项目。

## 📄 许可证

本项目基于MIT许可证开源。

---

**项目状态**: ✅ 基础架构完成，可运行  
**下一步**: 完善视频播放器和具体功能实现 🚀
