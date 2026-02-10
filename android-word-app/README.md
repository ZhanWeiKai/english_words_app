# English Word Learning App (Android)

一个现代化的英语单词学习 Android 应用，使用 **Jetpack Compose** 构建。

## ✨ 特性

- ✅ **用户认证**: 注册/登录功能，JWT令牌认证
- ✅ **单词宝库**: 添加、搜索、管理单词，按掌握程度筛选
- ✅ **AI聊天**: 与AI助手互动学习英语
- ✅ **场景练习**: 模拟真实场景进行口语练习
- ✅ **训练总结**: 查看学习进度和统计数据
- ✅ **现代化UI**: Material Design 3，流畅的动画效果
- ✅ **底部导航**: 便捷的页面切换

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material 3
- **架构**: MVVM + Navigation Component
- **网络请求**: Retrofit2 + OkHttp
- **本地存储**: DataStore (替代SharedPreferences)
- **依赖注入**: 手动DI (简单高效)
- **异步处理**: Coroutines + Flow

## 📁 项目结构

```
com.englishword/
├── data/
│   ├── api/
│   │   └── ApiService.kt          # Retrofit API接口
│   ├── model/
│   │   └── Models.kt              # 数据模型 (User, Word, ChatMessage等)
│   ├── TokenManager.kt            # JWT令牌管理 (DataStore)
│   └── RetrofitClient.kt          # Retrofit客户端配置
├── ui/
│   ├── screens/
│   │   ├── LoginScreen.kt         # 登录/注册页面
│   │   ├── LoginViewModel.kt      # 登录业务逻辑
│   │   ├── WordVaultScreen.kt     # 单词宝库
│   │   ├── AIChatScreen.kt        # AI聊天
│   │   ├── ScenePracticeScreen.kt # 场景练习
│   │   └── TrainingSummaryScreen.kt # 训练总结
│   └── theme/
│       ├── Color.kt               # 颜色定义
│       ├── Type.kt                # 字体样式
│       └── Theme.kt               # 主题配置
└── MainActivity.kt                # 主入口 + 导航设置
```

## 🎨 设计系统

### 颜色方案
- **主色**: `#e07a5f` (温暖的橙红色)
- **成功色**: `#81b29a` (柔和的绿色)
- **背景色**: `#faf9f7` (米白色)
- **文字色**: `#2d3436` (深灰色)

### 掌握程度星级
- ⭐ 1星: 初学者 (BEGINNER)
- ⭐⭐ 2星: 基础 (ELEMENTARY)
- ⭐⭐⭐ 3星: 中级 (INTERMEDIATE)
- ⭐⭐⭐⭐ 4星: 高级 (ADVANCED)
- ⭐⭐⭐⭐⭐ 5星: 精通 (PROFICIENT)

## 🔌 后端集成

- **Base URL**: `http://47.83.126.42:8885/api/`
- **认证方式**: JWT Bearer Token
- **存储方式**: DataStore (本地令牌持久化)

### API端点
- `POST /auth/register` - 用户注册
- `POST /auth/login` - 用户登录
- `GET /words` - 获取单词列表
- `POST /words` - 添加单词
- `PUT /words/{id}/mastery` - 更新掌握程度
- `POST /ai/chat` - AI聊天

## 🚀 构建说明

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 34
- Gradle 8.2

### 构建步骤

```bash
# 1. 进入项目目录
cd android-word-app

# 2. 清理构建
./gradlew clean

# 3. 构建Debug APK
./gradlew assembleDebug

# 4. 安装到设备
./gradlew installDebug

# 5. 构建Release APK (需要签名配置)
./gradlew assembleRelease
```

### APK输出位置
- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release.apk`

## 📱 应用截图

### 1. 登录页面
- 简洁的登录/注册界面
- Material Design 3 风格
- 输入验证和错误提示

### 2. 单词宝库
- 卡片式单词展示
- 掌握程度星级显示
- 搜索和筛选功能
- 浮动添加按钮

### 3. AI聊天
- 气泡式对话界面
- 实时消息发送
- 自动滚动到最新消息

### 4. 场景练习
- 多场景选择
- 沉浸式练习体验
- 完成度反馈

### 5. 训练总结
- 学习统计数据
- 成就展示
- 鼓励继续学习

## 🎯 开发路线

### ✅ 已完成
- [x] 项目架构搭建
- [x] 用户认证功能
- [x] 5个主要页面UI
- [x] 底部导航
- [x] Material Design 3 主题
- [x] 后端API集成

### 🚧 进行中
- [ ] 实际API调用和数据处理
- [ ] 单词CRUD完整功能
- [ ] AI聊天真实集成
- [ ] 场景练习内容

### 📋 计划中
- [ ] 动画效果优化
- [ ] 离线模式支持
- [ ] 单词发音功能
- [ ] 复习提醒通知
- [ ] 数据同步

## 🔧 配置说明

### build.gradle.kts
```kotlin
// 后端服务器配置
buildConfigField("String", "BASE_URL", "\"http://47.83.126.42:8885/api/\"")
```

### AndroidManifest.xml
```xml
android:usesCleartextTraffic="true"  <!-- 允许HTTP流量 -->
```

## 📄 License

本项目采用 MIT License。

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

## 📞 联系方式

- 项目地址: [GitHub](https://github.com/ZhanWeiKai/english_words_app.git)
- 后端仓库: [english-word-backend](../english-word-backend/)

---

**使用 Jetpack Compose 构建** 🚀 | **Material Design 3** 🎨 | **Kotlin** 💙
