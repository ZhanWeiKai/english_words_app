# English Word App - 技术架构设计文档

**项目名称**: English Word App (英语单词学习应用)
**创建日期**: 2025-02-05
**技术栈**: Spring Boot + MySQL + Android + 智谱AI
**部署方式**: Docker Compose

---

## 📋 目录

- [一、技术栈选型](#一技术栈选型)
- [二、服务器配置](#二服务器配置)
- [三、数据库设计](#三数据库设计)
- [四、后端架构设计](#四后端架构设计)
- [五、Android客户端架构](#五android客户端架构)
- [六、API接口设计](#六api接口设计)
- [七、AI服务集成](#七ai服务集成)
- [八、部署方案](#八部署方案)
- [九、开发计划](#九开发计划)
- [十、成本估算](#十成本估算)

---

## 一、技术栈选型

### 1.1 后端技术栈

| 技术 | 版本 | 说明 | 参考 |
|------|------|------|------|
| **Spring Boot** | 3.2.1 | Web框架 | 复用 eatwhere |
| **JDK** | 17 | Java版本 | 复用 eatwhere |
| **MySQL** | 8.0+ | 数据库 | 复用 eatwhere |
| **Spring Data JPA** | 3.2.1 | ORM框架 | 复用 eatwhere |
| **Spring WebSocket** | 3.2.1 | WebSocket支持 | 复用 eatwhere |
| **JWT** | 0.12.3 | Token认证 | 复用 eatwhere |
| **BCrypt** | - | 密码加密 | 复用 eatwhere |
| **SpringDoc** | 2.3.0 | API文档 | 复用 eatwhere |
| **Maven** | 3.9+ | 构建工具 | 复用 eatwhere |

### 1.2 Android技术栈

| 技术 | 版本 | 说明 | 参考 |
|------|------|------|------|
| **语言** | Java | 开发语言 | 复用 eatwhere |
| **Min SDK** | 24 (Android 7.0) | 最低版本 | 复用 eatwhere |
| **Target SDK** | 34 (Android 14) | 目标版本 | 复用 eatwhere |
| **架构** | MVVM + Repository | 架构模式 | 复用 eatwhere |
| **Retrofit** | 2.9.0 | 网络请求 | 复用 eatwhere |
| **OkHttp** | 4.12.0 | HTTP客户端 | 复用 eatwhere |
| **Gson** | 2.10.1 | JSON解析 | 复用 eatwhere |
| **Room Database** | 2.6.0 | 本地数据库 | 新增（单词缓存） |
| **WebSocket** | OkHttp WebSocket | 实时通信 | 复用 eatwhere |

### 1.3 AI服务选型

**选择：智谱AI (GLM-4-Flash)**

| 对比项 | 智谱AI | OpenAI | 推荐 |
|--------|--------|--------|------|
| **API调用成本** | ✅ 新用户免费额度 + 低费率 | ❌ 纯付费，较高 | **智谱** |
| **国内访问速度** | ✅ 快速无延迟 | ❌ 需代理，不稳定 | **智谱** |
| **中文理解能力** | ✅ 专为中文优化 | ⚠️ 英文为主 | **智谱** |
| **API稳定性** | ✅ 国内服务稳定 | ⚠️ 网络问题频发 | **智谱** |
| **配置经验** | ✅ eatwhere已验证可用 | ❌ 需重新配置 | **智谱** |
| **免费额度** | ✅ 有 | ❌ 无 | **智谱** |

**结论：使用智谱AI，成本更低，访问更快，配置更简单**

---

## 二、服务器配置

### 2.1 服务器信息

**重要：以下配置独立于 eatwhere 项目**

| 配置项 | 值 | 说明 |
|--------|-----|------|
| **服务器IP** | 47.242.74.112 | 与 eatwhere 共用服务器 |
| **域名** | api.jamesweb.org | 与 eatwhere 共用域名 |
| **后端端口** | **8885** ⚠️ | 新端口，避免与 eatwhere(8883) 冲突 |
| **数据库端口** | **3307** ⚠️ | 新端口，避免与 eatwhere(3306) 冲突 |
| **WebSocket端口** | **8885** ⚠️ | 与后端共用端口，路径 `/api/ws` |

### 2.2 访问地址

| 服务 | URL | 说明 |
|------|-----|------|
| **API基础地址** | `http://47.242.74.112:8885/api` | 后端API |
| **API文档** | `http://47.242.74.112:8885/api/swagger-ui.html` | Swagger UI |
| **WebSocket** | `ws://47.242.74.112:8885/api/ws` | 实时通信 |

### 2.3 Docker端口映射

```yaml
# docker-compose.yml 端口配置
services:
  mysql:
    ports:
      - "3307:3306"  # 宿主机3307映射到容器3306

  app:
    ports:
      - "8885:8885"  # 后端API端口
```

---

## 三、数据库设计

### 3.1 数据库配置

**重要：使用独立的数据库和端口**

| 配置项 | 值 | 说明 |
|--------|-----|------|
| **数据库名** | `english_word_app` ⚠️ | 独立数据库 |
| **字符集** | utf8mb4 | 支持emoji和多语言 |
| **排序规则** | utf8mb4_unicode_ci | |
| **用户名** | root | 复用MySQL root用户 |
| **密码** | 123456 | 复用MySQL root密码 |
| **连接端口** | 3307 ⚠️ | 独立端口 |

### 3.2 数据表设计

#### 3.2.1 用户表 (user)

```sql
CREATE TABLE user (
    user_id VARCHAR(255) PRIMARY KEY COMMENT '用户唯一ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（登录用）',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

#### 3.2.2 单词表 (word)

```sql
CREATE TABLE word (
    word_id VARCHAR(255) PRIMARY KEY COMMENT '单词唯一ID',
    user_id VARCHAR(255) NOT NULL COMMENT '所属用户ID',
    word VARCHAR(100) NOT NULL COMMENT '单词（小写）',
    pronunciation VARCHAR(200) COMMENT '音标（IPA）',
    part_of_speech VARCHAR(50) COMMENT '词性（n./v./adj./adv.等）',
    definition TEXT COMMENT '中文释义',
    example_sentence TEXT COMMENT '例句（英文）',
    example_translation TEXT COMMENT '例句翻译',
    mastery_level INT DEFAULT 1 COMMENT '掌握程度（1-5星）',
    status VARCHAR(20) DEFAULT 'LEARNING' COMMENT '状态：LEARNING=学习中，MASTERED=已掌握',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_word (word),
    INDEX idx_status (status),
    INDEX idx_mastery_level (mastery_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词表';
```

#### 3.2.3 训练会话表 (training_session)

```sql
CREATE TABLE training_session (
    session_id VARCHAR(255) PRIMARY KEY COMMENT '会话唯一ID',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID',
    word_ids JSON NOT NULL COMMENT '训练的单词ID列表',
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP NULL COMMENT '结束时间',
    results JSON COMMENT '训练结果（单词ID、掌握等级变化等）',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练会话表';
```

#### 3.2.4 AI对话记录表 (ai_conversation)

```sql
CREATE TABLE ai_conversation (
    conversation_id VARCHAR(255) PRIMARY KEY COMMENT '对话唯一ID',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID',
    messages JSON NOT NULL COMMENT '对话历史（JSON数组）',
    context_word_id VARCHAR(255) COMMENT '关联的单词ID（Word Inquiry模式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录表';
```

### 3.3 数据库关系图

```
user (用户表)
  │
  ├─┬─ word (单词表，一对多)
  │ │
  │ └─ training_session (训练会话表，一对多)
  │
  └── ai_conversation (AI对话记录表，一对多)
```

---

## 四、后端架构设计

### 4.1 项目结构

```
english-word-backend/
├── src/main/java/com/englishword/
│   ├── EnglishWordAppApplication.java      # 启动类
│   │
│   ├── controller/                          # REST API控制器
│   │   ├── AuthController.java             # 认证接口
│   │   ├── WordController.java             # 单词管理接口
│   │   ├── TrainingController.java         # 训练接口
│   │   ├── AIController.java               # AI聊天接口
│   │   └── UserController.java             # 用户信息接口
│   │
│   ├── service/                             # 业务逻辑层
│   │   ├── AuthService.java
│   │   ├── WordService.java
│   │   ├── TrainingService.java
│   │   ├── AIService.java
│   │   └── UserService.java
│   │
│   ├── repository/                          # 数据访问层
│   │   ├── UserRepository.java
│   │   ├── WordRepository.java
│   │   ├── TrainingSessionRepository.java
│   │   └── AIConversationRepository.java
│   │
│   ├── entity/                              # 数据库实体
│   │   ├── User.java
│   │   ├── Word.java
│   │   ├── TrainingSession.java
│   │   └── AIConversation.java
│   │
│   ├── dto/                                 # 数据传输对象
│   │   ├── request/                        # 请求DTO
│   │   │   ├── LoginRequest.java
│   │   │   ├── AddWordRequest.java
│   │   │   ├── StartTrainingRequest.java
│   │   │   └── AIChatRequest.java
│   │   └── response/                       # 响应DTO
│   │       ├── WordResponse.java
│   │       ├── TrainingResultResponse.java
│   │       └── AIChatResponse.java
│   │
│   ├── config/                              # 配置类
│   │   ├── WebSocketConfig.java           # WebSocket配置
│   │   ├── SecurityConfig.java            # 安全配置
│   │   ├── CorsConfig.java                # 跨域配置
│   │   └── ZhipuAIConfig.java             # 智谱AI配置
│   │
│   └── util/                               # 工具类
│       ├── JwtUtil.java                   # JWT工具
│       └── ZhipuAIClient.java             # 智谱AI客户端
│
├── src/main/resources/
│   ├── application.yml                     # 主配置文件
│   ├── application-local.yml.example       # 本地开发配置模板
│   └── db/migration/                       # 数据库迁移脚本（Flyway）
│       └── V1__init_schema.sql
│
├── docker-compose.yml                      # Docker编排文件
├── Dockerfile                              # Docker镜像构建文件
├── pom.xml                                 # Maven配置
└── README.md
```

### 4.2 核心配置文件

#### application.yml

```yaml
server:
  port: 8885                                    # ⚠️ 独立端口
  servlet:
    context-path: /api

spring:
  application:
    name: english-word-backend

  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3307/english_word_app?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
    open-in-view: false

  # 文件上传配置
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB
      file-size-threshold: 2MB

# JWT配置
jwt:
  secret: EnglishWordAppSecretKey2026ForJWTTokenGenerationMustBeLongEnough
  expiration: 604800000  # 7天 (单位: 毫秒)

# 智谱AI配置
zhipuai:
  api-key: ${ZHIPUAI_API_KEY}             # 从环境变量读取
  api-url: https://open.bigmodel.cn/api/paas/v4/chat/completions
  model: glm-4-flash                     # 使用快速模型（成本更低）
  max-tokens: 2000                       # 最大回复长度
  temperature: 0.7                       # 创造性程度

# 日志配置
logging:
  level:
    root: INFO
    com.englishword: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# SpringDoc配置
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
  packages-to-scan: com.englishword.controller
```

### 4.3 核心业务逻辑

#### 4.3.1 认证流程

```java
// 1. 用户注册
POST /api/auth/register
Request: { "username": "testuser", "password": "123456", "nickname": "测试用户" }
Response: { "code": 200, "message": "注册成功", "data": { "userId": "...", "token": "..." } }

// 2. 用户登录
POST /api/auth/login
Request: { "username": "testuser", "password": "123456" }
Response: { "code": 200, "message": "登录成功", "data": { "userId": "...", "token": "..." } }

// 3. Token验证
GET /api/auth/me
Headers: Authorization: Bearer <token>
Response: { "code": 200, "data": { "userId": "...", "username": "..." } }
```

#### 4.3.2 单词管理流程

```java
// 1. 添加单词
POST /api/words
Headers: Authorization: Bearer <token>
Request: { "word": "ephemeral", "definition": "短暂的" }
Response: { "code": 200, "data": { "wordId": "...", "masteryLevel": 1 } }

// 2. 获取单词列表
GET /api/words?status=LEARNING&page=0&size=20
Headers: Authorization: Bearer <token>
Response: {
  "code": 200,
  "data": {
    "content": [
      { "wordId": "...", "word": "ephemeral", "masteryLevel": 2, "status": "LEARNING" }
    ],
    "totalElements": 100
  }
}

// 3. 更新掌握程度
PUT /api/words/{wordId}/mastery
Headers: Authorization: Bearer <token>
Request: { "masteryLevel": 3 }
Response: { "code": 200, "message": "更新成功" }
```

#### 4.3.3 AI聊天流程

```java
// 1. 发送消息（WebSocket）
WS /api/ws
Headers: Authorization: Bearer <token>

// 客户端发送
{
  "type": "chat",
  "conversationId": "...",  // 可选，继续历史对话
  "message": "ephemeral是什么意思？"
}

// 服务端返回
{
  "type": "ai_response",
  "message": "**Ephemeral** /ɪˈfemərəl/ (adj.) 短暂的；瞬息的...",
  "suggestions": [
    { "word": "ephemeral", "action": "add_to_list" },
    { "word": "ephemeral", "action": "train" }
  ]
}
```

---

## 五、Android客户端架构

### 5.1 项目结构

```
english-word-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/englishword/
│   │   │   │
│   │   │   ├── ui/                           # UI层
│   │   │   │   ├── login/                   # 登录/注册
│   │   │   │   │   ├── LoginActivity.java
│   │   │   │   │   └── LoginViewModel.java
│   │   │   │   │
│   │   │   │   ├── main/                    # 主页（单词库）
│   │   │   │   │   ├── MainActivity.java
│   │   │   │   │   ├── MainViewModel.java
│   │   │   │   │   └── WordAdapter.java
│   │   │   │   │
│   │   │   │   ├── aichat/                  # AI聊天
│   │   │   │   │   ├── AIChatActivity.java
│   │   │   │   │   ├── AIChatViewModel.java
│   │   │   │   │   └── MessageAdapter.java
│   │   │   │   │
│   │   │   │   ├── training/                # 场景练习
│   │   │   │   │   ├── ScenePracticeActivity.java
│   │   │   │   │   ├── TrainingViewModel.java
│   │   │   │   │   └── RoleCardAdapter.java
│   │   │   │   │
│   │   │   │   └── summary/                 # 训练总结
│   │   │   │       ├── TrainingSummaryActivity.java
│   │   │   │       └── SummaryViewModel.java
│   │   │   │
│   │   │   ├── data/                         # 数据层
│   │   │   │   ├── api/                     # API接口
│   │   │   │   │   ├── ApiService.java
│   │   │   │   │   ├── WebSocketClient.java
│   │   │   │   │   └── ApiResponse.java
│   │   │   │   │
│   │   │   │   ├── model/                   # 数据模型
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Word.java
│   │   │   │   │   ├── TrainingSession.java
│   │   │   │   │   └── ChatMessage.java
│   │   │   │   │
│   │   │   │   ├── repository/              # 数据仓库
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── WordRepository.java
│   │   │   │   │   └── TrainingRepository.java
│   │   │   │   │
│   │   │   │   ├── local/                   # 本地数据库
│   │   │   │   │   ├── AppDatabase.java     # Room数据库
│   │   │   │   │   ├── WordDao.java         # 单词DAO
│   │   │   │   │   └── UserDao.java         # 用户DAO
│   │   │   │   │
│   │   │   │   └── preferences/              # 本地存储
│   │   │   │       └── PreferencesManager.java
│   │   │   │
│   │   │   └── utils/                        # 工具类
│   │   │       ├── RetrofitClient.java      # Retrofit客户端
│   │   │       ├── JWTManager.java          # Token管理
│   │   │       └── AnimationUtils.java      # 动画工具
│   │   │
│   │   └── res/                              # 资源文件
│   │       ├── layout/                       # 布局文件
│   │       │   ├── activity_login.xml
│   │       │   ├── activity_main.xml
│   │       │   ├── activity_ai_chat.xml
│   │       │   ├── activity_scene_practice.xml
│   │       │   ├── activity_training_summary.xml
│   │       │   ├── item_word.xml
│   │       │   └── item_chat_message.xml
│   │       │
│   │       ├── values/                       # 资源值
│   │       │   ├── colors.xml               # 颜色定义
│   │       │   ├── strings.xml              # 字符串资源
│   │       │   ├── dimens.xml               # 尺寸定义
│   │       │   └── styles.xml               # 样式定义
│   │       │
│   │       └── drawable/                     # 图片资源
│   │           ├── ic_launcher_background.xml
│   │           └── ic_star.xml
│   │
│   └── build.gradle.kts                      # 应用级构建配置
│
├── build.gradle.kts                          # 项目级构建配置
├── settings.gradle.kts                       # Gradle设置
└── gradle.properties
```

### 5.2 网络配置

#### build.gradle.kts

```kotlin
android {
    // ...
    buildTypes {
        debug {
            // ⚠️ 使用独立端口
            buildConfigField("String", "BASE_URL", "\"http://47.242.74.112:8885/api/\"")
            buildConfigField("String", "WS_URL", "\"ws://47.242.74.112:8885/api/ws\"")

            // 本地开发（使用10.0.2.2访问主机）
            // buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8885/api/\"")
            // buildConfigField("String", "WS_URL", "\"ws://10.0.2.2:8885/api/ws\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"http://47.242.74.112:8885/api/\"")
            buildConfigField("String", "WS_URL", "\"ws://47.242.74.112:8885/api/ws\"")
        }
    }
}

dependencies {
    // 网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON解析
    implementation("com.google.code.gson:gson:2.10.1")

    // 本地数据库
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")

    // MVVM架构组件
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    // 其他依赖
    // ...
}
```

### 5.3 权限配置

#### AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.englishword">

    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- 存储权限（Android 12及以下） -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <!-- 相机权限（如果后续添加扫一扫功能） -->
    <uses-permission android:name="android.permission.CAMERA" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:usesCleartextTraffic="true"  <!-- 允许HTTP流量 -->
        android:theme="@style/Theme.EnglishWord">

        <!-- 所有Activity -->
        <activity
            android:name=".ui.login.LoginActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- 其他Activity -->
        <!-- ... -->
    </application>
</manifest>
```

---

## 六、API接口设计

### 6.1 认证模块

| 路径 | 方法 | 说明 | 是否需要Token |
|------|------|------|--------------|
| `/api/auth/register` | POST | 用户注册 | ❌ |
| `/api/auth/login` | POST | 用户登录 | ❌ |
| `/api/auth/logout` | POST | 用户登出 | ✅ |
| `/api/auth/me` | GET | 获取当前用户信息 | ✅ |

#### POST /api/auth/register

**请求：**
```json
{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": "user_1234567890",
    "username": "testuser",
    "nickname": "测试用户",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 6.2 单词管理模块

| 路径 | 方法 | 说明 | 是否需要Token |
|------|------|------|--------------|
| `/api/words` | GET | 获取单词列表 | ✅ |
| `/api/words` | POST | 添加单词 | ✅ |
| `/api/words/{id}` | GET | 获取单词详情 | ✅ |
| `/api/words/{id}` | PUT | 更新单词 | ✅ |
| `/api/words/{id}` | DELETE | 删除单词 | ✅ |
| `/api/words/{id}/mastery` | PUT | 更新掌握程度 | ✅ |
| `/api/words/search` | GET | 搜索单词 | ✅ |

#### GET /api/words

**查询参数：**
- `status`: LEARNING / MASTERED / ALL（默认ALL）
- `page`: 页码（从0开始，默认0）
- `size`: 每页数量（默认20）
- `sort`: 排序字段（created_at / mastery_level / word）

**响应：**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "wordId": "word_001",
        "word": "ephemeral",
        "pronunciation": "/ɪˈfemərəl/",
        "partOfSpeech": "adj.",
        "definition": "短暂的；瞬息的",
        "exampleSentence": "Fashion is ephemeral, changing with every season.",
        "exampleTranslation": "时尚是短暂的，每一季都在变化。",
        "masteryLevel": 3,
        "status": "LEARNING",
        "createdAt": "2025-02-05T10:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "currentPage": 0
  }
}
```

### 6.3 训练模块

| 路径 | 方法 | 说明 | 是否需要Token |
|------|------|------|--------------|
| `/api/training/start` | POST | 开始训练 | ✅ |
| `/api/training/complete` | POST | 完成训练 | ✅ |
| `/api/training/history` | GET | 获取训练历史 | ✅ |

#### POST /api/training/start

**请求：**
```json
{
  "wordIds": ["word_001", "word_002", "word_003"],
  "mode": "SCENE_PRACTICE"
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "sessionId": "session_001",
    "targetWords": [
      {
        "wordId": "word_001",
        "word": "ephemeral",
        "masteryLevel": 2
      }
    ],
    "scenario": "You're at a coffee shop. The barista asks: 'How would you describe the atmosphere here?'"
  }
}
```

### 6.4 AI聊天模块

| 路径 | 方法 | 说明 | 是否需要Token |
|------|------|------|--------------|
| `/api/ai/chat` | POST | AI对话（HTTP） | ✅ |
| `/api/ws` | WS | WebSocket实时对话 | ✅ |

#### POST /api/ai/chat

**请求：**
```json
{
  "message": "ephemeral是什么意思？用法是什么？",
  "conversationId": "conv_001",  // 可选，继续历史对话
  "context": {                  // 可选，额外上下文
    "wordId": "word_001"
  }
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "conversationId": "conv_001",
    "message": "**Ephemeral** /ɪˈfemərəl/ (adj.) 短暂的；瞬息的\n\n### 中文释义\n形容持续时间很短的事物...\n\n### 例句\n- Fashion is **ephemeral**.\n- 时尚是**短暂的**。\n\n### 同义词\n- temporary, transient, fleeting",
    "suggestions": [
      {
        "type": "add_to_list",
        "word": "ephemeral",
        "label": "添加到单词本"
      },
      {
        "type": "train",
        "word": "ephemeral",
        "label": "开始训练"
      }
    ]
  }
}
```

---

## 七、AI服务集成

### 7.1 智谱AI配置

#### application.yml

```yaml
zhipuai:
  api-key: ${ZHIPUAI_API_KEY}      # 从环境变量读取
  api-url: https://open.bigmodel.cn/api/paas/v4/chat/completions
  model: glm-4-flash               # 快速模型（成本更低）
  max-tokens: 2000                 # 最大回复长度
  temperature: 0.7                 # 创造性程度（0-1）
  top-p: 0.9                       # 核采样参数
```

#### 环境变量配置

**本地开发：**
```bash
# Windows (PowerShell)
$env:ZHIPUAI_API_KEY="your_api_key_here"

# Linux/Mac
export ZHIPUAI_API_KEY="your_api_key_here"
```

**生产环境（Docker）：**

创建 `.env` 文件（不提交到Git）：
```
ZHIPUAI_API_KEY=your_production_api_key_here
```

在 `docker-compose.yml` 中引用：
```yaml
services:
  app:
    environment:
      - ZHIPUAI_API_KEY=${ZHIPUAI_API_KEY}
```

### 7.2 Prompt模板设计

#### Word Inquiry 模式 Prompt

```
你是"English Word App"的专业英语AI助手，专门帮助学习者理解单词含义和用法。

## 你的职责
1. **单词解释**：提供准确的中文释义、音标、词性
2. **例句展示**：给出实用的英文例句和中文翻译
3. **用法说明**：解释单词的使用场景和搭配
4. **同义词对比**：提供近义词及其细微差别

## 回复格式要求
- **必须使用Markdown格式**
- 单词使用 **加粗** 标题
- 音标使用行内代码
- 释义使用无序列表
- 例句使用引用块 >
- 重要提示使用加粗

## 示例格式
**{word}** /{pronunciation}/ ({part_of_speech}) {中文释义}

### 详细释义
- 定义1
- 定义2

### 例句
> {example_sentence}

> 翻译：{example_translation}

### 用法说明
- 使用场景1
- 常见搭配：{collocations}

### 同义词
- {synonym1}: {difference}
- {synonym2}: {difference}

**重要提示**：
- 回复要友好、鼓励、专业
- 适合中高级英语学习者
- 例句要贴近生活场景
- 不要建议图片URL
```

#### Word Training 模式 Prompt

```
你是一位英语对话教练，正在通过角色扮演场景帮助用户练习使用目标单词。

## 当前任务
用户正在练习单词：**{target_word}**

## 场景设定
{scenario_description}

## 你的职责
1. **扮演场景角色**：根据设定的场景与用户对话
2. **引导使用目标词**：创造机会让用户使用 {target_word}
3. **自然对话**：保持对话流畅，不生硬
4. **友好鼓励**：用户使用正确时给予积极反馈
5. **纠正指导**：用户使用错误时委婉纠正并示范

## 对话规则
- 每次回复简短（50字以内）
- 引导用户说完整句子
- 不要直接告诉答案，而是通过提问引导
- 使用表情符号增加友好度 😊

## 反馈时机
- 用户正确使用目标词：👍 "Great! You used {target_word} perfectly!"
- 用户使用错误：💡 "Almost! You could say: ..."
- 用户卡住时：💭 "Hint: Think about {context}..."

请开始对话，记住要帮助用户自然地使用 {target_word}！
```

### 7.3 API调用示例

#### Java代码示例

```java
@Service
public class ZhipuAIService {

    @Value("${zhipuai.api-key}")
    private String apiKey;

    @Value("${zhipuai.api-url}")
    private String apiUrl;

    @Value("${zhipuai.model}")
    private String model;

    public String chat(String message, String conversationHistory) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();

            // 系统提示词
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt());
            messages.add(systemMessage);

            // 历史对话（如果有）
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                // 解析并添加历史消息
                // ...
            }

            // 当前用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            // 调用智谱AI API
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    new ObjectMapper().writeValueAsString(requestBody)
                ))
                .build();

            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            // 解析响应
            JsonNode root = new ObjectMapper().readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            throw new RuntimeException("AI调用失败", e);
        }
    }

    private String getSystemPrompt() {
        // 返回上面定义的Prompt模板
        return "你是English Word App的专业英语AI助手...";
    }
}
```

### 7.4 成本控制

#### 使用策略

1. **模型选择**：
   - 使用 `glm-4-flash`（快速模型）而非 `glm-4-plus`
   - Flash 模型成本更低，响应更快

2. **Token限制**：
   - 单次回复最大 2000 tokens
   - 历史对话保留最近 10 轮

3. **缓存机制**：
   - 相同问题 24 小时内直接返回缓存结果
   - 减少重复 API 调用

4. **用户配额**：
   - 每用户每天最多 100 次 AI 调用
   - 超出后提示升级或次日再试

#### 成本估算

| 项目 | 单价 | 日均 | 月均 |
|------|------|------|------|
| 智谱AI Flash | ¥0.001/1K tokens | 50K tokens | 1.5M tokens |
| **AI成本** | - | ¥0.05 | **¥1.5** |
| 100用户 × 50次 | - | ¥5 | **¥150** |

**结论：月均 AI 成本约 ¥150（100活跃用户）**

---

## 八、部署方案

### 8.1 Docker Compose 编排

#### docker-compose.yml

```yaml
version: '3.8'

services:
  # MySQL数据库
  mysql:
    image: mysql:8.0
    container_name: english-word-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: english_word_app        # ⚠️ 独立数据库名
      TZ: Asia/Shanghai
    ports:
      - "3307:3306"                          # ⚠️ 独立端口
    volumes:
      # 数据持久化
      - mysql-data:/var/lib/mysql
      # 初始化SQL脚本
      - ./init_db.sql:/docker-entrypoint-initdb.d/init_db.sql
    networks:
      - english-word-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p123456"]
      timeout: 20s
      retries: 10

  # Spring Boot应用
  app:
    # 使用官方OpenJDK 17镜像
    image: eclipse-temurin:17-jre-alpine
    container_name: english-word-app
    restart: always
    working_dir: /app
    environment:
      # 数据库连接配置（连接到MySQL容器）
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/english_word_app?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
      # 智谱AI API Key
      ZHIPUAI_API_KEY: ${ZHIPUAI_API_KEY}
      TZ: Asia/Shanghai
    ports:
      - "8885:8885"                          # ⚠️ 独立端口
    volumes:
      # 挂载jar包
      - ./target/english-word-backend-1.0.0.jar:/app/app.jar
      # 挂载上传文件目录（如果需要）
      - ./uploads:/app/uploads
    networks:
      - english-word-network
    depends_on:
      mysql:
        condition: service_healthy
    command: ["java", "-jar", "app.jar"]

# 数据卷
volumes:
  mysql-data:
    driver: local

# 网络
networks:
  english-word-network:
    driver: bridge
```

### 8.2 部署步骤

#### 1. 本地打包

```bash
cd english-word-backend
mvn clean package -DskipTests
```

#### 2. 上传到服务器

```powershell
# 创建远程目录
ssh root@47.242.74.112 "mkdir -p /root/english-word"

# 上传文件
scp .\docker-compose.yml root@47.242.74.112:/root/english-word/
scp .\target\english-word-backend-1.0.0.jar root@47.242.74.112:/root/english-word/target/
scp .\deploy.sh root@47.242.74.112:/root/english-word/
```

#### 3. 启动服务

```bash
ssh root@47.242.74.112
cd /root/english-word
chmod +x deploy.sh
./deploy.sh deploy
```

#### 4. 验证部署

```bash
# 检查服务状态
docker compose ps

# 查看日志
docker compose logs -f app

# 测试API
curl http://47.242.74.112:8885/api/swagger-ui.html
```

### 8.3 常用命令

```bash
# 部署/启动
./deploy.sh deploy

# 重启服务
./deploy.sh restart

# 停止服务
./deploy.sh stop

# 查看日志
./deploy.sh logs-app

# 查看状态
./deploy.sh status
```

### 8.4 Nginx反向代理配置（可选）

如果需要通过域名访问，可以配置 Nginx：

```nginx
# /etc/nginx/sites-available/english-word-app

server {
    listen 80;
    server_name api.jamesweb.org;

    # eatwhere 项目
    location /eatwhat/ {
        proxy_pass http://localhost:8883/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # english-word 项目
    location /english-word/ {
        proxy_pass http://localhost:8885/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

## 九、开发计划

### 9.1 开发阶段划分

#### 第一阶段：后端基础框架（2周）

**目标**：搭建后端基础架构，实现核心API

| 任务 | 工作量 | 负责人 | 状态 |
|------|--------|--------|------|
| 项目结构搭建 | 2天 | 后端 | ⏳ |
| 数据库设计与初始化 | 2天 | 后端 | ⏳ |
| 用户认证模块 | 2天 | 后端 | ⏳ |
| 单词管理API | 3天 | 后端 | ⏳ |
| AI聊天接口 | 3天 | 后端 | ⏳ |
| 单元测试 | 2天 | 后端 | ⏳ |

**里程碑**：后端API可测试，Swagger文档可访问

#### 第二阶段：Android客户端框架（2周）

**目标**：搭建Android基础架构，实现核心UI

| 任务 | 工作量 | 负责人 | 状态 |
|------|--------|--------|------|
| 项目结构搭建 | 2天 | Android | ⏳ |
| 网络层配置 | 2天 | Android | ⏳ |
| 登录/注册UI | 3天 | Android | ⏳ |
| 单词库UI | 4天 | Android | ⏳ |
| Room数据库集成 | 2天 | Android | ⏳ |
| 基础动画实现 | 2天 | Android | ⏳ |

**里程碑**：可以登录、查看单词列表

#### 第三阶段：核心功能开发（3周）

**目标**：实现AI聊天和场景练习功能

| 任务 | 工作量 | 负责人 | 状态 |
|------|--------|--------|------|
| AI聊天UI | 3天 | Android | ⏳ |
| WebSocket集成 | 3天 | Android + 后端 | ⏳ |
| 场景练习逻辑 | 5天 | Android + 后端 | ⏳ |
| 训练总结页面 | 2天 | Android | ⏳ |
| 完整动画系统 | 3天 | Android | ⏳ |
| 联调测试 | 3天 | 全员 | ⏳ |

**里程碑**：完整功能流程可跑通

#### 第四阶段：测试与优化（2周）

**目标**：完善功能，优化性能

| 任务 | 工作量 | 负责人 | 状态 |
|------|--------|--------|------|
| 功能测试 | 3天 | 全员 | ⏳ |
| Bug修复 | 3天 | 全员 | ⏳ |
| 性能优化 | 2天 | 全员 | ⏳ |
| UI优化 | 2天 | Android | ⏳ |
| 部署上线 | 2天 | 后端 | ⏳ |

**里程碑**：MVP版本上线

### 9.2 甘特图（估算）

```
Week 1-2:  [后端框架]
Week 3-4:           [Android框架]
Week 5-7:                     [核心功能开发]
Week 8-9:                                [测试与优化]
```

**总开发周期：9周（约2.25个月）**

### 9.3 团队配置建议

| 角色 | 人数 | 职责 |
|------|------|------|
| 后端工程师 | 1人 | Spring Boot开发、API设计、数据库设计 |
| Android工程师 | 1人 | Android客户端开发、UI实现、动画 |
| UI设计师 | 0.5人 | UI设计、动画设计（已完成） |
| 测试工程师 | 0.5人 | 功能测试、兼容性测试 |

**最小团队：2人（后端 + Android）**

---

## 十、成本估算

### 10.1 一次性成本

| 项目 | 金额 | 说明 |
|------|------|------|
| 服务器 | ¥0 | 复用 eatwhere 服务器 |
| 域名 | ¥0 | 复用 eatwhere 域名 |
| 开发工具 | ¥0 | IntelliJ IDEA Community（免费） |
| **总计** | **¥0** | 无一次性成本 |

### 10.2 月度运营成本

| 项目 | 配置 | 金额 | 说明 |
|------|------|------|------|
| 云服务器 | 1核2G | ¥50-100 | 如果共用 eatwhere 则更便宜 |
| AI API调用 | 100用户 × 50次/天 | ¥150 | 智谱AI，根据实际使用量 |
| 数据备份 | - | ¥10 | 定期备份成本 |
| **总计** | - | **¥110-260/月** | |

### 10.3 开发成本

| 方案 | 周期 | 成本 | 说明 |
|------|------|------|------|
| 自己开发 | 9周 | ¥0 | 仅时间成本 |
| 外包开发 | 2.5个月 | ¥3-5万 | 市场价 |
| 混合方案 | - | - | 核心功能自己，非核心外包 |

### 10.4 ROI分析

**假设：**
- 免费用户转化率：5%
- 付费用户月费：¥19.9
- 目标用户：1000人

**收入估算：**
- 付费用户 = 1000 × 5% = 50人
- 月收入 = 50 × ¥19.9 = ¥995
- 年收入 = ¥995 × 12 = ¥11,940

**成本估算：**
- 年运营成本 = ¥200 × 12 = ¥2,400
- 年开发成本 = ¥0（自己开发）

**ROI：**
- 第一年净利润 = ¥11,940 - ¥2,400 = ¥9,540
- ROI = 9,540 / 2,400 = **397%**

**结论：项目经济可行性高**

---

## 十一、风险评估与应对

### 11.1 技术风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| AI API费用超预算 | 高 | 中 | 1. 使用智谱AI（低成本）<br>2. 设置每日调用上限<br>3. 实现缓存机制 |
| 服务器性能不足 | 中 | 低 | 1. 初期用户少，1核2G够<br>2. 监控性能，及时升级<br>3. 优化数据库查询 |
| WebSocket不稳定 | 中 | 中 | 1. 实现断线重连<br>2. 降级到HTTP轮询<br>3. 充分测试 |
| Android兼容性 | 中 | 中 | 1. Min SDK 24覆盖99%<br>2. 多设备测试<br>3. 适配不同屏幕尺寸 |

### 11.2 业务风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| 用户增长缓慢 | 高 | 中 | 1. 优化产品体验<br>2. 口碑传播<br>3. 社交媒体推广 |
| AI回复质量不佳 | 高 | 低 | 1. 优化Prompt<br>2. 收集用户反馈<br>3. 持续调优 |
| 付费转化率低 | 中 | 中 | 1. 免费试用<br>2. 优质内容<br>3. 合理定价 |
| 竞品模仿 | 中 | 高 | 1. 快速迭代<br>2. 打造特色<br>3. 用户粘性 |

### 11.3 运营风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| 数据泄露 | 高 | 低 | 1. 密码加密<br>2. JWT认证<br>3. 定期安全审计 |
| 服务器宕机 | 高 | 低 | 1. Docker自动重启<br>2. 日志监控<br>3. 备份数据 |
| 成本超支 | 中 | 中 | 1. 设置预算上限<br>2. 监控API使用量<br>3. 及时优化 |

---

## 十二、总结与建议

### 12.1 可行性结论

✅ **高度可行**

**理由：**
1. **技术成熟**：100%复用 eatwhere 成熟架构
2. **成本可控**：月运营成本 ¥110-260，AI成本¥150
3. **开发周期短**：9周完成 MVP
4. **风险低**：已有成功案例，技术栈验证过
5. **扩展性好**：Docker部署，易于后期扩展
6. **ROI高**：预计第一年净利润 ¥9,540

### 12.2 核心优势

1. **差异化定位**：AI驱动 + 场景练习，区别于传统背单词App
2. **学习效果好**：真实场景训练，而非死记硬背
3. **用户体验佳**：精心设计的UI和动画
4. **技术领先**：使用最新的AI技术（智谱GLM-4）
5. **成本优势**：使用国内AI服务，成本远低于OpenAI

### 12.3 启动建议

**立即行动：**

1. ✅ **第一阶段（本周）**：
   - 创建后端项目结构
   - 设计并创建数据库
   - 配置智谱AI API Key
   - 实现用户认证模块

2. ✅ **第二阶段（下周）**：
   - 创建Android项目
   - 实现登录/注册UI
   - 配置网络层
   - 实现单词列表展示

3. ✅ **第三阶段（第3-4周）**：
   - 实现AI聊天功能
   - 实现场景练习逻辑
   - 完成动画系统

4. ✅ **第四阶段（第5-9周）**：
   - 全面测试
   - 优化性能
   - 部署上线

### 12.4 后续优化方向

**MVP之后的功能增强：**

1. **Phase 2（3个月后）**：
   - 语音识别（练习口语）
   - 单词发音评测
   - 社交分享功能
   - 学习统计报表

2. **Phase 3（6个月后）**：
   - 多语言支持（日语、韩语）
   - 离线模式
   - 自定义场景创建
   - AI学习路径推荐

3. **Phase 4（1年后）**：
   - 社区功能
   - 排行榜
   - 会员体系
   - API开放平台

---

## 附录

### A. 参考资料

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [智谱AI开放平台](https://open.bigmodel.cn/)
- [Android开发者指南](https://developer.android.com/guide)
- [WebSocket协议RFC](https://tools.ietf.org/html/rfc6455)
- [Eatwhat项目参考](../eatwhat/)

### B. 联系方式

- **项目负责人**：[您的名字]
- **技术支持**：[邮箱地址]
- **GitHub仓库**：[仓库地址]

### C. 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| v1.0 | 2025-02-05 | Claude Code | 初始版本，完整技术架构设计 |

---

**文档结束**
