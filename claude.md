# English Word App - Claude Development Notes

## Project Information
- **Project Name**: English Word App
- **Description**: Android应用学习英语单词，支持AI聊天和场景训练
- **Project Path**: C:\claude-project\english-word-app

## Development Environment

### Maven
- **Maven Home**: `C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3`
- **Maven Executable**: `C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd`
- **Maven Repository**: `C:\Users\weika\.m2\repository`
- **Build Command**:
  ```bash
  cd english-word-backend
  "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests
  ```
- **Full Command with IDEA params**:
  ```bash
  "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd" -Didea.version=2025.2.5 -Dmaven.ext.class.path=C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven-event-listener.jar -Djansi.passthrough=true -Dstyle.color=always -Dmaven.repo.local=C:\Users\weika\.m2\repository org.apache.maven.plugins:maven-clean-plugin:3.3.2:clean -f pom.xml
  ```

### IntelliJ IDEA
- **Installation Path**: `C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\bin`
- **Executable**: `idea64.exe`
- **Open Project Command**:
  ```bash
  cmd /c start "" "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\bin\idea64.exe" C:\claude-project\english-word-app
  ```

### Android Device
- **Device ID**: 4plz9paebem7q4qg
- **Device Type**: Xiaomi (Honor 25080RABDC, Android 15)

## API Configuration

### Local Database
- **Host**: `localhost:3306`
- **Database**: `english_word_app`
- **Username**: `root`
- **Password**: `123456`
- **Create Database Command**:
  ```bash
  mysql -h localhost -P 3306 -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS english_word_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  ```

### Remote Database (Production)

### Backend Server
- **API Base URL**: `http://47.83.126.42:8885/api/`
- **Swagger UI**: `http://47.83.126.42:8885/api/swagger-ui.html`

### ZhipuAI (智谱AI)
- **API Key**: `cea9d940b7b7498d916e1c924ba3b6ca.zwaG7aTXwBW60Dr4`
- **API URL**: `https://open.bigmodel.cn/api/paas/v4/chat/completions`
- **Model**: `glm-4-flash`
- **Configuration File**: `english-word-backend/src/main/resources/application.yml`

## Project Structure

### Android App (`android-word-app/`)
- **Package**: `com.englishword`
- **Language**: Kotlin
- **Min SDK**: 24
- **Target SDK**: 34

#### Key Screens
- `LoginScreen.kt` - 登录/注册页面
- `WordVaultScreen.kt` - 单词库主页（支持多选训练）
- `AIChatScreen.kt` - AI聊天界面（支持咨询和训练模式）
- `TrainingSummaryScreen.kt` - 训练总结页面

#### Key Components
- `MainActivity.kt` - 主Activity，导航控制
- `RetrofitClient.kt` - API客户端配置
- `TokenManager.kt` - Token管理
- `ApiService.kt` - API接口定义

### Backend (`english-word-backend/`)
- **Language**: Java
- **Framework**: Spring Boot 3.x
- **Port**: 8885
- **Context Path**: `/api`

#### Key Controllers
- `AuthController.java` - 认证接口
- `WordController.java` - 单词管理
- `AIController.java` - AI聊天接口

#### Key Services
- `AIConversationService.java` - AI对话服务
- `ZhipuAIService.java` - 智谱AI集成

## Development Commands

### Build Android APK
```bash
cd android-word-app
./gradlew assembleDebug
```

### Install APK to Device
```bash
adb install -r android-word-app/app/build/outputs/apk/debug/app-debug.apk
```

### View Device Logs
```bash
adb logcat | grep -E "LoginViewModel|AIChatScreen|EnglishWord"
```

### Check Connected Devices
```bash
adb devices
```

## Current Features

### ✅ Implemented
1. 用户注册/登录
2. JWT Token认证
3. 单词库管理
4. 多选单词功能（长按进入，最多5个）
5. AI聊天功能（集成智谱AI）
6. 训练模式（基于选中的单词）
7. FAB按钮（AI Assistant + Start Training）
8. 单词使用追踪
9. 对话轮次计数

### 🚧 In Progress
- 测试AI聊天功能
- 优化训练prompt
- 实现多维度评分系统
- 完善训练总结页面

## Important Notes

### Token Management
- Token保存在Android DataStore中
- 登录/注册后自动保存token
- RetrofitClient自动在请求头添加Authorization

### AI Chat Modes
- **word_inquiry**: 咨询模式（询问单词含义）
- **word_training**: 训练模式（练习使用单词）

### Navigation Structure
- Bottom tabs: Vault, AI Chat
- Scene Practice: 通过AI Chat的Train按钮进入
- Training Summary: 训练完成后显示的结果页面

## Git Repository
- **Current Branch**: master
- **Recent Commit**: feat: 实现单词库多选训练功能 (Phase 1)
