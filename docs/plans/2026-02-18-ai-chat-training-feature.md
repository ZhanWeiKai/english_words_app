# AI Chat Training Feature Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement AI-powered word training feature that allows users to select words from vault and practice them in IELTS-style conversations.

**Architecture:** Android app (Kotlin/Compose) + Backend (Spring Boot) + ZhipuAI API. Training words flow from WordVault → AIChatScreen → Backend → ZhipuAI.

**Tech Stack:** Kotlin, Jetpack Compose, Retrofit2, Coroutines, StateFlow, Spring Boot, ZhipuAI GLM-4-Flash

---

## 1. Existing Flow (当前行为)

### User Authentication Flow (不改变)
```
LoginScreen → 输入用户名密码 → 调用 /api/auth/login →
保存 Token 到 DataStore → 导航到 WordVaultScreen
```

### Word Vault Flow (不改变)
```
WordVaultScreen → 显示用户单词列表 →
点击单词 → 查看详情 → 返回列表
```

### AI Chat Flow (之前是空的)
```
AIChatScreen → 显示空消息列表 → 无法发送消息（没有 API 集成）
```

---

## 2. Extension Point (扩展点)

### 允许修改的模块

| 模块 | 文件 | 修改内容 |
|------|------|----------|
| **Data Models** | `Models.kt` | 添加 `AIChatResponse`, `AIChatRequest` |
| **API Layer** | `ApiService.kt` | 更新 chat 接口签名 |
| **ViewModel** | `AIChatViewModel.kt` | 新建，管理聊天状态 |
| **UI Screen** | `AIChatScreen.kt` | 集成 ViewModel |
| **Word Vault** | `WordVaultScreen.kt` | 添加多选模式 |
| **Navigation** | `MainActivity.kt` | 传递选中单词 |
| **Backend Request** | `AIChatRequest.java` | 添加 `trainingWords` |
| **Backend Service** | `AIConversationService.java` | 传递 `trainingWords` |
| **AI Service** | `ZhipuAIService.java` | 更新训练 prompt |

### 扩展点示意图

```
┌─────────────────────────────────────────────────────────────┐
│                    Android App                               │
│  ┌──────────────┐     ┌──────────────┐     ┌─────────────┐  │
│  │WordVaultScreen│────▶│ MainActivity │────▶│AIChatScreen │  │
│  │ (多选模式)    │     │ (传递单词)   │     │(训练界面)   │  │
│  └──────────────┘     └──────────────┘     └──────┬──────┘  │
│                                                    │         │
│                                            ┌──────▼──────┐  │
│                                            │AIChatViewModel│ │
│                                            │(管理状态)   │  │
│                                            └──────┬──────┘  │
│                                                    │         │
│                                            ┌──────▼──────┐  │
│                                            │ ApiService  │  │
│                                            │(Retrofit)   │  │
│                                            └──────┬──────┘  │
└───────────────────────────────────────────────────┼─────────┘
                                                    │ HTTP
┌───────────────────────────────────────────────────┼─────────┐
│                    Backend                         │         │
│                                            ┌──────▼──────┐  │
│                                            │AIController │  │
│                                            └──────┬──────┘  │
│                                            ┌──────▼──────┐  │
│                                            │AIConversation│ │
│                                            │Service      │  │
│                                            └──────┬──────┘  │
│                                            ┌──────▼──────┐  │
│                                            │ZhipuAIService│ │
│                                            │(训练Prompt) │  │
│                                            └──────┬──────┘  │
└───────────────────────────────────────────────────┼─────────┘
                                                    │
                                            ┌──────▼──────┐
                                            │  ZhipuAI    │
                                            │  GLM-4-Flash│
                                            └─────────────┘
```

---

## 3. Non-breaking Rules (不破坏规则)

### 必须保持不变

| 规则 | 描述 |
|------|------|
| ❌ 不改变认证流程 | Token 管理、登录/注册流程保持不变 |
| ❌ 不改变单词 CRUD | 添加、删除、更新单词的 API 和逻辑不变 |
| ❌ 不改变其他 Screen | LoginScreen, TrainingSummaryScreen 不受影响 |
| ❌ 不改变 API 兼容性 | 现有 API 签名向后兼容，新字段可选 |

### API 兼容性保证

```kotlin
// 旧调用方式仍然有效
ApiService.chat(mapOf("message" to "hello"))  // 仍然工作

// 新调用方式
ApiService.chat(AIChatRequest(message = "hello", trainingWords = listOf("word1")))
```

---

## 4. Risk Points (风险点)

| 风险 | 影响范围 | 缓解措施 |
|------|----------|----------|
| **API 签名变更** | 可能破坏现有调用 | 使用可选参数，保持向后兼容 |
| **网络超时** | 用户体验差 | 添加加载状态，错误处理 |
| **Token 过期** | API 调用失败 | RetrofitClient 已处理 Token 刷新 |
| **并发状态** | 消息列表混乱 | 使用 StateFlow 确保线程安全 |
| **训练 Prompt 格式** | AI 返回不符合预期 | 明确 prompt 格式要求 |

---

## Task Breakdown

### Task 1: Add Data Models (Build)
- 添加 `AIChatResponse` 数据类
- 添加 `AIChatRequest` 数据类（含 `trainingWords`）
- 验证：编译通过

### Task 2: Update API Layer (Build)
- 更新 `ApiService.chat()` 为 suspend 函数
- 验证：编译通过

### Task 3: Create ViewModel (Build)
- 创建 `AIChatViewModel`
- 实现消息状态管理
- 实现 API 调用逻辑
- 验证：单元测试通过

### Task 4: Update AIChatScreen (Build)
- 集成 ViewModel
- 添加加载指示器
- 更新发送逻辑
- 验证：UI 正常显示

### Task 5: Add Multi-select to WordVault (Build)
- 实现长按进入多选模式
- 添加选择状态管理
- 添加 Start Training FAB
- 验证：多选功能正常

### Task 6: Update Navigation (Build)
- MainActivity 传递选中单词
- 验证：导航参数正确传递

### Task 7: Update Backend (Build)
- 添加 `trainingWords` 字段
- 更新 prompt 格式
- 验证：API 返回正确格式

### Task 8: Regression Test (Verify)
- 登录流程正常
- 单词 CRUD 正常
- AI Chat 正常
- 验证：所有功能无回归

---

## Validation Gate

完成所有任务后，必须运行：

### Backend Tests
```bash
cd english-word-backend
mvn test
```

### Android Build
```bash
cd android-word-app
gradlew assembleDebug
```

### Manual Tests
1. 登录 → 成功
2. 添加单词 → 成功
3. AI Chat 发消息 → 收到 AI 回复
4. 多选单词训练 → AI 返回雅思格式问题

---

## Commit Message Format

```
feat: add AI-powered word training with IELTS examiner format

- Add trainingWords support to API
- Implement multi-select mode in WordVault
- Create AIChatViewModel for state management
- Update training prompt to IELTS examiner format

Co-Authored-By: Claude <noreply@anthropic.com>
```
